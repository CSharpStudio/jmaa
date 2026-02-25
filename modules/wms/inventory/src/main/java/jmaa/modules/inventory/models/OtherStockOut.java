package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.ArrayUtils;
import org.jmaa.sdk.util.KvMap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Model.Meta(name = "wms.other_stock_out", label = "其它出库", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class OtherStockOut extends Model {
    static Field code = Field.Char().label("其它出库单号").unique().readonly();
    static Field type = Field.Selection(new Options() {{
        put("customer", "赠客户");
        put("supplier", "赠供应商");
        put("issue", "小工单发料");
        put("maintenance", "维修材料");
        put("scrap", "报废出库");
        put("compensation", "赔偿出库");
        put("expressage", "物流损耗补发");
        put("feed", "生产上料");
    }}).label("其它出库类型").required(true).defaultValue("customer");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("发货仓库");
    static Field remark = Field.Char().label("备注");
    static Field line_ids = Field.One2many("wms.other_stock_out_line", "other_stock_out_id").label("物料明细");
    static Field details_ids = Field.One2many("wms.other_stock_out_details", "other_stock_out_id").label("扫码明细");

    /**
     * 指定仓库时按库位顺序加载下一个待发物料。
     * 根据出库规则推荐物料标签。
     */
    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待发物料，根据出库规则推荐物料标签")
    public Object loadStockOutMaterial(Records record,
                                       @Doc("仓库") String warehouseId,
                                       @Doc("偏移量") Integer offset) {
        Environment env = record.getEnv();
        Records lines = env.get("wms.other_stock_out_line").find(Criteria.equal("other_stock_out_id", record.getId())
            .and("status", "in", Utils.asList("new", "delivering")));
        if (!lines.any()) {
            throw new ValidationException(record.l10n("没有待发物料"));
        }
        Records line = lines.browse();
        Records warehouse = env.get("md.warehouse", warehouseId);
        Records material = env.get("md.material");
        List<String> materialIds = lines.stream().map(l -> l.getRec("material_id").getId()).collect(Collectors.toList());
        //指定仓库时，按仓库找物料，如果没找到，在所有仓库中找有库存的物料
        String[] onhandMaterial = findOnhandMaterial(record, materialIds, warehouseId, offset);
        // 表头选了仓库,扫码不让再变更仓库
        /*if (Utils.isNotEmpty(warehouseId)) {
            onhandMaterial = findOnhandMaterial(record, materialIds, warehouseId, offset);
        }
        if (Utils.isEmpty(onhandMaterial)) {
            onhandMaterial = findOnhandMaterial(record, materialIds, null, offset);
        }*/
        if (Utils.isNotEmpty(onhandMaterial)) {
            String materialId = onhandMaterial[0];
            line = lines.filter(l -> Utils.equals(materialId, l.getRec("material_id").getId()));
            /*warehouse = warehouse.browse(onhandMaterial[1]);*/
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
        double toDeliveryQty = Utils.round(line.getDouble("request_qty") - line.getDouble("scan_qty"));
        return getStockOutMaterialByWarehouse(record, material, warehouse, toDeliveryQty);
    }

    /**
     * 根据仓库获取推荐的出库信息
     */
    public Map<String, Object> getStockOutMaterialByWarehouse(Records record, Records material, Records warehouse, double toDeliveryQty) {
        Environment env = record.getEnv();
        Records onhand = env.get("stock.onhand");
        if (warehouse.any() && Utils.large(toDeliveryQty, 0)) {
            onhand = findOnhandByRule(record, material, warehouse);
        }
        return getStockOutMaterialByOnhand(record, material, warehouse, onhand, toDeliveryQty);
    }

    /**
     * 根据出库规则(散料先出、先进先出、后进先出)查找库存
     */
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

    /**
     * 根据onhand获取出库信息
     */
    public Map<String, Object> getStockOutMaterialByOnhand(Records record, Records material, Records warehouse, Records onhand, double toDeliveryQty) {
        Map<String, Object> result = new HashMap<>();
        String stockRule = material.getString("stock_rule");
        Records unit = material.getRec("unit_id");
        result.put("id", record.getId());
        result.put("status", record.get("status"));
        result.put("suggest_location", onhand.getRec("location_id").get("code"));
        result.put("material_id", material.getPresent());
        result.put("material_name_spec", material.get("name_spec"));
        result.put("to_delivery_qty", toDeliveryQty);
        result.put("warehouse_id", warehouse.getPresent());
        result.put("stock_rule", material.get("stock_rule"));
        result.put("abc_type", material.getSelection("abc_type"));
        result.put("unit_id", unit.getPresent());
        result.put("unit_accuracy", unit.get("accuracy"));
        if ("sn".equals(stockRule)) {
            result.put("suggest_sn", onhand.get("sn"));
        } else {
            result.put("suggest_sn", onhand.get("lot_num"));
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

    /**
     * 查找有库存的物料，指定仓库时，在指定的仓库找，没指定仓库时，在所有出库仓库找。
     *
     * @return 找到返回物料和仓库[materialId, warehouseId]，否则返回null
     */
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
            Records warehouses = record.getRec("warehouse_id");
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

    @ServiceMethod(label = "出库", doc = "物料编码则查询出库需求，物料标签则出库")
    public Object delivery(Records record, @Doc("标签条码/物料编码") String code) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            if (!material.any()) {
                throw new ValidationException(material.l10n("物料[%s]不存在", codes[1]));
            }
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                return stockOutByLabel(record, label);
            } else if ("lot".equals(stockRule)) {
                String lotNum = codes[2];
                if (env.getConfig().getBoolean("lot_out_qty")) {
                    Records lotSnTransient = env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId())
                        .and(Criteria.equal("type", "wms.other_stock_out"))
                        .and(Criteria.equal("lot_num", lotNum))
                        .and(Criteria.equal("material_id",material.getId()))
                        .and(Criteria.equal("sn", codes[0])));
                    if (lotSnTransient.any()) {
                        // 存在,
                        throw new ValidationException(record.l10n("当前批次标签已使用,序列号[%s],请扫描其他标签", codes[0]));
                    }
                }
                return stockOutByLotAndNum(record, lotNum, material, code, Utils.toDouble(codes[codes.length - 1]));
            } else {
                // 数量
                return stockOutByLotAndNum(record, null, material, code, Utils.toDouble(codes[codes.length - 1]));
            }
        } else {
            Records label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
            if (label.any()) {
                material = label.getRec("material_id");
                if ("sn".equals(material.get("stock_rule"))) {
                    return stockOutByLabel(record, label);
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
            throw new ValidationException(record.l10n("物料[%s]是序列号管控，请使用物料标签出库", material.get("code")));
        }
        Records line = env.get("wms.other_stock_out_line").find(Criteria.equal("other_stock_out_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(line.l10n("物料[%s]不在出库需求", material.get("code")));
        }
        Records warehouses = record.getRec("warehouse_id");
        Cursor cr = env.getCursor();
        Records warehouse = env.get("md.warehouse");
        cr.execute("select warehouse_id, sum(usable_qty) from stock_onhand where material_id=%s " +
                "and warehouse_id in %s group by warehouse_id order by sum(usable_qty)",
            Utils.asList(material.getId(), warehouses.getIds()));
        if (cr.getRowCount() > 0) {
            warehouse = warehouse.browse((String) cr.fetchOne()[0]);
        }
        double toDeliveryQty = Utils.round(line.getDouble("request_qty") - line.getDouble("scan_qty"));
        Map<String, Object> result = new HashMap<>();
        result.put("data", getStockOutMaterialByWarehouse(record, material, warehouse, toDeliveryQty));
        result.put("message", line.l10n("条码[%s]读取物料成功", code));
        return result;
    }

    /**
     * 按物料标签出库
     */
    public Object stockOutByLabel(Records record, Records label) {
        Environment env = record.getEnv();
        Records material = label.getRec("material_id");
        Records line = env.get("wms.other_stock_out_line").find(Criteria.equal("other_stock_out_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(record.l10n("标签[%s]物料[%s]不在出库需求", label.get("sn"), material.get("present")));
        }
        Cursor cr = env.getCursor();
        //事务锁定出库行
        cr.execute("update wms_other_stock_out_line set id=id where id=%s", Arrays.asList(line.getId()));
        double requestQty = line.getDouble("request_qty");
        double scanQty = line.getDouble("scan_qty");
        double toDeliveryQty = Utils.round(requestQty - scanQty);
        if (Utils.lessOrEqual(toDeliveryQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
        if (!onhand.any() || !"onhand".equals(onhand.getString("status"))) {
            String onhandStatus = onhand.any() ? record.l10n("库存状态为[%s]", onhand.getSelection("status")) : "";
            throw new ValidationException(record.l10n("物料标签[%s]状态为[%s]%s", label.get("sn"), label.getSelection("status"), onhandStatus));
        }
        String warehouseId = onhand.getRec("warehouse_id").getId();
        String[] warehouseIds = record.getRec("warehouse_id").getIds();
        if (!Arrays.asList(warehouseIds).contains(warehouseId)) {
            throw new ValidationException(record.l10n("标签库存数据对应的仓库,非出库单发货仓库,请检查数据"));
        }
        checkStockOutRules(record, onhand, label);
        double onhandQty = onhand.getDouble("usable_qty");
        Map<String, Object> result = new HashMap<>();
        if (Utils.lessOrEqual(onhandQty, toDeliveryQty) || canOverSend(record, onhand.getRec("warehouse_id"), material)) {
            createStockOutDetails(record, onhand.getDouble("ok_qty"), onhand.getRec("material_id").getId(),
                label.getId(), onhand.getRec("warehouse_id").getId(), onhand.getRec("location_id").getId(), label.getString("lot_num"));
            onhand.set("allot_qty", onhandQty);
            onhand.set("usable_qty", 0);
            onhand.set("status", "allot");
            label.set("status", "allot");
            line.set("scan_qty", scanQty + onhandQty);
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.other_stock_out");
            log.put("related_id", record.getId());
            log.put("related_code", record.get("code"));
            label.call("logStatus", log);
            toDeliveryQty = Math.max(0, Utils.round(toDeliveryQty - onhandQty));
            result.put("data", getStockOutMaterialByWarehouse(record, material, onhand.getRec("warehouse_id"), toDeliveryQty));
            result.put("message", record.l10n("条码[%s]数量[%s]出库成功", label.get("sn"), onhandQty));
        } else {
            Records unit = material.getRec("unit_id");
            Map<String, Object> data = new HashMap<>();
            data.put("accuracy", unit.getInteger("accuracy"));
            data.put("sn", label.get("sn"));
            data.put("qty", onhandQty);
            data.put("to_delivery_qty", toDeliveryQty);
            data.put("split_qty", toDeliveryQty);
            data.put("print_old", true);
            result.put("action", "split");
            result.put("data", getStockOutMaterialByOnhand(record, material, onhand.getRec("warehouse_id"), onhand, toDeliveryQty));
            result.put("split", data);
            result.put("message", record.l10n("条码[%s]需要拆分", label.get("sn")));
        }
        return result;
    }

    /**
     * 创建出库明细
     */
    public Records createStockOutDetails(Records record, Double qty, String materialId, String labelId, String warehouseId, String locationId, String lot_num) {
        Map<String, Object> data = new HashMap<>();
        data.put("other_stock_out_id", record.getId());
        data.put("qty", qty);
        data.put("label_id", labelId);
        data.put("lot_num", lot_num);
        data.put("material_id", materialId);
        data.put("warehouse_id", warehouseId);
        data.put("location_id", locationId);
        return record.getEnv().get("wms.other_stock_out_details").create(data);
    }

    /**
     * 是否允许超发，需同时满足以下条件
     * <pre>
     *     1.仓库允许超发
     *     2.ABC类配置允许超发
     * </pre>
     */
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

    /**
     * 校验出库规则，散件先出、先进先出、后进先出
     */
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

    /**
     * 校验后进先出
     */
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

    /**
     * 校验先进先出
     */
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

    public Map<String, Object> stockOutByLotAndNum(Records record, String lotNum, Records material, String code, Double labelQty) {
        Environment env = record.getEnv();
        Records line = env.get("wms.other_stock_out_line").find(Criteria.equal("other_stock_out_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(record.l10n("标签[%s]物料[%s]不在出库需求", code, material.get("present")));
        }
        double requestQty = line.getDouble("request_qty");
        double scanQty = line.getDouble("scan_qty");
        double toDeliveryQty = Utils.round(requestQty - scanQty);
        Records onhand = null;
        String[] warehouseIds = record.getRec("warehouse_id").getIds();
        String stockRule = material.getString("stock_rule");
        if ("lot".equals(stockRule)) {
            onhand = env.get("stock.onhand").find(Criteria.equal("lot_num", lotNum).and(Criteria.equal("material_id", material.getId())).and(Criteria.in("warehouse_id", warehouseIds)).and(Criteria.greater("usable_qty", 0)));
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
        Map<String, Object> dataMap = getStockOutMaterialByWarehouse(record, material, onhand.getRec("warehouse_id"), toDeliveryQty);
        dataMap.put("sn", code);
        dataMap.put("scan_qty", Utils.largeOrEqual(labelQty, toDeliveryQty) ? toDeliveryQty : labelQty);
        dataMap.put("location_id", mapList.get("location_id"));
        dataMap.put("lot_out_qty", env.getConfig().getBoolean("lot_out_qty"));
        Map<String, Object> result = new HashMap<>();
        result.put("message", record.l10n("批次条码[%s]扫码成功", code));
        result.put("data", dataMap);
        return result;
    }

    //根据包装条码或者物料编码查询物料
    public Records findMaterial(Records records, String code) {
        Environment env = records.getEnv();
        Records lpn = env.get("packing.package").find(Criteria.equal("code", code));
        if (lpn.any()) {
            return lpn.getRec("material_id");
        }
        return env.get("md.material").find(Criteria.equal("code", code));
    }

    @ServiceMethod(label = "扫码确定", doc = "批次/数量管控的物料进行出库", auth = "delivery")
    public Object stockOutMaterial(Records record, @Doc("标签") String code, @Doc("仓库") String warehouseId,
                                   @Doc("库位") String locationId, @Doc("物料") String materialId, @Doc("出库数量") Double qty,
                                   @Doc("是否打印") Boolean printFlag, @Doc("标签模板") String templateId) {
        if (Utils.lessOrEqual(qty, 0)) {
            throw new ValidationException(record.l10n("出库数量必须大于0"));
        }
        printFlag = Utils.toBoolean(printFlag, false);
        if (printFlag && Utils.isBlank(templateId)) {
            throw new ValidationException(record.l10n("打印标签,请选择标签模板"));
        }
        Environment env = record.getEnv();
        Records line = env.get("wms.other_stock_out_line").find(Criteria.equal("other_stock_out_id", record.getId()).and("material_id", "=", materialId));
        if (!line.any()) {
            throw new ValidationException("找不到出库物料");
        }
        Cursor cr = env.getCursor();
        //事务锁定出库行
        cr.execute("update wms_other_stock_out_line set id=id where id=%s", Collections.singletonList(line.getId()));
        Records material = env.get("md.material", materialId);
        double requestQty = line.getDouble("request_qty");
        double scanQty = line.getDouble("scan_qty");
        double toDeliveryQty = Utils.round(requestQty - scanQty);
        if (Utils.lessOrEqual(toDeliveryQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        // 标签码扫扫码的时候已经校验过了,
        Records onhand = null;
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        if ("lot".equals(material.get("stock_rule"))) {
            onhand = record.getEnv().get("stock.onhand").find(Criteria.equal("material_id", materialId).and(Criteria.equal("lot_num", codes[2]))
                .and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId)));
        } else {
            onhand = record.getEnv().get("stock.onhand").find(Criteria.equal("material_id", materialId)
                .and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId))
                .and(Criteria.equal("label_id", null)).and(Criteria.equal("lot_num", null)));
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
            throw new ValidationException(record.l10n("出库数量[%s]不能大于库存可用数量[%s]", qty, onhand.getDouble("usable_qty")));
        }
        if (Utils.large(qty, toDeliveryQty) && !canOverSend(record, warehouse, material)) {
            throw new ValidationException(record.l10n("出库数量[%s]不能大于待发数量[%s]", qty, toDeliveryQty));
        }
        line.set("scan_qty", scanQty + qty);
        String sql = "";
        if (Utils.isNotBlank(locationId)) {
            sql = " and location_id = %s ";
        } else {
            sql = " and location_id is %s  ";
        }
        if ("lot".equals(material.get("stock_rule"))) {
            // 相同批号.相同库位数据合并
            Records detail = record.getEnv().get("wms.other_stock_out_details").find(Criteria.equal("other_stock_out_id", record.getId())
                .and(Criteria.equal("lot_num", codes[2])).and(Criteria.equal("warehouse_id", warehouseId))
                .and(Criteria.equal("location_id", locationId)));
            String detailId = null;
            if (detail.any()) {
                // 存在相同的
                detail.ensureOne();
                detail.set("qty", Utils.round(detail.getDouble("qty") + qty));
                detailId = detail.getId();
            } else {
                // 不存在
                Records stockOutDetails = createStockOutDetails(record, qty, materialId, null, warehouseId, locationId, codes[2]);
                detailId = stockOutDetails.getId();
            }
            cr.execute("update stock_onhand set usable_qty=usable_qty-%s, allot_qty=allot_qty+%s where lot_num = %s and  material_id=%s and warehouse_id=%s " + sql,
                Arrays.asList(qty, qty, codes[2], materialId, warehouseId, locationId));
            boolean lotOutQtyFlag = env.getConfig().getBoolean("lot_out_qty");
            if (lotOutQtyFlag) {
                Map<String, Object> data = new HashMap<>();
                data.put("order_id", record.getId());
                data.put("sn", codes[0]);
                data.put("material_id", materialId);
                data.put("lot_num", codes[2]);
                data.put("detail_id", detailId);
                data.put("type", "wms.other_stock_out");
                env.get("lbl.lot_status").create(data);
            }
        } else {
            Records detail = record.getEnv().get("wms.other_stock_out_details").find(Criteria.equal("other_stock_out_id", record.getId())
                .and(Criteria.equal("material_id", materialId)).and(Criteria.equal("warehouse_id", warehouseId))
                .and(Criteria.equal("location_id", locationId)));
            if (detail.any()) {
                detail.ensureOne();
                detail.set("qty", Utils.round(detail.getDouble("qty") + qty));
            } else {
                createStockOutDetails(record, qty, materialId, null, warehouseId, locationId, null);
            }
            cr.execute("update stock_onhand set usable_qty=usable_qty-%s, allot_qty=allot_qty+%s where material_id=%s and warehouse_id=%s  and lot_num is null and label_id is null" + sql,
                Arrays.asList(qty, qty, materialId, warehouseId, locationId));
        }
        Map<String, Object> result = new HashMap<>();
        toDeliveryQty = Math.max(0, Utils.round(toDeliveryQty - qty));
        result.put("data", getStockOutMaterialByWarehouse(record, material, warehouse, toDeliveryQty));
        result.put("message", record.l10n("物料[%s]数量[%s]出库成功", material.get("code"), qty));
        result.put("printMap", printFlag ? getPrintMap(material, qty, templateId, code, codes) : Collections.emptyMap());
        return result;
    }

    public Map<String, Object> getPrintMap(Records material, Double qty, String templateId, String code, String[] codes) {
        String sn = null;
        String lotNum = null;
        if ("lot".equals(material.getString("stock_rule"))) {
            lotNum = codes[2];
            // 批次管控打印新标签,没必要生成新的sn,直接在后面加01吧,不然后面打印出来的时候,序列号是空的,
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

    @ServiceMethod(label = "拆分标签", doc = "拆分生成新标签")
    public Object splitLabel(Records record, @Doc("条码") String sn, @Doc("拆分数量") Double splitQty, @Doc("是否打印原标签") Boolean printOld) {
        printOld = Utils.toBoolean(printOld, false);
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

    @ServiceMethod(label = "出库", doc = "根据出库明细生成出库单")
    public Object stockOut(Records records) {
        for (Records record : records) {
            Records details = record.getEnv().get("wms.other_stock_out_details").find(Criteria.equal("other_stock_out_id", record.getId()).and("stock_out_id", "=", null));
            if (!details.any()) {
                throw new ValidationException("没有可出库物料");
            }
            Set<String> materialIds = new HashSet<>();
            for (Records detail : details) {
                materialIds.add(detail.getRec("material_id").getId());
            }
            Records lines = record.getEnv().get("wms.other_stock_out_line").find(
                Criteria.equal("other_stock_out_id", record.getId()).and("material_id", "in", materialIds).and("status", "=", "delivered"));
            lines.set("status", "done");
            String message = record.l10n("出库");
            Records stockOut = record.getEnv().get("stock.stock_out").find(Criteria.equal("related_id", record.getId()));
            if (!stockOut.any()) {
                //创建出库单，出库单与出库单一一对应
                Map<String, Object> data = new HashMap<>();
                data.put("type", "wms.other_stock_out");
                data.put("related_code", record.get("code"));
                data.put("related_model", record.getMeta().getName());
                data.put("related_id", record.getId());
                stockOut = stockOut.create(data);
                message += record.l10n("，出库单: %s", stockOut.get("code"));
            }
            record.getEnv().get("lbl.lot_status").find(Criteria.equal("order_id", record.getId()).and(Criteria.equal("type", "wms.other_stock_out"))).delete();
            details.set("stock_out_id", stockOut.getId());
            stockOut.call("stockOut");
            record.call("trackMessage", message);
        }
        updateLineStatus(records);
        return Action.reload(records.l10n("出库成功"));
    }

    public void updateLineStatus(Records records) {
        //执行sql前flush保存
        records.flush();
        Cursor cr = records.getEnv().getCursor();
        String sql = "select distinct status from wms_other_stock_out_line where other_stock_out_id=%s";
        for (Records record : records) {
            cr.execute(sql, Collections.singletonList(record.getId()));
            boolean done = cr.fetchAll().stream().map(r -> (String) r[0]).allMatch("done"::equals);
            if (done) {
                //如果全部为完成状态，则更新为已完成状态
                record.set("status", "done");
                break;
            }
        }
    }
}
