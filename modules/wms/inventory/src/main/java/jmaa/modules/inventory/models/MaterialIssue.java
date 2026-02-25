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

/**
 * @author eric
 */
@Model.Meta(inherit = {"mfg.material_issue", "mixin.order_status"})
public class MaterialIssue extends Model {
    static Field details_ids = Field.One2many("mfg.material_issue_details", "issue_id").label("发料明细");
    static Field status = Field.Selection().addSelection(new Options() {{
        put("approve", "已审核");
        put("reject", "驳回");
        put("close", "关闭");
        put("done", "完成");
        put("cancel", "取消");
    }});

    /**
     * 提交，校验发货仓库
     */
    @ServiceMethod(label = "提交")
    public Object commit(Records records, Map<String, Object> values, String comment) {
        callSuper(records, values, comment);
        String body = records.l10n("提交") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "commit");
        return Action.reload(records.l10n("操作成功"));
    }

    /**
     * 查找有库存的物料，指定仓库时，在指定的仓库找，没指定仓库时，在所有发料仓库找。
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
            Records warehouses = record.getRec("warehouse_ids");
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

    /**
     * 指定仓库时按库位顺序加载下一个待发物料。
     * 根据出库规则推荐物料标签。
     */
    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待发物料，根据出库规则推荐物料标签")
    public Map<String, Object> loadIssueMaterial(Records record,
                                                 @Doc("仓库") String warehouseId,
                                                 @Doc("偏移量") Integer offset) {
        Environment env = record.getEnv();
        Records lines = env.get("mfg.material_issue_line").find(Criteria.equal("issue_id", record.getId())
            .and("status", "not in", Utils.asList("done", "issued")));
        if (!lines.any()) {
            throw new ValidationException(record.l10n("没有待发物料"));
        }
        Records line = lines.browse();
        Records warehouse = env.get("md.warehouse");
        Records material = env.get("md.material");
        List<String> materialIds = lines.stream().map(l -> l.getRec("material_id").getId()).collect(Collectors.toList());
        //指定仓库时，按仓库找物料，如果没找到，在所有仓库中找有库存的物料
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
        double toIssueQty = Utils.round(line.getDouble("request_qty") - line.getDouble("issued_qty"));
        return getIssueMaterialByWarehouse(record, line, warehouse, toIssueQty);
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
     * 根据仓库获取推荐的发料信息
     */
    public Map<String, Object> getIssueMaterialByWarehouse(Records record, Records line, Records warehouse, double toIssueQty) {
        Environment env = record.getEnv();
        Records onhand = env.get("stock.onhand");
        if (warehouse.any() && Utils.large(toIssueQty, 0)) {
            onhand = findOnhandByRule(record, line.getRec("material_id"), warehouse);
        }
        return getIssueMaterialByOnhand(record, line, warehouse, onhand, toIssueQty);
    }

    /**
     * 扫描条码发料。
     * <pre>
     *     1.序列号管控物料，扫描物料标签发料.
     *     2.数量管制物料，扫描物料标签、LPN、物料编码查询物料信息，输入发料数量调用{@link MaterialIssue#issueMaterial}发料.
     * </pre>
     */
    @ServiceMethod(label = "发料", doc = "物料编码则查询发料需求，物料标签则发料")
    public Object issue(Records record, @Doc("标签条码/物料编码") String code) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                return issueByLabel(record, label);
            }
        } else {
            Records label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
            if (label.any()) {
                material = label.getRec("material_id");
                if ("sn".equals(material.get("stock_rule"))) {
                    return issueByLabel(record, label);
                }
            }
            if (!material.any()) {
                material = findMaterial(record, code);
            }
            if (!material.any()) {
                throw new ValidationException(record.l10n("条码[%s]无法识别", code));
            }
            if ("sn".equals(material.get("stock_rule"))) {
                throw new ValidationException(record.l10n("物料[%s]是序列号管控，请使用物料标签发料", material.get("code")));
            }
        }
        return issueByLotOrNum(record, material, code, codes);
    }

    /**
     * 批次或数量发料
     */
    public Map<String, Object> issueByLotOrNum(Records record, Records material, String code, String[] codes) {
        Environment env = record.getEnv();
        Records line = env.get("mfg.material_issue_line").find(Criteria.equal("issue_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(record.l10n("标签[%s]物料[%s]不在发料需求", code, material.get("present")));
        }
        double requestQty = line.getDouble("request_qty");
        double issuedQty = line.getDouble("issued_qty");
        double toIssueQty = Utils.round(requestQty - issuedQty);
        if (Utils.lessOrEqual(toIssueQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需要，不能超发", material.get("present")));
        }
        String[] warehouseIds = record.getRec("warehouse_ids").getIds();
        String stockRule = material.getString("stock_rule");
        Criteria criteria = Criteria.equal("material_id", material.getId())
            .and(Criteria.in("warehouse_id", warehouseIds))
            .and(Criteria.greater("usable_qty", 0))
            .and(Criteria.equal("label_id", null));
        if ("lot".equals(stockRule) && codes.length > 2) {
            criteria.and("lot_num", "=", codes[2]);
        }
        Records onhand = env.get("stock.onhand").find(criteria);
        if (!onhand.any()) {
            throw new ValidationException(record.l10n("无可用库存数据,请检查标签数据"));
        }
        double labelQty = codes.length > 1 ? Utils.toDouble(codes[codes.length - 1])
            : Utils.round(onhand.stream().mapToDouble(r -> r.getDouble("usable_qty")).sum());
        onhand = onhand.first();
        Records unit = material.getRec("unit_id");
        Map<String, Object> confirm = new HashMap<>();
        confirm.put("code", code);
        confirm.put("material_id", material.getPresent());
        confirm.put("material_name_spec", material.get("name_spec"));
        confirm.put("label_qty", labelQty);
        confirm.put("issue_qty", Utils.large(toIssueQty, labelQty) ? labelQty : toIssueQty);
        confirm.put("warehouse_id", onhand.getRec("warehouse_id").getPresent());
        confirm.put("location_id", onhand.getRec("location_id").getPresent());
        confirm.put("location", onhand.getRec("location_id").getString("code"));
        confirm.put("unit_accuracy", unit.getInteger("accuracy"));
        confirm.put("to_issue_qty", toIssueQty);
        confirm.put("template_id", material.getRec("print_tpl_id").getPresent());
        if ("lot".equals(stockRule)) {
            confirm.put("lock_qty", env.getConfig().getBoolean("lot_out_qty"));
        }
        confirm.put("print_label", Utils.large(labelQty, toIssueQty));
        // 直接弹框, 弹框确认的时候才最终确认,执行的是issueMaterial方法
        Map<String, Object> result = new HashMap<>();
        result.put("data", getIssueMaterialByWarehouse(record, line, onhand.getRec("warehouse_id"), toIssueQty));
        result.put("confirm", confirm);
        result.put("message", record.l10n("条码[%s]识别成功，待确认", code));
        return result;
    }

    //根据包装条码或者物料编码查询物料
    public Records findMaterial(Records record, String code) {
        Environment env = record.getEnv();
        Records lpn = env.get("packing.package").find(Criteria.equal("code", code));
        if (lpn.any()) {
            return lpn.getRec("material_id");
        }
        return env.get("md.material").find(Criteria.equal("code", code));
    }

    /**
     * 按物料标签发料
     */
    public Map<String, Object> issueByLabel(Records record, Records label) {
        Environment env = record.getEnv();
        Records material = label.getRec("material_id");
        Records line = env.get("mfg.material_issue_line").find(Criteria.equal("issue_id", record.getId()).and("material_id", "=", material.getId()));
        if (!line.any()) {
            throw new ValidationException(record.l10n("标签[%s]物料[%s]不在发料需求", label.get("sn"), material.get("present")));
        }
        Cursor cr = env.getCursor();
        //事务锁定发料行
        cr.execute("update mfg_material_issue_line set id=id where id=%s", Arrays.asList(line.getId()));
        double requestQty = line.getDouble("request_qty");
        double issuedQty = line.getDouble("issued_qty");
        double toIssueQty = Utils.round(requestQty - issuedQty);
        if (Utils.lessOrEqual(toIssueQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
        if (!onhand.any() || !"onhand".equals(onhand.getString("status"))) {
            String onhandStatus = onhand.any() ? record.l10n("库存状态为[%s]", onhand.getSelection("status")) : "";
            throw new ValidationException(record.l10n("物料标签[%s]状态为[%s]%s", label.get("sn"), label.getSelection("status"), onhandStatus));
        }
        String warehouseId = onhand.getRec("warehouse_id").getId();
        String[] warehouseIds = record.getRec("warehouse_ids").getIds();
        if (!Arrays.asList(warehouseIds).contains(warehouseId)) {
            throw new ValidationException(record.l10n("标签库存数据对应的仓库,非发料单发货仓库,请检查数据"));
        }
        checkStockOutRules(record, onhand, label);
        double onhandQty = onhand.getDouble("usable_qty");
        Map<String, Object> result = new HashMap<>();
        if (Utils.lessOrEqual(onhandQty, toIssueQty) || canOverSend(record, onhand.getRec("warehouse_id"), material)) {
            createIssueDetails(record, onhand.getDouble("ok_qty"), onhand.getRec("material_id").getId(),
                label.getId(), onhand.getRec("warehouse_id").getId(), onhand.getRec("location_id").getId(), label.getString("lot_num"));
            onhand.set("allot_qty", onhandQty);
            onhand.set("usable_qty", 0);
            onhand.set("status", "allot");
            label.set("status", "allot");
            line.set("issued_qty", issuedQty + onhandQty);
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "mfg.material_issue");
            log.put("related_id", record.getId());
            log.put("related_code", record.get("code"));
            label.call("logStatus", log);
            toIssueQty = Math.max(0, Utils.round(toIssueQty - onhandQty));
            result.put("data", getIssueMaterialByWarehouse(record, line, onhand.getRec("warehouse_id"), toIssueQty));
            result.put("message", record.l10n("条码[%s]数量[%s]发料成功", label.get("sn"), onhandQty));
        } else {
            Records unit = material.getRec("unit_id");
            Map<String, Object> split = new HashMap<>();
            split.put("unit_accuracy", unit.getInteger("accuracy"));
            split.put("material_id", material.getPresent());
            split.put("material_name_spec", material.get("name_spec"));
            split.put("warehouse_id", onhand.getRec("warehouse_id").getPresent());
            split.put("location_id", onhand.getRec("location_id").getPresent());
            split.put("location", onhand.getRec("location_id").getString("code"));
            split.put("sn", label.get("sn"));
            split.put("qty", onhandQty);
            split.put("to_issue_qty", toIssueQty);
            split.put("split_qty", toIssueQty);
            split.put("print_old", true);
            result.put("data", getIssueMaterialByOnhand(record, line, onhand.getRec("warehouse_id"), onhand, toIssueQty));
            result.put("split", split);
            result.put("message", record.l10n("条码[%s]数量超过待发数量，需要拆分", label.get("sn")));
        }
        return result;
    }

    /**
     * 根据onhand获取发料信息
     */
    public Map<String, Object> getIssueMaterialByOnhand(Records record, Records line, Records warehouse, Records onhand, double toIssueQty) {
        Map<String, Object> result = new HashMap<>();
        Records material = line.getRec("material_id");
        String stockRule = material.getString("stock_rule");
        Records unit = material.getRec("unit_id");
        result.put("id", record.getId());
        result.put("status", record.get("status"));
        result.put("suggest_location", onhand.getRec("location_id").get("code"));
        result.put("material_id", material.getPresent());
        result.put("material_name_spec", material.get("name_spec"));
        result.put("request_qty", line.getDouble("request_qty"));
        result.put("issued_qty", line.getDouble("issued_qty"));
        result.put("to_issue_qty", toIssueQty);
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
     * 创建发料明细
     */
    public void createIssueDetails(Records record, Double qty, String materialId, String labelId, String warehouseId, String locationId, String lot_num) {
        Map<String, Object> data = new HashMap<>();
        data.put("issue_id", record.getId());
        data.put("qty", qty);
        data.put("label_id", labelId);
        data.put("lot_num", lot_num);
        data.put("material_id", materialId);
        data.put("warehouse_id", warehouseId);
        data.put("location_id", locationId);
        record.getEnv().get("mfg.material_issue_details").create(data);
    }

    @ServiceMethod(label = "拆分标签发料", doc = "拆分生成新标签")
    public Object splitLabel(Records record,
                             @Doc("条码") String sn,
                             @Doc("拆分数量") double splitQty,
                             @Doc("是否打印原标签") boolean printOld) {
        Records label = (Records) record.getEnv().get("lbl.material_label").call("splitLabel", sn, splitQty);
        Map<String, Object> result = issueByLabel(record, label);
        Records printTemplate = label.getRec("print_template_id");
        if (printOld) {
            Records oldLabel = label.find(Criteria.equal("sn", sn));
            label.union(oldLabel);
        }
        Map<String, Object> printData = (Map<String, Object>) printTemplate.call("print", new KvMap().set("labels", label));
        result.put("printData", printData);
        return result;
    }

    @ServiceMethod(label = "发料", doc = "数量管控的物料进行发料", auth = "issue")
    public Object issueMaterial(Records record,
                                @Doc("标签") String code,
                                @Doc("仓库") String warehouseId,
                                @Doc("库位") String locationId,
                                @Doc("物料") String materialId,
                                @Doc("发料数量") double qty,
                                @Doc("是否打印") boolean printLabel,
                                @Doc("标签模板") String templateId) {
        if (Utils.lessOrEqual(qty, 0)) {
            throw new ValidationException(record.l10n("发料数量必须大于0"));
        }
        if (printLabel && Utils.isBlank(templateId)) {
            throw new ValidationException(record.l10n("打印标签,请选择标签模板"));
        }
        Environment env = record.getEnv();
        Records line = env.get("mfg.material_issue_line").find(Criteria.equal("issue_id", record.getId()).and("material_id", "=", materialId));
        if (!line.any()) {
            throw new ValidationException("找不到发料物料");
        }
        Cursor cr = env.getCursor();
        //事务锁定发料行
        cr.execute("update mfg_material_issue_line set id=id where id=%s", Arrays.asList(line.getId()));
        Records material = env.get("md.material", materialId);
        double requestQty = line.getDouble("request_qty");
        double issuedQty = line.getDouble("issued_qty");
        double toIssueQty = Utils.round(requestQty - issuedQty);
        if (Utils.lessOrEqual(toIssueQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        // 标签码扫扫码的时候已经校验过了,
        Records onhand = record.getEnv().get("stock.onhand");
        String lotNum = null;
        if ("lot".equals(material.get("stock_rule"))) {
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
            lotNum = codes[2];
        }
        onhand = onhand.find(Criteria.equal("material_id", materialId)
            .and("warehouse_id", "=", warehouseId).and("location_id", "=", locationId)
            .and("label_id", "=", null).and("lot_num", "=", lotNum));
        if (!onhand.any()) {
            throw new ValidationException(Utils.isEmpty(locationId) ? record.l10n("仓库[%s]无不在库位的物料[%s]库存", env.get("md.warehouse", warehouseId).get("present"), material.get("code"))
                : record.l10n("仓库[%s]库位[%s]没有物料[%s]库存", env.get("md.warehouse", warehouseId).get("present"), env.get("md.store_location", locationId).get("present"), material.get("code")));
        }
        if (!"onhand".equals(onhand.getString("status"))) {
            throw new ValidationException(record.l10n("物料[%s]库存状态为[%s]，不能发料", material.get("code"), onhand.getSelection("status")));
        }
        Records warehouse = onhand.getRec("warehouse_id");
        if (warehouse.getBoolean("frozen")) {
            throw new ValidationException(record.l10n("仓库[%s]已冻结，不能发料", warehouse.get("present")));
        }
        if (Utils.large(qty, onhand.getDouble("usable_qty"))) {
            throw new ValidationException(record.l10n("发料数量[%s]不能大于库存可用数量[%s]", qty, onhand.getDouble("usable_qty")));
        }
        if (Utils.large(qty, toIssueQty) && !canOverSend(record, warehouse, material)) {
            throw new ValidationException(record.l10n("发料数量[%s]不能大于待发数量[%s]", qty, toIssueQty));
        }
        createIssueDetails(record, qty, materialId, null, warehouseId, locationId, lotNum);
        line.set("issued_qty", issuedQty + qty);
        String sql = Utils.isNotBlank(locationId) ? " and location_id = %s " : " and location_id is %s  ";
        if (Utils.isNotEmpty(lotNum)) {
            cr.execute("update stock_onhand set usable_qty=usable_qty-%s, allot_qty=allot_qty+%s where lot_num = %s and  material_id=%s and warehouse_id=%s " + sql,
                Arrays.asList(qty, qty, lotNum, materialId, warehouseId, locationId));
        } else {
            cr.execute("update stock_onhand set usable_qty=usable_qty-%s, allot_qty=allot_qty+%s where material_id=%s and warehouse_id=%s  and lot_num is null and label_id is null" + sql,
                Arrays.asList(qty, qty, materialId, warehouseId, locationId));
        }
        Map<String, Object> result = new HashMap<>();
        toIssueQty = Math.max(0, Utils.round(toIssueQty - qty));
        result.put("data", getIssueMaterialByWarehouse(record, line, warehouse, toIssueQty));
        result.put("message", record.l10n("物料[%s]数量[%s]发料成功", material.get("code"), qty));
        if (printLabel) {
            result.put("printData", getPrintData(record, material, qty, templateId, code, lotNum));
        }
        return result;
    }

    public Map<String, Object> getPrintData(Records records, Records material, Double qty, String templateId, String code, String lotNum) {
        String sn = ((List<String>) material.getEnv().get("lbl.material_label").call("createCodes", material.getId(), 1)).get(0);
        KvMap labelData = new KvMap()
            .set("code", code)
            .set("sn", sn)
            .set("lot_num", lotNum)
            .set("qty", qty)
            .set("material_code", material.get("code"))
            .set("material_name", material.get("name"))
            .set("material_spec", material.get("spec"))
            .set("unit", material.getRec("unit_id").get("name"))
            .set("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (Utils.isNotEmpty(lotNum)) {
            Records lot = records.getEnv().get("lbl.lot_num").find(Criteria.equal("code", lotNum).and(Criteria.equal("material_id",material.getId())));
            if (lot.any()) {
                Records supplier = lot.getRec("supplier_id");
                labelData.set("product_date", Utils.format(lot.getDate("product_date"), "yyyy-MM-dd"))
                    .set("supplier_name", Utils.replaceAll(supplier.getString("name"), ",", "，"))
                    .set("supplier_chars", supplier.get("chars"))
                    .set("supplier_code", supplier.get("code"));
            }
        }
        Records printTemplate = material.getEnv().get("print.template", templateId);
        return (Map<String, Object>) printTemplate.call("print", new KvMap().set("data", Utils.asList(labelData)));
    }

    @ServiceMethod(label = "出库", doc = "根据发料明细生成出库单")
    public Object stockOut(Records records) {
        for (Records record : records) {
            Records details = record.getEnv().get("mfg.material_issue_details").find(Criteria.equal("issue_id", record.getId()).and("stock_out_id", "=", null));
            if (!details.any()) {
                throw new ValidationException("没有可出库物料");
            }
            Set<String> materialIds = new HashSet<>();
            for (Records detail : details) {
                materialIds.add(detail.getRec("material_id").getId());
            }
            Records lines = record.getEnv().get("mfg.material_issue_line").find(
                Criteria.equal("issue_id", record.getId()).and("material_id", "in", materialIds).and("status", "=", "issued"));
            lines.set("status", "done");
            String message = record.l10n("出库");
            Records stockOut = record.getEnv().get("stock.stock_out");
            // 点一次出库,就新增一条出库单,
            Map<String, Object> data = new HashMap<>();
            data.put("type", "issue");
            data.put("related_code", record.get("code"));
            data.put("related_model", record.getMeta().getName());
            data.put("related_id", record.getId());
            stockOut = stockOut.create(data);
            details.set("stock_out_id", stockOut.getId());
            stockOut.call("stockOut");
            record.call("trackMessage", message);
        }
        updateMaterialIssueStatus(records);
        return Action.reload(records.l10n("出库成功"));
    }

    /**
     * 更新发料单状态，所有发料行完成，则发料单完成
     */
    public void updateMaterialIssueStatus(Records records) {
        //执行sql前flush保存
        records.flush();
        Cursor cr = records.getEnv().getCursor();
        String sql = "select distinct status from mfg_material_issue_line where issue_id=%s";
        for (Records record : records) {
            cr.execute(sql, Utils.asList(record.getId()));
            boolean done = cr.fetchAll().stream().map(r -> (String) r[0]).allMatch("done"::equals);
            if (done) {
                //如果全部为完成状态，则更新为已完成状态
                record.set("status", "done");
            }
        }
    }
}
