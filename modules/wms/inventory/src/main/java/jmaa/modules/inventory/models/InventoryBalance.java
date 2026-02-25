package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.tools.SpringUtils;
import org.jmaa.sdk.tools.ThrowableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Model.Meta(name = "wms.inventory_balance", label = "仓库平账单", inherit = {"mixin.order_status", "code.auto_code"})
public class InventoryBalance extends Model {
    private static final Logger log = LoggerFactory.getLogger(InventoryBalance.class);
    static Field code = Field.Char().label("盘点平账单号");
    static Field status = Field.Selection(new Options() {{
        put("draft", "草稿");
        put("balance", "平账中");
        put("done", "完成");
    }}).label("状态").defaultValue("draft").readonly(true).tracking();
    static Field inventory_check_id = Field.Many2one("wms.inventory_check").label("盘点单");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").related("inventory_check_id.warehouse_id");
    static Field line_ids = Field.One2many("wms.inventory_check_line", "inventory_balance_id").label("盘点物料明细").related("inventory_check_id.line_ids");
    static Field details_ids = Field.One2many("wms.inventory_check_details", "inventory_balance_id").label("盘点标签明细").related("inventory_check_id.details_ids");

    // 开线程,
    // env不共用,每个单独,return 会提交,处理平账异常
    // 代码优化
    @ServiceMethod(label = "盘点平账", doc = "盘点平账")
    public void inventoryBalance(Records record) {
        Environment env = record.getEnv();
        ThreadPoolExecutor executor = SpringUtils.getBean(ThreadPoolExecutor.class);
        executor.execute(balance(env, record.getId()));
        record.set("status", "balance");
    }

    // 下面可以给定时任务做,也可以放这里,线程开跑
    private Runnable balance(Environment environment, String inventoryBalanceId) {
        return () -> {
            try (Cursor cursor = environment.getDatabase().openCursor()) {
                Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                // 清空环境缓存
                env.getCache().invalidate();
                Records inventoryBalance = env.get("wms.inventory_balance", inventoryBalanceId);
                Records inventoryCheck = inventoryBalance.getRec("inventory_check_id");
                Records lineIds = inventoryCheck.getRec("line_ids");
                List<Future<Boolean>> futures = new ArrayList<>();
                // 每个物料一个线程,会不会oom todo, 后续拿实际数据测试几次
                ThreadPoolExecutor executor = SpringUtils.getBean(ThreadPoolExecutor.class);
                for (Records line : lineIds) {
                    //Callable 有返回值, 会报错,如果存在某一线程异常,则会在get处终止  不会生成入库单和出库单  √
                    //Runnable 无返回值,无报错,会一直执行, 会有入库单,出库单,实际明细数据不全 性能更好 但后续其他数据需依赖之前的结果
                    futures.add(executor.submit(runItemTask(env, inventoryBalanceId, line.getId())));
                }
                for (Future<Boolean> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        inventoryBalance.call("trackMessage", "执行平账操作失败: " + ThrowableUtils.getMessage(e));
                        cursor.commit();
                        return;
                    }
                }
                // 上面完成,才开始处理
                Records stockInDetails = env.get("stock.stock_in_details").find(Criteria.equal("inventory_balance_id", inventoryBalanceId));
                Records stockIn = env.get("stock.stock_in");
                if (stockInDetails.any()) {
                    Map<String, Object> stockInMap = new HashMap<>();
                    stockInMap.put("status", "new");
                    stockInMap.put("type", "wms.inventory_balance");
                    stockInMap.put("related_model", "wms.inventory_balance");
                    stockInMap.put("related_code", inventoryBalance.getString("code"));
                    stockInMap.put("related_id", inventoryBalance.getId());
                    stockInMap.put("remark", "盘点平账,盘盈");
                    stockIn = stockIn.create(stockInMap);
                    stockInDetails.set("stock_in_id", stockIn.getId());
                }
                Records stockOutDetails = env.get("stock.stock_out_details").find(Criteria.equal("inventory_balance_id", inventoryBalanceId));
                Records stockOut = env.get("stock.stock_out");
                if (stockOutDetails.any()) {
                    Map<String, Object> stockOutMap = new HashMap<>();
                    stockOutMap.put("status", "new");
                    stockOutMap.put("type", "wms.inventory_balance");
                    stockOutMap.put("related_model", "wms.inventory_balance");
                    stockOutMap.put("related_code", inventoryBalance.getString("code"));
                    stockOutMap.put("related_id", inventoryBalance.getId());
                    stockOutMap.put("remark", "盘点平账,盘亏");
                    stockOut = stockOut.create(stockOutMap);
                    stockOutDetails.set("stock_out_id", stockOut.getId());
                }
                String sql = "select distinct status from wms_inventory_check_line where inventory_check_id=%s";
                Cursor cr = env.getCursor();
                cr.execute(sql, Collections.singletonList(inventoryCheck.getId()));
                boolean done = cr.fetchAll().stream().map(r -> (String) r[0]).allMatch("done"::equals);
                if (done) {
                    //如果全部为完成状态，则更新为已完成状态
                    inventoryCheck.set("status", "done");
                    inventoryBalance.set("status", "done");
                    stockOut.set("status", "done");
                    stockIn.set("status", "done");
                }
                env.get("base").flush();
                cursor.commit();
            } catch (Exception e) {
                try (Cursor cursor = environment.getDatabase().openCursor()) {
                    Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                    env.get("wms.inventory_balance", inventoryBalanceId).call("trackMessage", "执行平账操作失败: " + ThrowableUtils.getMessage(e));
                    cursor.commit();
                }
            }
        };
    }

    public Callable<Boolean> runItemTask(Environment environment, String balanceId, String lineId) {
        return () -> {
            try (Cursor cursor = environment.getDatabase().openCursor()) {
                Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                // 清空环境缓存
                env.getCache().invalidate();
                Records inventoryCheckLine = env.get("wms.inventory_check_line", lineId);
                if ("done".equals(inventoryCheckLine.getString("status"))) {
                    return true;
                }
                Records inventoryBalance = env.get("wms.inventory_balance", balanceId);
                Records material = inventoryCheckLine.getRec("material_id");
                Records inventoryCheck = inventoryCheckLine.getRec("inventory_check_id");
                Records warehouse = inventoryCheck.getRec("warehouse_id");
                String stockRule = material.getString("stock_rule");
                String category = material.getString("category");
                Records inventoryCheckDetails = env.get("wms.inventory_check_details").find(Criteria.equal("inventory_check_id", inventoryCheck.getId())
                    .and(Criteria.equal("material_id", material.getId())));
                if ("sn".equals(stockRule) || "semi-finished".equals(category) || "finished".equals(category)) {
                    extractedSn(inventoryCheckDetails, inventoryCheck, inventoryBalance, material, warehouse);
                } else if ("lot".equals(stockRule)) {
                    extractedLot(inventoryCheckDetails, inventoryCheck, balanceId, material, warehouse);
                } else {
                    extractedNum(inventoryCheckDetails, inventoryCheck, balanceId, material, warehouse);
                }
                Records lossesOnhand = env.get("stock.onhand").find(Criteria.equal("warehouse_id", warehouse.getId())
                    .and(Criteria.equal("material_id", material.getId())).and(Criteria.equal("status", "frozen")));
                if (lossesOnhand.any()) {
                    // 这里是没有扫码的 冻结中  的在库标签, 全部生成出库单,库存删除
                    for (Records onhand : lossesOnhand) {
                        Map<String, Object> stockDetailMap = new HashMap<>();
                        stockDetailMap.put("status", "done");
                        stockDetailMap.put("qty", onhand.getDouble("frozen_qty"));
                        stockDetailMap.put("warehouse_id", warehouse.getId());
                        stockDetailMap.put("location_id", onhand.getRec("location_id").getId());
                        stockDetailMap.put("material_id", material.getId());
                        stockDetailMap.put("inventory_balance_id", inventoryBalance.getId());
                        if ("sn".equals(stockRule) || "semi-finished".equals(category) || "finished".equals(category)) {
                            Records label = onhand.getRec("label_id");
                            stockDetailMap.put("label_id", label.getId());
                            stockDetailMap.put("lot_num", onhand.getString("lot_num"));
                            label.set("status", "balance");
                            label.set("qty", 0d);
                            // 操作日志
                            Map<String, Object> log = new HashMap<>();
                            log.put("operation", "wms.inventory_check:non_balance");
                            log.put("related_id", inventoryBalance.getId());
                            log.put("related_code", inventoryBalance.get("code"));
                            label.call("logStatus", log);
                        } else if ("lot".equals(stockRule)) {
                            stockDetailMap.put("lot_num", onhand.getString("lot_num"));
                        }
                        env.get("stock.stock_out_details").create(stockDetailMap);
                    }
                    lossesOnhand.delete();
                }
                inventoryCheckDetails.set("status", "done");
                inventoryCheckLine.set("status", "done");
                env.get("base").flush();
                cursor.commit();
                return true;
            } catch (Exception e) {
                try (Cursor cursor = environment.getDatabase().openCursor()) {
                    Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                    env.get("wms.inventory_balance", balanceId).call("trackMessage", "执行平账操作失败: " + ThrowableUtils.getMessage(e));
                    cursor.commit();
                }
                // 报错存库了
                return false;
            }
        };
    }

    private void extractedNum(Records inventoryCheckDetails, Records inventoryCheck, String inventoryBalanceId, Records material, Records warehouse) {
        // 合并 库位
        Environment env = inventoryCheck.getEnv();
        Map<String, Double> detailsMap = new HashMap<>();
        for (Records detail : inventoryCheckDetails) {
            double baseQty = inventoryCheck.getBoolean("second_flag") ? detail.getDouble("second_qty") : detail.getDouble("first_qty");
            String locationId = detail.getRec("location_id").getId();
            String key = locationId + "_" + detail.getDouble("qty");
            if (detailsMap.containsKey(key)) {
                detailsMap.put(key, Utils.round(baseQty + detailsMap.get(key)));
            } else {
                detailsMap.put(key, baseQty);
            }
        }
        for (Map.Entry<String, Double> entry : detailsMap.entrySet()) {
            String key = entry.getKey();
            String[] split = key.split("_");
            String locationId = "null".equals(split[0]) ? null : split[0];
            double qty = Utils.toDouble(split[1]);
            Double baseQty = entry.getValue();
            Cursor cr = env.getCursor();
            String sql = "update stock_onhand set ok_qty=ok_qty+%s,usable_qty=usable_qty+%s,status='onhand',frozen_qty=0 where material_id=%s   and warehouse_id=%s ";
            List<Object> params = new ArrayList<>(Arrays.asList(baseQty, baseQty, material.getId(), warehouse.getId()));
            if (Utils.isNotEmpty(locationId)) {
                sql += " and location_id=%s";
                params.add(locationId);
            } else {
                sql += " and location_id is null";
            }
            cr.execute(sql, params);
            double mapQty = Math.abs(Utils.round(qty - baseQty));
            // 判断是多还是少
            Map<String, Object> stockDetailMap = new HashMap<>();
            stockDetailMap.put("status", "done");
            stockDetailMap.put("qty", mapQty);
            stockDetailMap.put("warehouse_id", warehouse.getId());
            stockDetailMap.put("location_id", locationId);
            stockDetailMap.put("material_id", material.getId());
            stockDetailMap.put("inventory_balance_id", inventoryBalanceId);
            if (Utils.less(qty, baseQty)) {
                // 盈
                stockDetailMap.put("stock_qty", mapQty);
                Records records = env.get("stock.stock_in_details").create(stockDetailMap);
            } else if (Utils.large(qty, baseQty)) {
                // 亏
                Records records = env.get("stock.stock_out_details").create(stockDetailMap);
            }
        }
    }

    private void extractedLot(Records inventoryCheckDetails, Records inventoryCheck, String inventoryBalanceId, Records material, Records warehouse) {
        Environment env = inventoryCheckDetails.getEnv();
        Map<String, Double> detailsMap = new HashMap<>();
        for (Records detail : inventoryCheckDetails) {
            double baseQty = inventoryCheck.getBoolean("second_flag") ? detail.getDouble("second_qty") : detail.getDouble("first_qty");
            String lotNum = detail.getString("lot_num");
            String locationId = detail.getRec("location_id").getId();
            String key = lotNum + "_" + locationId + "_" + detail.getDouble("qty");
            if (detailsMap.containsKey(key)) {
                detailsMap.put(key, Utils.round(baseQty + detailsMap.get(key)));
            } else {
                detailsMap.put(key, baseQty);
            }
        }
        // 合并好了. 批次/库位 + 数量,直接改
        for (Map.Entry<String, Double> entry : detailsMap.entrySet()) {
            String key = entry.getKey();
            String[] split = key.split("_");
            String lotNum = split[0];
            double qty = Utils.toDouble(split[2]);
            String locationId = "null".equals(split[1]) ? null : split[1];
            Double baseQty = entry.getValue();
            Cursor cr = env.getCursor();
            String sql = "update stock_onhand set ok_qty=ok_qty+%s,usable_qty=usable_qty+%s,status='onhand',frozen_qty=0  where material_id=%s and lot_num=%s and warehouse_id=%s ";
            List<Object> params = new ArrayList<>(Arrays.asList(baseQty, baseQty, material.getId(), lotNum, warehouse.getId()));
            if (Utils.isNotEmpty(locationId)) {
                sql += " and location_id=%s";
                params.add(locationId);
            } else {
                sql += " and location_id is null";
            }
            cr.execute(sql, params);
            // 标签原来的库存数, 如果是新标签这里就是0
            double mapQty = Math.abs(Utils.round(qty - baseQty));
            // 判断是多还是少
            Map<String, Object> stockDetailMap = new HashMap<>();
            stockDetailMap.put("status", "done");
            stockDetailMap.put("qty", mapQty);
            stockDetailMap.put("warehouse_id", warehouse.getId());
            stockDetailMap.put("location_id", locationId);
            stockDetailMap.put("material_id", material.getId());
            stockDetailMap.put("lot_num", lotNum);
            stockDetailMap.put("inventory_balance_id", inventoryBalanceId);
            if (Utils.less(qty, baseQty)) {
                // 盈
                stockDetailMap.put("stock_qty", mapQty);
                Records records = env.get("stock.stock_in_details").create(stockDetailMap);
            } else if (Utils.large(qty, baseQty)) {
                // 亏
                Records records = env.get("stock.stock_out_details").create(stockDetailMap);
            }
        }
    }

    private void extractedSn(Records inventoryCheckDetails, Records inventoryCheck, Records inventoryBalance, Records material, Records warehouse) {
        Environment env = inventoryCheckDetails.getEnv();
        for (Records detail : inventoryCheckDetails) {
            if ("done".equals(detail.getString("status"))) {
                continue;
            }
            double baseQty = inventoryCheck.getBoolean("second_flag") ? detail.getDouble("second_qty") : detail.getDouble("first_qty");
            Records label = detail.getRec("label_id");
            // 标签原来的库存数, 如果是新标签这里就是0
            double qty = detail.getDouble("qty");
            double mapQty = Math.abs(Utils.round(qty - baseQty));
            label.set("status", "onhand");
            label.set("qty", baseQty);
            // 操作日志
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.inventory_check:scan_balance");
            log.put("related_id", inventoryBalance.getId());
            log.put("related_code", inventoryBalance.get("code"));
            label.call("logStatus", log);
            if (Utils.equals(qty, 0d)) {
                Map<String, Object> onhandMap = new HashMap<>();
                onhandMap.put("material_id", material.getId());
                onhandMap.put("ok_qty", mapQty);
                onhandMap.put("usable_qty", mapQty);
                onhandMap.put("company_id", warehouse.getRec("company_id").getId());
                onhandMap.put("label_id", label.getId());
                onhandMap.put("lot_num", label.getString("lot_num"));
                onhandMap.put("warehouse_id", warehouse.getId());
                onhandMap.put("location_id", detail.getRec("location_id").getId());
                onhandMap.put("status", "onhand");
                env.get("stock.onhand").create(onhandMap);
            } else {
                // 先查冻结数,对比盘点数, 多的生成入库数, 少的生成出库数, 入库类型盘盈入库,少的盘亏出库
                Cursor cr = env.getCursor();
                String sql = "update stock_onhand set ok_qty=ok_qty+%s,usable_qty=usable_qty+%s,status='onhand',frozen_qty=0 where material_id=%s and label_id=%s and warehouse_id=%s ";
                List<Object> params = new ArrayList<>(Arrays.asList(baseQty, baseQty, material.getId(), label.getId(), warehouse.getId()));
                cr.execute(sql, params);
            }
            // 判断是多还是少
            Map<String, Object> stockInDetailMap = new HashMap<>();
            stockInDetailMap.put("status", "done");
            stockInDetailMap.put("qty", mapQty);
            stockInDetailMap.put("warehouse_id", warehouse.getId());
            stockInDetailMap.put("location_id", detail.getRec("location_id").getId());
            stockInDetailMap.put("material_id", material.getId());
            stockInDetailMap.put("label_id", label.getId());
            stockInDetailMap.put("lot_num", label.get("lot_num"));
            stockInDetailMap.put("inventory_balance_id", inventoryBalance.getId());
            if (Utils.less(qty, baseQty)) {
                // 盈
                stockInDetailMap.put("stock_qty", mapQty);
                Records records = env.get("stock.stock_in_details").create(stockInDetailMap);
            } else if (Utils.large(qty, baseQty)) {
                // 亏
                Records records = env.get("stock.stock_out_details").create(stockInDetailMap);
            }
        }
    }
}
