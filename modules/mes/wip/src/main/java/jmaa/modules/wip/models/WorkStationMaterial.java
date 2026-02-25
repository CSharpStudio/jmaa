package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ServerDate;

import java.util.*;

@Model.Service(remove = "@edit")
@Model.Meta(name = "mfg.work_station_material", label = "工位库存", order = "id", inherit = {"mixin.material", "mixin.company"})
public class WorkStationMaterial extends Model {
    static Field lot_num = Field.Char().label("批次号");
    static Field workshop_id = Field.Many2one("md.workshop").related("station_id.resource_id.workshop_id");
    static Field resource_id = Field.Many2one("md.work_resource").related("station_id.resource_id");
    static Field process_id = Field.Many2one("md.work_process").related("station_id.process_id");
    static Field station_id = Field.Many2one("md.work_station").label("工位").required();
    static Field qty = Field.Float().label("数量");
    static Field unit_accuracy = Field.Integer().related("unit_id.accuracy").label("单位精度");

    /**
     * 工位上料，规范操作严格根据标签数量上料，不能严格规范操作时，可配置[允许修改上料数量]参数
     */
    @ServiceMethod(label = "上料", auth = "feeding")
    public Object feeding(Records records, String code, double qty, String stationId, String workOrderId) {
        Environment env = records.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        Map<String, Object> result = new HashMap<>();
        boolean qtyModifiable = env.getConfig().getBoolean("feed_qty_modifiable");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                if (!label.any()) {
                    throw new ValidationException(records.l10n("条码[%s]无效", code));
                }
                if (!qtyModifiable) {
                    qty = label.getDouble("qty");
                }
                if (Utils.large(qty, 0)) {
                    feedingWithLabel(records, label, qty, workOrderId, stationId);
                    result.put("message", records.l10n("物料[%s]上料成功，数量[%s]", material.get("code"), qty));
                } else {
                    result.put("material_id", material.getPresent());
                    result.put("qty", label.getDouble("qty"));
                    result.put("lot_num", label.get("lot_num"));
                }
            } else {
                String lotNum = "lot".equals(stockRule) ? codes[2] : null;
                if (!qtyModifiable) {
                    qty = Utils.toDouble(codes[codes.length - 1]);
                }
                if (Utils.large(qty, 0)) {
                    feedingMaterial(records, workOrderId, stationId, material, qty, lotNum, null, codes[0]);
                    result.put("message", records.l10n("物料[%s]上料成功，数量[%s]", material.get("code"), qty));
                } else {
                    result.put("material_id", material.getPresent());
                    result.put("qty", Utils.toDouble(codes[codes.length - 1]));
                    result.put("lot_num", lotNum);
                }
            }
            return result;
        }
        Records label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
        if (label.any()) {
            if (!qtyModifiable) {
                qty = label.getDouble("qty");
            }
            if (Utils.large(qty, 0)) {
                feedingWithLabel(records, label, qty, workOrderId, stationId);
                result.put("message", records.l10n("物料[%s]上料成功，数量[%s]", label.getRec("material_id").get("code"), qty));
            } else {
                result.put("material_id", label.getRec("material_id").getPresent());
                result.put("qty", label.getDouble("qty"));
                result.put("lot_num", label.get("lot_num"));
            }
            return result;
        }
        throw new ValidationException(records.l10n("条码[%s]无法识别", code));
    }

    public void feedingMaterial(Records records, String workOrderId, String stationId, Records material, double qty, String lotNum, String labelId, String sn) {
        Environment env = records.getEnv();
        Records process = env.get("md.work_station", stationId).getRec("process_id");
        Records processBom = env.get("mfg.work_order_process_bom").find(Criteria.equal("work_order_id", workOrderId)
            .and("process_id", "=", process.getId())
            .and("material_id", "=", material.getId()));
        if (!processBom.any()) {
            throw new ValidationException(records.l10n("物料[%s]不在工单[%s]工序[%s]BOM", material.get("present"),
                env.get("mfg.work_order", workOrderId).get("present"), process.get("present")));
        }
        Records stationMaterial = env.get("mfg.work_station_material").find(Criteria.equal("station_id", stationId)
            .and("material_id", "=", material.getId())
            .and("lot_num", "=", lotNum));
        if (stationMaterial.any()) {
            env.getCursor().execute("update mfg_work_station_material set qty=qty+%s where id=%s",
                Utils.asList(qty, stationMaterial.getId()));
        } else {
            stationMaterial.create(new KvMap()
                .set("material_id", material.getId())
                .set("station_id", stationId)
                .set("qty", qty)
                .set("lot_num", lotNum));
        }
        env.get("mfg.feeding_log").create(new KvMap()
            .set("station_id", stationId)
            .set("material_id", material.getId())
            .set("label_id", labelId)
            .set("lot_num", lotNum)
            .set("qty", qty)
            .set("sn", sn)
            .set("create_uid", env.getUserId())
            .set("work_order_id", workOrderId)
            .set("create_date", new ServerDate()));
    }

    public void feedingWithLabel(Records records, Records label, double qty, String workOrderId, String stationId) {
        String status = label.getString("status");
        if ("feed".equals(status)) {
            throw new ValidationException(records.l10n("条码[%s]已上料，不能重复上料", label.get("sn")));
        }
        if (!"new".equals(status) && !"stock-out".equals(status) && !"to-return".equals(status) && !"unload".equals(status)) {
            throw new ValidationException(records.l10n("物料标签[%s]状态为[%s]", label.get("sn"), label.getSelection("status")));
        }
        label.set("status", "feed");
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "mfg.work_station_material");
        Records station = records.getEnv().get("md.work_station", stationId);
        String location = station.getRec("workshop_id").get("present")
            + "/" + station.getRec("resource_id").get("present")
            + "/" + station.get("present");
        log.put("location", location);
        label.call("logStatus", log);
        feedingMaterial(records, workOrderId, stationId, label.getRec("material_id"), qty, label.getString("lot_num"), label.getId(), null);
    }

    @ServiceMethod(label = "查找工位库存", auth = "read")
    public Map<String, Object> findStationMaterial(Records records, String stationId, String code, List<String> fields) {
        Environment env = records.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        String lotNum;
        Records label = env.get("lbl.material_label");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = label.find(Criteria.equal("sn", codes[0]));
                lotNum = label.getString("lot_num");
            } else {
                lotNum = "lot".equals(stockRule) ? codes[2] : null;
            }
        } else {
            label = label.find(Criteria.equal("sn", code));
            if (!label.any()) {
                throw new ValidationException(records.l10n("条码[%s]无效", code));
            }
            material = label.getRec("material_id");
            lotNum = label.getString("lot_num");
        }
        if (label.any()) {
            String labelStatus = label.getString("status");
            if ("onhand".equals(labelStatus) || "unload".equals(labelStatus)) {
                throw new ValidationException(records.l10n("标签条码[%s]状态为[%s]，不可下料", label.get("sn"), label.getSelection("status")));
            }
        }
        Records workStationMaterial = records.find(Criteria.equal("station_id", stationId)
            .and(Criteria.equal("material_id", material.getId())).and(Criteria.equal("lot_num", lotNum)));
        if (!workStationMaterial.any()) {
            Records station = env.get("md.work_station", stationId);
            String msg = records.l10n("工位[%s]不存在物料[%s]", station.get("present"), material.get("code"));
            if (Utils.isNotBlank(lotNum)) {
                msg += records.l10n("批次[%s]", lotNum);
            }
            throw new ValidationException(msg);
        }
        return workStationMaterial.read(fields).get(0);
    }

    @ServiceMethod(label = "下料")
    public Object unload(Records record,
                         @Doc("条码") String code,
                         @Doc("下料数量") double qty,
                         @Doc("是否打印") boolean print) {
        if (Utils.lessOrEqual(qty, 0D)) {
            throw new ValidationException(record.l10n("下料数量必须大于0"));
        }
        double stationQty = record.getDouble("qty");
        if (Utils.large(qty, stationQty)) {
            throw new ValidationException(record.l10n("下料数量[%s]超过工位数量[%s]", qty, stationQty));
        }
        Environment env = record.getEnv();
        Records material = record.getRec("material_id");
        String lotNum = record.getString("lot_num");
        Records label = env.get("lbl.material_label");
        String stockRule = material.getString("stock_rule");
        if ("sn".equals(stockRule)) {
            if (Utils.isNotBlank(code)) {
                String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
                label = label.find(Criteria.equal("sn", codes[0]));
                if (!label.any()) {
                    throw new ValidationException(record.l10n("条码[%s]无效", code));
                }
                if (!Utils.equals(label.getRec("material_id"), material)) {
                    throw new ValidationException(record.l10n("条码[%s]物料[%s]与工位物料[%s]不一致", code,
                        label.getRec("material_id").get("code"), material.get("code")));
                }
                String labelStatus = label.getString("status");
                if ("onhand".equals(labelStatus) || "unload".equals(labelStatus)) {
                    throw new ValidationException(record.l10n("标签条码[%s]状态为[%s]，不可下料", label.get("sn"), label.getSelection("status")));
                }
                if (!Utils.equals(lotNum, label.getString("lot_num"))) {
                    throw new ValidationException(record.l10n("标签条码[%s]批次[%s]与工位物料批次[%s]不一致", code,
                        label.getString("lot_num"), lotNum));
                }
                label.set("qty", qty);
                label.set("status", "unload");
                logLabelStatus(record, label);
            } else {
                Records lot = env.get("lbl.lot_num").find(Criteria.equal("code", lotNum));
                Records printTemplate = material.getRec("print_tpl_id");
                Map<String, Object> labelData = new KvMap()
                    .set("supplier_id", lot.getRec("supplier_id").getId())
                    .set("print_template_id", printTemplate.getId());
                label = (Records) label.call("createLabel", material.getId(), qty, qty, lot.get("product_date"), "", lotNum, labelData);
                logLabelStatus(record, label);
            }
        }
        Records station = record.getRec("station_id");
        Cursor cr = env.getCursor();
        cr.execute("update mfg_work_station_material set qty=qty-%s where id=%s", Arrays.asList(qty, record.getId()));
        cr.execute("delete from mfg_work_station_material where id=%s and qty<=0", Arrays.asList(record.getId()));
        // 记录下料
        String sn = null;
        if (Utils.isNotEmpty(code)) {
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
            sn = codes[0];
        }
        env.get("mfg.feeding_log").create(new KvMap()
            .set("station_id", station.getId())
            .set("material_id", material.getId())
            .set("label_id", label.getId())
            .set("sn", sn)
            .set("lot_num", lotNum)
            .set("qty", -qty)
            .set("create_uid", env.getUserId())
            .set("create_date", new ServerDate()));
        Map<String, Object> result = new HashMap<>();
        if (Utils.isNotEmpty(lotNum)) {
            result.put("message", record.l10n("物料[%s]批次[%s]下料数量[%s]", material.get("code"), lotNum, qty));
        } else {
            result.put("message", record.l10n("物料[%s]下料数量[%s]", material.get("code"), qty));
        }
        result.put("qty", Utils.round(stationQty - qty));
        if (print) {
            Object printData = label.any() ? label.call("reprintLabel") :
                printNoLabel(record, lotNum, qty);
            result.put("printData", printData);
        }
        return result;
    }

    /**
     * 打印非序列号管控的标签
     */
    public Object printNoLabel(Records records, String lotNum, double qty) {
        Records material = records.getRec("material_id");
        Records template = material.getRec("print_tpl_id");
        if (!template.any()) {
            throw new ValidationException(records.l10n("物料[%s]没有默认打印模板", material.get("code")));
        }
        Records supplier;
        String productDate = "";
        String code;
        String sn;
        if ("lot".equals(material.getString("stock_rule"))) {
            sn = lotNum + "-1";
            code = sn + "|" + material.get("code") + "|" + lotNum + "|" + qty;
            Records lot = records.getEnv().get("lbl.lot_num").find(Criteria.equal("code", lotNum));
            supplier = lot.getRec("supplier_id");
            productDate = Utils.format(lot.getDate("product_date"), "yyyy-MM-dd");
        } else {
            sn = Utils.format(new Date(), "yyMMddHHmmss");
            code = sn + "|" + material.get("code") + "|" + qty;
            supplier = records.getEnv().get("md.supplier");
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
            .set("product_date", productDate)
            .set("product_lot", "")
            .set("supplier_name", supplier.get("name"))
            .set("supplier_chars", supplier.get("chars"))
            .set("supplier_code", supplier.get("code"))
            .set("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));

        List<Map<String, Object>> list = new ArrayList<>(1);
        list.add(labelData);
        return template.call("print", new KvMap().set("data", list));
    }

    /**
     * 标签状态记录
     */
    public void logLabelStatus(Records record, Records label) {
        // 添加状态记录
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "mfg.work_station_material:unload");
        Records station = record.getRec("station_id");
        String location = station.getRec("workshop_id").get("present")
            + "/" + station.getRec("resource_id").get("present")
            + "/" + station.get("present");
        log.put("location", location);
        label.call("logStatus", log);
    }

    String[] findMaterialId(Records records, String code) {
        String[] codes = (String[]) records.getEnv().get("lbl.code_parse").call("parse", code);
        Records material = records.getEnv().get("md.material");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            return material.getIds();
        }
        material = material.find(Criteria.like("code", code).or(Criteria.like("name_spec", code)));
        if (material.any()) {
            return material.getIds();
        }
        Records label = records.getEnv().get("lbl.material_label").find(Criteria.equal("sn", code));
        if (label.any()) {
            return label.getRec("material_id").getIds();
        }
        return new String[0];
    }

    @ServiceMethod(label = "查询工位库存", auth = "read")
    public Object searchStock(Records records, String stationId, String code, List<String> fields, Integer offset, Integer limit) {
        Criteria criteria = Criteria.equal("station_id", stationId);
        if (Utils.isNotEmpty(code)) {
            criteria.and("material_id", "in", findMaterialId(records, code));
        }
        return records.searchLimit(fields, criteria, offset, limit, null);
    }
}
