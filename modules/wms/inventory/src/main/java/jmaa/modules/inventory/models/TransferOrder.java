package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.ArrayUtils;
import org.jmaa.sdk.util.KvMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Model.Meta(name = "wms.transfer_order", label = "仓库调拨", inherit = {"mixin.order_status", "code.auto_code", "mixin.company"})
public class TransferOrder extends Model {
    private static final Logger log = LoggerFactory.getLogger(TransferOrder.class);
    static Field code = Field.Char().label("调拨单号").help("自动生成");
    // 仓库
    static Field source_warehouse_ids = Field.Many2many("md.warehouse", "wms_transfer_warehouse", "transfer_id", "warehouse_id").ondelete(DeleteMode.Restrict).label("原仓库").required(true);
    static Field target_warehouse_id = Field.Many2one("md.warehouse").ondelete(DeleteMode.Restrict).label("目标仓库").required();
    static Field target_warehouse_contact = Field.Char().label("目标仓库联系人").related("target_warehouse_id.contact");
    static Field required_time = Field.DateTime().label("需求时间").required();
    static Field remark = Field.Char().label("备注");
    // 需求
    static Field line_ids = Field.One2many("wms.transfer_order_line", "transfer_order_id").label("调拨需求明细");
    // 明细
    static Field details_ids = Field.One2many("wms.transfer_order_details", "transfer_order_id").label("调拨明细");

    public static List<Object> getParentCompany(Records rec) {
        Records company = rec.getEnv().getCompany();
        return Arrays.asList(company.getId(), company.get("name"));
    }

    @OnSaved("source_warehouse_ids")
    public void onSourceWarehouseIdsSaved(Records record) {
        // 没提交之前都可以修改仓库
        Records transferLine = record.getRec("line_ids");
        if (transferLine.any()) {
            Set<String> baseWarehouseList = record.getRec("source_warehouse_ids").stream().map(Records::getId).collect(Collectors.toSet());
            for (Records line : transferLine) {
                Records warehouses = (Records) line.getRec("material_id").call("findWarehouses");
                // 获取交集,并拿一个
                Set<String> materialWarehouseList = warehouses.stream().map(Records::getId).collect(Collectors.toSet());
                materialWarehouseList.retainAll(baseWarehouseList);
                if (materialWarehouseList.isEmpty()) {
                    line.set("warehouse_id", null);
                } else {
                    line.set("warehouse_id", materialWarehouseList.iterator().next());
                }
            }
        }
    }

    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待调拨物料，根据出库规则推荐物料标签")
    public Map<String, Object> loadTransferMaterial(Records record,
                                                    @Doc("仓库") String warehouseId,
                                                    @Doc("偏移量") Integer offset) {
        Environment env = record.getEnv();
        Records lines = env.get("wms.transfer_order_line").find(Criteria.equal("transfer_order_id", record.getId())
            .and("status", "not in", Utils.asList("done", "transfered")));
        if (!lines.any()) {
            throw new ValidationException(record.l10n("没有待扫码调拨的物料"));
        }
        Records line = lines.browse();
        Records warehouse = env.get("md.warehouse");
        Records material = env.get("md.material");
        List<String> materialIds = lines.stream().map(l -> l.getRec("material_id").getId()).collect(Collectors.toList());
        String[] onhandMaterial = null;
        if (Utils.isNotEmpty(warehouseId)) {
            onhandMaterial = findOnhandMaterial(record, materialIds, warehouseId, offset);
        }
        if (Utils.isEmpty(onhandMaterial)) {
            onhandMaterial = findOnhandMaterial(record, materialIds, null, offset);
        }
        if (Utils.isNotEmpty(onhandMaterial)) {
            String materialId = onhandMaterial[0];
            line = lines.filter(l -> Utils.equals(materialId, l.getRec("material_id").getId()));
            warehouse = warehouse.browse(onhandMaterial[1]);
            material = line.getRec("material_id");
        }
        if (!line.any()) {
            int idx = Utils.toInt(offset) % lines.size();
            if (idx < 0) {
                idx += lines.size();
            }
            line = lines.browse(lines.getIds()[idx]);
            material = line.getRec("material_id");
        }
        double toTransferQty = Utils.round(line.getDouble("request_qty") - line.getDouble("transfer_qty"));
        return getTransferMaterialByWarehouse(record, material, warehouse, toTransferQty);
    }

    public Map<String, Object> getTransferMaterialByWarehouse(Records record, Records material, Records warehouse, double toTransferQty) {
        Environment env = record.getEnv();
        Records onhand = env.get("stock.onhand");
        if (warehouse.any() && Utils.large(toTransferQty, 0)) {
            onhand = findOnhandByRule(record, material, warehouse);
        }
        return getTransferMaterialByOnhand(record, material, warehouse, onhand, toTransferQty);
    }

    public Records findOnhandByRule(Records record, Records material, Records warehouse) {
        Environment env = record.getEnv();
        Records onhand = env.get("stock.onhand");
        Cursor cr = env.getCursor();
        String rule = warehouse.getString("stock_out_rule");
        String column = env.getConfig().getString("fifo_date");
        String stockRule = material.getString("stock_rule");
        String order = ("product_date".equals(column) && "sn".equals(stockRule) ? " order by l.product_date" : " order by s.stock_in_time")
            + ("lifo".equals(rule) ? " desc" : " asc");
        String sql = null;
        if ("sn".equals(stockRule)) {
            sql = "select s.id from stock_onhand s join lbl_material_label l on s.label_id=l.id " +
                "where s.material_id=%s and s.warehouse_id=%s and s.status='onhand' and s.usable_qty > 0";
        } else {
            sql = "select s.id from stock_onhand s " +
                "where s.material_id=%s and s.warehouse_id=%s and s.status='onhand'  and s.usable_qty > 0";
        }
        if (warehouse.getBoolean("bulk_fo")) {
            String bulkSql = sql;
            if ("sn".equals(stockRule)) {
                bulkSql = bulkSql + " and l.qty<l.original_qty" + order;
            }
            cr.execute(cr.getSqlDialect().getPaging(bulkSql, 1, 0), Utils.asList(material.getId(), warehouse.getId()));
            if (cr.getRowCount() > 0) {
                return onhand.browse((String) cr.fetchOne()[0]);
            }
        }
        cr.execute(cr.getSqlDialect().getPaging(sql + order, 1, 0), Utils.asList(material.getId(), warehouse.getId()));
        if (cr.getRowCount() > 0) {
            onhand = onhand.browse((String) cr.fetchOne()[0]);
        }
        return onhand;
    }

    public Map<String, Object> getTransferMaterialByOnhand(Records record, Records material, Records warehouse, Records onhand, double toTransferQty) {
        Map<String, Object> result = new HashMap<>();
        String stockRule = material.getString("stock_rule");
        if ("sn".equals(stockRule)) {
            result.put("id", record.getId());
            result.put("status", record.get("status"));
            result.put("suggest_location", onhand.getRec("location_id").get("code"));
            result.put("suggest_sn", onhand.get("sn"));
            result.put("material_id", material.getPresent());
            result.put("material_code", material.getString("code"));
            result.put("material_name_spec", material.get("name_spec"));
            result.put("to_transfer_qty", toTransferQty);
            result.put("warehouse_id", warehouse.getPresent());
            result.put("stock_rule", material.get("stock_rule"));
            result.put("abc_type", material.getSelection("abc_type"));
            result.put("unit", material.getRec("unit_id").get("present"));
            result.put("unit_accuracy", material.getRec("unit_id").get("accuracy"));
            result.put("source_warehouse_ids", record.getRec("source_warehouse_ids").getIds());
        } else {
            result.put("id", record.getId());
            result.put("status", record.get("status"));
            result.put("suggest_location", onhand.getRec("location_id").get("code"));
            result.put("suggest_sn", onhand.get("lot_num"));
            result.put("material_id", material.getPresent());
            result.put("material_code", material.getString("code"));
            result.put("material_name_spec", material.get("name_spec"));
            result.put("to_transfer_qty", toTransferQty);
            result.put("warehouse_id", warehouse.getPresent());
            result.put("stock_rule", material.get("stock_rule"));
            result.put("abc_type", material.getSelection("abc_type"));
            result.put("unit", material.getRec("unit_id").get("present"));
            result.put("unit_accuracy", material.getRec("unit_id").get("accuracy"));
            result.put("source_warehouse_ids", record.getRec("source_warehouse_ids").getIds());
            result.put("lot_out_qty", record.getEnv().getConfig().getBoolean("lot_out_qty"));
        }
        if (warehouse.any()) {
            Cursor cr = record.getEnv().getCursor();
            cr.execute("select sum(usable_qty) from stock_onhand where status='onhand' and material_id=%s and warehouse_id=%s",
                Utils.asList(material.getId(), warehouse.getId()));
            result.put("onhand_qty", cr.fetchOne()[0]);
        } else {
            result.put("onhand_qty", 0);
        }
        return result;
    }

    public String[] findOnhandMaterial(Records record, List<String> materialIds, String warehouseId, int offset) {
        Cursor cr = record.getEnv().getCursor();
        List<Object> params = new ArrayList<>();
        params.add(materialIds);
        String sql = "select distinct o.material_id,o.warehouse_id,l.code from stock_onhand o left join md_store_location l on o.location_id=l.id"
            + " where o.status='onhand' and o.material_id in %s and usable_qty > 0";
        if (Utils.isNotEmpty(warehouseId)) {
            sql += " and o.warehouse_id=%s order by l.code";
            params.add(warehouseId);
        } else {
            sql += " and o.warehouse_id in %s order by l.code";
            Records warehouses = record.getRec("source_warehouse_ids");
            params.add(warehouses.getIds());
        }
        cr.execute(sql, params);
        List<Object[]> rows = cr.fetchAll();
        if (!rows.isEmpty()) {
            Set<String> set = new LinkedHashSet<>();
            set.addAll(rows.stream().map(r -> r[0] + "," + r[1]).collect(Collectors.toList()));
            int idx = Utils.toInt(offset) % set.size();
            if (idx < 0) {
                idx += set.size();
            }
            return set.toArray(ArrayUtils.EMPTY_STRING_ARRAY)[idx].split(",");
        }
        return null;
    }

    @ServiceMethod(label = "调拨", doc = "物料编码则查询调拨需求，物料标签则调拨")
    public Object transfer(Records record, @Doc("标签条码/物料编码") String code) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            if (!material.any()) {
                throw new ValidationException(record.l10n("物料[%s]不存在", codes[1]));
            }
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                return transferByLabel(record, label);
            } else if ("lot".equals(stockRule)) {
                String lotNum = codes[2];
                if (env.getConfig().getBoolean("lot_out_qty")) {
                    Records lotSnTransient = record.getEnv().get("lbl.lot_status").find(Criteria.equal("order_id", record.getId())
                        .and(Criteria.equal("type", "wms.transfer_order")).and(Criteria.equal("material_id",material.getId()))
                        .and(Criteria.equal("lot_num", lotNum)).and(Criteria.equal("sn", codes[0])));
                    if (lotSnTransient.any()) {
                        // 存在,
                        throw new ValidationException(record.l10n("当前批次标签已使用,序列号[%s],请扫描其他标签", codes[0]));
                    }
                }
                return transferByLotAndNum(record, lotNum, material, code, Utils.toDouble(codes[codes.length - 1]));
            } else {
                // 数量
                return transferByLotAndNum(record, null, material, code, Utils.toDouble(codes[codes.length - 1]));
            }
        } else {
            Records label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
            if (label.any()) {
                material = label.getRec("material_id");
                if ("sn".equals(material.get("stock_rule"))) {
                    return transferByLabel(record, label);
                }
            }
        }
        if (!material.any()) {
            material = findMaterial(record, code);
        }
        if (!material.any()) {
            throw new ValidationException(record.l10n("条码[%s]无法识别", code));
        }
        if ("sn".equals(material.get("stock_rule"))) {
            throw new ValidationException(record.l10n("物料[%s]是序列号管控，请使用物料标签调拨", material.get("code")));
        }
        Records line = env.get("wms.transfer_order_line").find(Criteria.equal("transfer_order_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(line.l10n("物料[%s]不在调拨需求", material.get("code")));
        }
        Records warehouses = record.getRec("source_warehouse_ids");
        Cursor cr = env.getCursor();
        Records warehouse = env.get("md.warehouse");
        cr.execute("select warehouse_id, sum(usable_qty) from stock_onhand where material_id=%s " +
                "and warehouse_id in %s group by warehouse_id order by sum(usable_qty)",
            Utils.asList(material.getId(), warehouses.getIds()));
        if (cr.getRowCount() > 0) {
            warehouse = warehouse.browse((String) cr.fetchOne()[0]);
        }
        double toTransferQty = Utils.round(line.getDouble("request_qty") - line.getDouble("transfer_qty"));
        Map<String, Object> result = new HashMap<>();
        result.put("data", getTransferMaterialByWarehouse(record, material, warehouse, toTransferQty));
        result.put("message", line.l10n("条码[%s]读取物料成功", code));
        return result;
    }

    public Records findMaterial(Records record, String code) {
        Environment env = record.getEnv();
        Records lpn = env.get("packing.package").find(Criteria.equal("code", code));
        if (lpn.any()) {
            return lpn.getRec("material_id");
        }
        return env.get("md.material").find(Criteria.equal("code", code));
    }

    public Map<String, Object> transferByLotAndNum(Records record, String lotNum, Records material, String code, Double qty) {
        Environment env = record.getEnv();
        Records line = env.get("wms.transfer_order_line").find(Criteria.equal("transfer_order_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(record.l10n("标签[%s]物料[%s]不在调拨需求", code, material.get("present")));
        }
        double requestQty = line.getDouble("request_qty");
        double transferQty = line.getDouble("transfer_qty");
        double toTransferQty = Utils.round(requestQty - transferQty);
        Records onhand = null;
        String[] warehouseIds = record.getRec("source_warehouse_ids").getIds();
        String stockRule = material.getString("stock_rule");
        if ("lot".equals(stockRule)) {
            onhand = env.get("stock.onhand").find(Criteria.equal("lot_num", lotNum).and(Criteria.equal("material_id",material.getId())).and(Criteria.in("warehouse_id", warehouseIds)).and(Criteria.greater("usable_qty", 0)));
        } else {
            onhand = env.get("stock.onhand").find(Criteria.equal("material_id", material.getId())
                .and(Criteria.in("warehouse_id", warehouseIds))
                .and(Criteria.greater("usable_qty", 0))
                .and(Criteria.equal("label_id", null))
                .and(Criteria.equal("lot_num", null)));
        }
        if (!onhand.any()) {
            throw new ValidationException(record.l10n("无可用库存数据,请检查标签数据"));
        }
        onhand = onhand.first();
        Map<String, Object> mapList = onhand.read(Utils.asList("usable_qty", "location_id")).get(0);
        Map<String, Object> dataMap = getTransferMaterialByWarehouse(record, material, onhand.getRec("warehouse_id"), toTransferQty);
        dataMap.put("sn", code);
        dataMap.put("transfer_qty", mapList.get("usable_qty"));
        dataMap.put("location_id", mapList.get("location_id"));
        // 调拨数和标签数,
        if (Utils.large(Utils.toDouble(dataMap.get("to_transfer_qty")), qty)) {
            dataMap.put("transfer_qty", qty);
        } else {
            dataMap.put("transfer_qty", dataMap.get("to_transfer_qty"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("message", record.l10n("条码[%s]扫码识别成功", code));
        result.put("data", dataMap);
        return result;
    }

    public Object transferByLabel(Records record, Records label) {
        Environment env = record.getEnv();
        Records material = label.getRec("material_id");
        Records line = env.get("wms.transfer_order_line").find(Criteria.equal("transfer_order_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(record.l10n("标签[%s]物料[%s]不在调拨需求", label.get("sn"), material.get("present")));
        }
        Cursor cr = env.getCursor();
        //事务锁定调拨行
        cr.execute("update wms_transfer_order_line set id=id where id=%s", Arrays.asList(line.getId()));
        double requestQty = line.getDouble("request_qty");
        double transferQty = line.getDouble("transfer_qty");
        double toTransferQty = Utils.round(requestQty - transferQty);
        if (Utils.lessOrEqual(toTransferQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
        if (!onhand.any() || !"onhand".equals(onhand.getString("status"))) {
            String onhandStatus = onhand.any() ? record.l10n("库存状态为[%s]", onhand.getSelection("status")) : "";
            throw new ValidationException(record.l10n("物料标签[%s]状态为[%s]%s", label.get("sn"), label.getSelection("status"), onhandStatus));
        }
        String warehouseId = onhand.getRec("warehouse_id").getId();
        String[] warehouseIds = record.getRec("source_warehouse_ids").getIds();
        if (!Arrays.asList(warehouseIds).contains(warehouseId)) {
            throw new ValidationException(record.l10n("标签库存数据对应的仓库,非调拨单发货仓库,请检查数据"));
        }
        checkStockOutRules(record, onhand, label);
        double onhandQty = onhand.getDouble("usable_qty");
        Map<String, Object> result = new HashMap<>();
        if (Utils.lessOrEqual(onhandQty, toTransferQty) || canOverSend(record, onhand.getRec("warehouse_id"), material)) {
            createTransferDetails(record, onhand.getDouble("ok_qty"), onhand.getRec("material_id").getId(),
                onhand.getRec("label_id").getId(), onhand.getRec("warehouse_id").getId(), onhand.getRec("location_id").getId(), onhand.getString("lot_num"), material.getString("stock_rule"));
            onhand.set("allot_qty", onhandQty);
            onhand.set("usable_qty", 0);
            onhand.set("status", "allot");
            label.set("status", "allot");
            line.set("transfer_qty", transferQty + onhandQty);
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.transfer_order");
            log.put("related_id", record.getId());
            log.put("related_code", record.get("code"));
            label.call("logStatus", log);
            toTransferQty = Math.max(0, Utils.round(toTransferQty - onhandQty));
            result.put("data", getTransferMaterialByWarehouse(record, material, onhand.getRec("warehouse_id"), toTransferQty));
            result.put("message", record.l10n("条码[%s]数量[%s]调拨成功", label.get("sn"), onhand.get("allot_qty")));
        } else {
            Records unit = material.getRec("unit_id");
            Map<String, Object> data = new HashMap<>();
            data.put("accuracy", unit.getInteger("accuracy"));
            data.put("sn", label.get("sn"));
            data.put("qty", onhandQty);
            data.put("to_transfer_qty", toTransferQty);
            data.put("split_qty", toTransferQty);
            data.put("print_old", true);
            result.put("action", "split");
            result.put("data", getTransferMaterialByOnhand(record, material, onhand.getRec("warehouse_id"), onhand, toTransferQty));
            result.put("split", data);
            result.put("message", record.l10n("条码[%s]需要拆分", label.get("sn")));
        }
        return result;
    }

    public void checkStockOutRules(Records record, Records onhand, Records label) {
        Records warehouse = onhand.getRec("warehouse_id");
        if (warehouse.getBoolean("frozen")) {
            throw new ValidationException(record.l10n("仓库[%s]已冻结", warehouse.get("present")));
        }
        //散料先出，对比标签的当前数量和原始数量
        if (warehouse.getBoolean("bulk_fo") && Utils.equals(label.getDouble("qty"), label.getDouble("original_qty"))) {
            Records material = label.getRec("material_id");
            Cursor cr = record.getEnv().getCursor();
            cr.execute("select label_id from stock_onhand s join lbl_material_label l on s.label_id=l.id where l.qty<l.original_qty and s.material_id=%s and s.warehouse_id=%s and s.status='onhand'",
                Utils.asList(material.getId(), warehouse.getId()));
            List<Object[]> rows = cr.fetchAll();
            if (!rows.isEmpty()) {
                List<String> labelIds = rows.stream().limit(10).map(r -> (String) r[0]).collect(Collectors.toList());
                String sn = label.browse(labelIds).stream().map(l -> l.getString("sn")).collect(Collectors.joining(","));
                throw new ValidationException(record.l10n("散料先出，请先发条码[%s]", sn));
            }
        }
        String outRule = warehouse.getString("stock_out_rule");
        if ("fifo".equals(outRule)) {
            checkFifo(record, onhand, label);
        } else if ("lifo".equals(outRule)) {
            checkLifo(record, onhand, label);
        }
    }

    public void checkFifo(Records record, Records onhand, Records label) {
        Cursor cr = record.getEnv().getCursor();
        Records warehouse = onhand.getRec("warehouse_id");
        Records material = onhand.getRec("material_id");
        int accuracy = Math.max(1, warehouse.getInteger("stock_out_accuracy"));
        String field = record.getEnv().getConfig().getString("fifo_date");
        Function<Cursor, String> computeSn = (c) -> {
            List<Object[]> rows = c.fetchAll();
            List<String> labelIds = rows.stream().limit(10).map(r -> (String) r[0]).collect(Collectors.toList());
            return label.browse(labelIds).stream().map(l -> l.getString("sn")).collect(Collectors.joining(","));
        };
        if ("stock_in_time".equals(field)) {
            cr.execute("select min(stock_in_time) from stock_onhand where material_id=%s and warehouse_id=%s and status='onhand'",
                Utils.asList(material.getId(), warehouse.getId()));
            Date dt = (Date) cr.fetchOne()[0];
            dt = Utils.Dates.addDays(dt, accuracy);
            Date stockTime = onhand.getDateTime("stock_in_time");
            if (stockTime.after(dt)) {
                cr.execute("select label_id from stock_onhand where material_id=%s and warehouse_id=%s and stock_in_time<=%s and status='onhand'",
                    Utils.asList(material.getId(), warehouse.getId(), dt));
                String sn = computeSn.apply(cr);
                throw new ValidationException(record.l10n("先进先出，请先发条码[%s]", sn));
            }
        } else {
            cr.execute("select min(l.product_date) from stock_onhand s join lbl_material_label l on s.label_id=l.id where s.material_id=%s and s.warehouse_id=%s and s.status='onhand'",
                Utils.asList(material.getId(), warehouse.getId()));
            Date dt = (Date) cr.fetchOne()[0];
            dt = Utils.Dates.addDays(dt, accuracy);
            Date productDate = onhand.getRec("label_id").getDate("product_date");
            if (productDate.after(dt)) {
                cr.execute("select label_id from stock_onhand s join lbl_material_label l on s.label_id=l.id where s.material_id=%s and s.warehouse_id=%s and l.product_date<=%s and s.status='onhand'",
                    Utils.asList(material.getId(), warehouse.getId(), dt));
                String sn = computeSn.apply(cr);
                throw new ValidationException(record.l10n("先进先出，请先发条码[%s]", sn));
            }
        }
    }

    public void checkLifo(Records record, Records onhand, Records label) {
        Cursor cr = record.getEnv().getCursor();
        Records warehouse = onhand.getRec("warehouse_id");
        Records material = onhand.getRec("material_id");
        int accuracy = Math.max(1, warehouse.getInteger("stock_out_accuracy"));
        String field = record.getEnv().getConfig().getString("fifo_date");
        Function<Cursor, String> computeSn = (c) -> {
            List<Object[]> rows = c.fetchAll();
            List<String> labelIds = rows.stream().limit(10).map(r -> (String) r[0]).collect(Collectors.toList());
            return label.browse(labelIds).stream().map(l -> l.getString("sn")).collect(Collectors.joining(","));
        };
        if ("stock_in_time".equals(field)) {
            cr.execute("select max(stock_in_time) from stock_onhand where material_id=%s and warehouse_id=%s and status='onhand'",
                Utils.asList(material.getId(), warehouse.getId()));
            Date dt = (Date) cr.fetchOne()[0];
            dt = Utils.Dates.addDays(dt, -accuracy);
            Date stockTime = onhand.getDateTime("stock_in_time");
            if (stockTime.before(dt)) {
                cr.execute("select label_id from stock_onhand where material_id=%s and warehouse_id=%s and stock_in_time>=%s and status='onhand'",
                    Utils.asList(material.getId(), warehouse.getId(), dt));
                String sn = computeSn.apply(cr);
                throw new ValidationException(record.l10n("后进先出，请先发条码[%s]", sn));
            }
        } else {
            cr.execute("select max(l.product_date) from stock_onhand s join lbl_material_label l on s.label_id=l.id where s.material_id=%s and s.warehouse_id=%s and s.status='onhand'",
                Utils.asList(material.getId(), warehouse.getId()));
            Date dt = (Date) cr.fetchOne()[0];
            dt = Utils.Dates.addDays(dt, -accuracy);
            Date productDate = onhand.getRec("label_id").getDate("product_date");
            if (productDate.before(dt)) {
                cr.execute("select label_id from stock_onhand s join lbl_material_label l on s.label_id=l.id where s.material_id=%s and s.warehouse_id=%s and l.product_date>=%s and s.status='onhand'",
                    Utils.asList(material.getId(), warehouse.getId(), dt));
                String sn = computeSn.apply(cr);
                throw new ValidationException(record.l10n("后进先出，请先发条码[%s]", sn));
            }
        }
    }

    public boolean canOverSend(Records record, Records warehouse, Records material) {
        if (!warehouse.getBoolean("over_send")) {
            return false;
        }
        String abcType = material.getString("abc_type");
        if (Utils.isNotEmpty(abcType)) {
            String abc = record.getEnv().getConfig().getString("abc_exceed");
            if (Utils.isNotEmpty(abc)) {
                return Utils.asList(abc.split(",")).contains(abcType);
            }
        }
        return false;
    }

    public Records createTransferDetails(Records record, Double qty, String materialId, String labelId, String warehouseId, String locationId, String lot_num, String stockRule) {
        Map<String, Object> data = new HashMap<>();
        data.put("transfer_order_id", record.getId());
        data.put("qty", qty);
        data.put("label_id", labelId);
        data.put("lot_num", lot_num);
        data.put("material_id", materialId);
        data.put("warehouse_id", warehouseId);
        data.put("location_id", locationId);
        return record.getEnv().get("wms.transfer_order_details").create(data);
    }

    @ServiceMethod(label = "拆分标签", doc = "拆分生成新标签")
    public Object splitLabel(Records record, @Doc("条码") String sn, @Doc("拆分数量") Double splitQty, @Doc("是否打印原标签") Boolean printOld) {
        Records newLabel = (Records) record.getEnv().get("lbl.material_label").call("splitLabel", sn, splitQty);
        Records printTemplate = newLabel.getRec("print_template_id");
        List<String> labelIds = new ArrayList<>();
        if (printOld) {
            Records label = newLabel.find(Criteria.equal("sn", sn));
            labelIds.add(label.getId());
        }
        labelIds.add(newLabel.getId());
        Map<String, Object> printData = (Map<String, Object>) printTemplate.call("print", new KvMap().set("labels", newLabel.browse(labelIds)));
        printData.put("newSn", newLabel.get("sn") + "|" + newLabel.getRec("material_id").get("code"));
        return printData;
    }

    @ServiceMethod(label = "调拨", doc = "数量管控的物料进行调拨", auth = "transfer")
    public Object transferMaterial(Records record, @Doc("标签") String code, @Doc("仓库") String warehouseId,
                                   @Doc("库位") String locationId, @Doc("物料") String materialId, @Doc("调拨数量") Double qty,
                                   @Doc("是否打印") Boolean printFlag, @Doc("标签模板") String templateId) {
        if (Utils.lessOrEqual(qty, 0)) {
            throw new ValidationException(record.l10n("调拨数量必须大于0"));
        }
        if (printFlag && Utils.isBlank(templateId)) {
            throw new ValidationException(record.l10n("打印标签,请选择标签模板"));
        }
        Environment env = record.getEnv();
        Records line = env.get("wms.transfer_order_line").find(Criteria.equal("transfer_order_id", record.getId()).and("material_id", "=", materialId));
        if (!line.any()) {
            throw new ValidationException("找不到调拨物料");
        }
        Cursor cr = env.getCursor();
        //事务锁定调拨行
        cr.execute("update wms_transfer_order_line set id=id where id=%s", Arrays.asList(line.getId()));
        Records material = env.get("md.material", materialId);
        double requestQty = line.getDouble("request_qty");
        double transferQty = line.getDouble("transfer_qty");
        double toTransferQty = Utils.round(requestQty - transferQty);
        if (Utils.lessOrEqual(toTransferQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        // 标签码扫扫码的时候已经校验过了,
        Records onhand = null;
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        if ("lot".equals(material.get("stock_rule"))) {
            onhand = record.getEnv().get("stock.onhand").find(Criteria.equal("material_id", materialId).and("lot_num", "=", codes[2])
                .and("warehouse_id", "=", warehouseId).and("location_id", "=", locationId));
        } else {
            onhand = record.getEnv().get("stock.onhand").find(Criteria.equal("material_id", materialId)
                .and("warehouse_id", "=", warehouseId).and("location_id", "=", locationId)
                .and("label_id", "=", null).and("lot_num", "=", null));
        }
        if (!onhand.any()) {
            if (Utils.isNotBlank(locationId)) {
                throw new ValidationException(record.l10n("仓库[%s]/库位[%s]没有物料[%s]库存", env.get("md.warehouse", warehouseId).get("present"), env.get("md.store_location", locationId).get("present"), material.get("code")));
            } else {
                throw new ValidationException(record.l10n("仓库[%s]没有物料[%s]库存", env.get("md.warehouse", warehouseId).get("present"), material.get("code")));
            }
        }
        if (!"onhand".equals(onhand.getString("status"))) {
            throw new ValidationException(record.l10n("物料[%s]库存状态为[%s]", material.get("code"), onhand.getSelection("status")));
        }
        Records warehouse = onhand.getRec("warehouse_id");
        if (warehouse.getBoolean("frozen")) {
            throw new ValidationException(record.l10n("仓库[%s]已冻结", warehouse.get("present")));
        }
        if (Utils.large(qty, onhand.getDouble("usable_qty"))) {
            throw new ValidationException(record.l10n("调拨数量[%s]不能大于库存可用数量[%s]", qty, onhand.getDouble("usable_qty")));
        }
        if (Utils.large(qty, toTransferQty) && !canOverSend(record, warehouse, material)) {
            throw new ValidationException(record.l10n("调拨数量[%s]不能大于待调拨数量[%s]", qty, toTransferQty));
        }
        line.set("transfer_qty", transferQty + qty);
        String sql = "";
        if (Utils.isNotBlank(locationId)) {
            sql = " and location_id = %s ";
        } else {
            sql = " and location_id is %s  ";
        }
        if ("lot".equals(material.get("stock_rule"))) {
            Records transferDetails = createTransferDetails(record, qty, materialId, null, warehouseId, locationId, codes[2], "lot");
            if (env.getConfig().getBoolean("lot_out_qty")) {
                Records lotSnTransient = env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId())
                    .and(Criteria.equal("type", "wms.transfer_order")).and(Criteria.equal("material_id",material.getId()))
                    .and(Criteria.equal("lot_num", codes[2])).and(Criteria.equal("sn", codes[0])));
                if (lotSnTransient.any()) {
                    // 存在,
                    throw new ValidationException(record.l10n("当前批次标签已使用,序列号[%s],请扫描其他标签", codes[0]));
                }
                Map<String, Object> data = new HashMap<>();
                data.put("order_id", record.getId());
                data.put("sn", codes[0]);
                data.put("material_id", materialId);
                data.put("lot_num", codes[2]);
                data.put("type", "wms.transfer_order");
                data.put("detail_id", transferDetails.getId());
                env.get("lbl.lot_status").create(data);
            }
            cr.execute("update stock_onhand set usable_qty=usable_qty-%s, allot_qty=allot_qty+%s where lot_num = %s and  material_id=%s and warehouse_id=%s " + sql,
                Arrays.asList(qty, qty, codes[2], materialId, warehouseId, locationId));
        } else {
            createTransferDetails(record, qty, materialId, null, warehouseId, locationId, null, "num");
            cr.execute("update stock_onhand set usable_qty=usable_qty-%s, allot_qty=allot_qty+%s where material_id=%s and warehouse_id=%s  and lot_num is null and label_id is null" + sql,
                Arrays.asList(qty, qty, materialId, warehouseId, locationId));
        }
        Map<String, Object> result = new HashMap<>();
        toTransferQty = Math.max(0, Utils.round(toTransferQty - qty));
        result.put("data", getTransferMaterialByWarehouse(record, material, warehouse, toTransferQty));
        result.put("message", record.l10n("物料[%s]数量[%s]调拨成功", material.get("code"), qty));
        result.put("printMap", printFlag ? getPrintMap(material, qty, templateId, code, codes) : Collections.emptyMap());
        return result;
    }

    public Map<String, Object> getPrintMap(Records material, Double qty, String templateId, String code, String[] codes) {
        String sn = null;
        String lotNum = null;
        if ("lot".equals(material.getString("stock_rule"))) {
            lotNum = codes[2];
            sn = ((List<String>) material.getEnv().get("lbl.material_label").call("createCodes", material.getId(), 1)).get(0);
        } else {
            sn = codes[0];
        }
        Map<String, Object> labelData = new KvMap()
            .set("code", code)
            .set("sn", sn)
            .set("lot_num", lotNum)
            .set("qty", qty)
            .set("material_code", material.get("code"))
            .set("material_name", material.get("name"))
            .set("material_spec", material.get("spec"))
            .set("unit", material.getRec("unit_id").get("name"))
            .set("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        Records printTemplate = material.getEnv().get("print.template", templateId);
        return (Map<String, Object>) printTemplate.call("print", new KvMap().set("data", Utils.asList(labelData)));
    }

    @ServiceMethod(label = "出库", doc = "根据调拨明细生成出库单")
    public Object stockOut(Records records) {
        for (Records record : records) {
            Records details = record.getEnv().get("wms.transfer_order_details").find(Criteria.equal("transfer_order_id", record.getId()).and("stock_out_id", "=", null));
            if (!details.any()) {
                throw new ValidationException("没有可出库物料");
            }
            Records targetWarehouse = record.getRec("target_warehouse_id");
            boolean transferReceiptFlag = targetWarehouse.getBoolean("transfer_receipt");
            Set<String> materialIds = new HashSet<>();
            Map<String, List<Map<String, Object>>> stockInMap = new HashMap<>();
            // 标签相同,库位不同,之前没合并,入库单需要合并
            for (Records detail : details) {
                Records material = detail.getRec("material_id");
                String materialId = material.getId();
                materialIds.add(materialId);
                // 相同的标签需要合并,直接在这里合并
                List<Map<String, Object>> detailMap = detail.read(Utils.asList("qty", "lot_num", "label_id", "material_id"));
                if (stockInMap.containsKey(materialId)) {
                    List<Map<String, Object>> maps = stockInMap.get(materialId);
                    maps.addAll(detailMap);
                    stockInMap.put(materialId, maps);
                    continue;
                }
                stockInMap.put(materialId, detailMap);
            }
            Records lines = record.getEnv().get("wms.transfer_order_line").find(
                Criteria.equal("transfer_order_id", record.getId()).and("material_id", "in", materialIds).and("status", "=", "transfered"));
            lines.set("status", "done");
            String message = record.l10n("出库");
            Records stockOut = record.getEnv().get("stock.stock_out").find(Criteria.equal("related_id", record.getId()));
            if (!stockOut.any()) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "transfer");
                data.put("related_code", record.get("code"));
                data.put("related_model", record.getMeta().getName());
                data.put("related_id", record.getId());
                stockOut = stockOut.create(data);
                message += record.l10n("，出库单: %s", stockOut.get("code"));
            }
            details.set("stock_out_id", stockOut.getId());
            if (transferReceiptFlag) {
                // 调拨接收
                stockOut.call("stockOut");
            } else {
                // 调拨不需要扫码接收,库存转移
                withoutTransferReceipt(record, targetWarehouse);
                stockOut.set("status", "done");
            }
            // 生成入库单  注意,这里可能点多次, 每次都生成新的,
            createStockIn(record, stockInMap, record.getString("code"), targetWarehouse.getId(), transferReceiptFlag);
            record.call("trackMessage", message);
            // 直接删
            record.getEnv().get("lbl.lot_status").find(Criteria.equal("order_id", record.getId()).and(Criteria.equal("type", "wms.transfer_order"))).delete();
        }
        updateMaterialTransferStatus(records);
        return Action.reload(records.l10n("出库成功"));
    }

    public void withoutTransferReceipt(Records record, Records targetWarehouse) {
        // 这里的需要直接入库
        Environment env = record.getEnv();
        Records stockOnhand = env.get("stock.onhand");
        String targetWarehouseId = targetWarehouse.getId();
        Records details = env.get("wms.transfer_order_details").find(Criteria.equal("transfer_order_id", record.getId()).and("status", "=", "new"));
        for (Records detail : details) {
            Records material = detail.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            Records warehouse = detail.getRec("warehouse_id");
            Records location = detail.getRec("location_id");
            double qty = detail.getDouble("qty");
            if ("sn".equals(stockRule)) {
                Records label = detail.getRec("label_id");
                label.set("status", "onhand");
                label.set("warehouse_id", targetWarehouseId);
                label.set("location_id", null);
                Records onhand = stockOnhand.find(Criteria.equal("label_id", label.getId()).and(Criteria.equal("material_id", material.getId())));
                onhand.ensureOne();
                onhand.set("usable_qty", onhand.get("allot_qty"));
                onhand.set("allot_qty", 0);
                onhand.set("status", "onhand");
                onhand.set("location_id", null);
                onhand.set("warehouse_id", targetWarehouseId);
                onhand.set("company_id", targetWarehouse.getRec("company_id").getId());
                onhand.set("stock_in_time", new Date());
                // 处理好了序列号库存数据
                Map<String, Object> log = new HashMap<>();
                // 调拨直接入库,无需出库,直接展示调拨单
                log.put("operation", "wms.transfer_order:receipt");
                log.put("related_id", record.getId());
                log.put("related_code", record.get("code"));
                label.call("logStatus", log);
            } else {
                Criteria sourceCriteria = null;
                Criteria targetCriteria = null;
                String lotNum = null;
                if ("lot".equals(stockRule)) {
                    // 扣减库存数量
                    lotNum = detail.getString("lot_num");
                    sourceCriteria = Criteria.equal("lot_num", lotNum)
                        .and(Criteria.equal("material_id", material.getId()))
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", location.getId()));
                    targetCriteria = Criteria.equal("lot_num", lotNum)
                        .and(Criteria.equal("material_id", material.getId()))
                        .and(Criteria.equal("warehouse_id", targetWarehouseId))
                        .and(Criteria.equal("location_id", null));
                } else {
                    sourceCriteria = Criteria.equal("material_id", material.getId())
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", location.getId()))
                        .and(Criteria.equal("lot_num", null))
                        .and(Criteria.equal("label_id", null));
                    targetCriteria = Criteria.equal("material_id", material.getId())
                        .and(Criteria.equal("warehouse_id", targetWarehouseId))
                        .and(Criteria.equal("location_id", null))
                        .and(Criteria.equal("lot_num", null))
                        .and(Criteria.equal("label_id", null));
                }
                // 调拨仓库存
                Records sourceStockOnhand = stockOnhand.find(sourceCriteria);
                // 这种就只能有一条,
                sourceStockOnhand.set("ok_qty", Utils.round(sourceStockOnhand.getDouble("ok_qty") - qty));
                sourceStockOnhand.set("allot_qty", Utils.round(sourceStockOnhand.getDouble("allot_qty") - qty));
                // 目标仓库存
                Records targetStockOnhand = stockOnhand.find(targetCriteria);
                if (targetStockOnhand.any()) {
                    // 目标仓 当前批次数据存在,
                    targetStockOnhand.set("ok_qty", targetStockOnhand.getDouble("ok_qty") + qty);
                    targetStockOnhand.set("usable_qty", targetStockOnhand.getDouble("usable_qty") + qty);
                    targetStockOnhand.set("stock_in_time", new Date());
                } else {
                    // 目标仓,不存在这个批次数据 规范的管控条件
                    Map<String, Object> targetDataMap = new HashMap<>();
                    targetDataMap.put("ok_qty", qty);
                    targetDataMap.put("usable_qty", qty);
                    targetDataMap.put("allot_qty", 0);
                    targetDataMap.put("location_id", null);
                    targetDataMap.put("warehouse_id", targetWarehouseId);
                    targetDataMap.put("company_id", targetWarehouse.getRec("company_id").getId());
                    targetDataMap.put("stock_in_time", new Date());
                    targetDataMap.put("material_id", material.getId());
                    targetDataMap.put("lot_num", lotNum);
                    targetDataMap.put("status", "onhand");
                    stockOnhand.create(targetDataMap);
                }
                if (Utils.equals(sourceStockOnhand.getDouble("usable_qty"), 0d) && Utils.equals(sourceStockOnhand.getDouble("ok_qty"), 0d) && Utils.equals(sourceStockOnhand.getDouble("allot_qty"), 0d)) {
                    sourceStockOnhand.delete();
                }
            }
        }
        details.set("status", "done");
    }

    public void createStockIn(Records record, Map<String, List<Map<String, Object>>> stockInMap, String transferCode, String warehouseId, Boolean transferReceiptFlag) {
        Map<String, Object> stockMap = new HashMap<>();
        stockMap.put("type", "wms.transfer_order");
        stockMap.put("related_code", transferCode);
        stockMap.put("related_model", "wms.transfer_order");
        stockMap.put("related_id", record.getId());
        stockMap.put("status", transferReceiptFlag ? "new" : "done");
        Records stockIn = record.getEnv().get("stock.stock_in").create(stockMap);
        // 明细
        Records stockInDetails = record.getEnv().get("stock.stock_in_details");
        List<Map<String, Object>> maps = mergeStockData(record, stockInMap, stockIn.getId(), warehouseId, transferReceiptFlag);
        stockInDetails.createBatch(maps);
    }

    // 不需要继承 , 返回合并 lot_num, label_id ,双null 数据,
    public List<Map<String, Object>> mergeStockData(Records record, Map<String, List<Map<String, Object>>> stockInMap, String stockInId, String warehouseId, Boolean transferReceiptFlag) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历每个material_id组
        for (Map.Entry<String, List<Map<String, Object>>> entry : stockInMap.entrySet()) {
            String materialId = entry.getKey();
            List<Map<String, Object>> detailList = entry.getValue();
            // 内部分组Map: 分组键 -> 记录列表
            Map<Object, List<Map<String, Object>>> innerGroup = new HashMap<>();
            // 对当前material_id下的记录进行分组
            for (Map<String, Object> detailMap : detailList) {
                Object lotNum = detailMap.get("lot_num");
                Object labelId = detailMap.get("label_id");
                // 确定分组键
                Object groupKey;
                if (Utils.isNotEmpty(labelId)) {
                    // 按label_id分组
                    groupKey = "LABEL:" + labelId;
                } else if (Utils.isNotEmpty(lotNum)) {
                    // 按lot_num分组
                    groupKey = "LOT:" + lotNum;
                } else {
                    // 双null组
                    groupKey = "NULL_GROUP";
                }
                // 添加到对应分组
                innerGroup.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(detailMap);
            }
            // 合并每个分组
            for (List<Map<String, Object>> group : innerGroup.values()) {
                Double totalQty = 0d;
                Object lotNum = null;
                Object labelId = null;
                // 计算总qty并获取标识字段
                for (Map<String, Object> detailMap : group) {
                    totalQty += Utils.toDouble(detailMap.get("qty"));
                    // 保留第一个非空值
                    if (Utils.isEmpty(labelId)) {
                        labelId = detailMap.get("label_id");
                    }
                    if (Utils.isEmpty(lotNum)) {
                        lotNum = detailMap.get("lot_num");
                    }
                }
                // 创建合并后的记录
                Map<String, Object> merged = new HashMap<>();
                merged.put("stock_in_id", stockInId);
                merged.put("warehouse_id", warehouseId);
                merged.put("status", "to-stock");
                merged.put("material_id", materialId);
                merged.put("qty", totalQty);
                merged.put("stock_qty", totalQty);
                // 根据分组类型设置字段
                if (Utils.isNotEmpty(labelId)) {
                    merged.put("lot_num", lotNum);
                    merged.put("label_id", labelId);
                } else if (Utils.isNotEmpty(lotNum)) {
                    merged.put("lot_num", lotNum);
                    merged.put("label_id", null);
                } else {
                    merged.put("lot_num", null);
                    merged.put("label_id", null);
                }
                if (!transferReceiptFlag) {
                    // 无需接收,直接完成
                    merged.put("status", "done");
                }
                result.add(merged);
            }
        }
        return result;
    }

    public void updateMaterialTransferStatus(Records records) {
        //执行sql前flush保存
        records.flush();
        Cursor cr = records.getEnv().getCursor();
        String sql = "select distinct status from wms_transfer_order_line where transfer_order_id=%s";
        for (Records record : records) {
            cr.execute(sql, Arrays.asList(record.getId()));
            List<String> status = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
            boolean done = status.stream().allMatch(s -> "done".equals(s));
            if (done) {
                //如果全部为完成状态，则更新为已完成状态
                record.set("status", "done");
                break;
            }
        }
    }

    @ActionMethod
    public Action onTargetWarehouseIdChange(Records rec) {
        AttrAction action = new AttrAction();
        Records warehouse = rec.getRec("target_warehouse_id");
        if (warehouse.any()) {
            action.setValue("target_warehouse_contact", warehouse.getString("contact"));
        } else {
            action.setValue("target_warehouse_id", null);
        }
        return action;
    }

    @Model.ServiceMethod(doc = "仓库调拨单打印", label = "仓库调拨单打印", auth = "read")
    public Map<String, Object> printTransferOrder(Records records) {
        Records tpl = records.getEnv().get("print.template").find(Criteria.equal("rule", "print.rule.transfer_order")
            .and("active", "=", true), 0, 1, "");
        if (tpl.any()) {
            Map<String, Object> printResult = (Map<String, Object>) tpl.call("print", new KvMap() {{
                put("transferOrder", records);
            }});
            return printResult;
        }
        throw new ValidationException(records.l10n("没有配置仓库调拨单打印模板"));
    }
}
