package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.services.CreateService;
import org.jmaa.sdk.util.KvMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "mfg.material_return_mobile", label = "移动端退料", inherit = {"mfg.material_return", "mfg.material_return_dialog"}, table = "mfg_material_return")
@Model.Service(remove = "@edit")
@Model.Service(name = "create", label = "创建", auth = "read", description = "为模型创建新记录", type = CreateService.class)
public class MaterialReturnMobile extends ValueModel {
    static Field material_id = Field.Many2one("md.material").label("物料编码");
    static Field sn = Field.Char().label("条码").store(false);

    public List<Object> getCodeRule(Records rec, Map<String, Object> data) {
        return Arrays.asList("code", "mfg.material_return");
    }

    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        return rec.getEnv().get("mfg.material_return",rec.getId()).createBatch(valuesList);
    }

    @ServiceMethod(label = "退料", doc = "扫码后确认按钮", auth = "read")
    public Object returnMaterial(Records record, @Doc("标签") String code, @Doc("退料数量") Double returnedQty,
                                 @Doc("是否打印原标签") Boolean printFlag, @Doc("标签模板") String templateId, @Doc("物料") String materialId, @Doc("仓库") String warehouseId) {
        return record.getEnv().get("mfg.material_return", record.getId()).call("returnMaterial", code, returnedQty, printFlag, templateId, materialId, warehouseId);
    }

    @ServiceMethod(label = "扫描标签", doc = "序列号直接退,其他显示明细", auth = "read")
    public Object scanCode(Records record, String code) {
        return record.getEnv().get("mfg.material_return", record.getId()).call("scanCode", code);
    }

    @ServiceMethod(label = "提交", doc = "提交单据，状态改为已提交", auth = "read")
    public Object commit(Records record, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        return record.getEnv().get("mfg.material_return", record.getId()).call("commit", values, comment);
    }

    @ActionMethod
    public Object onSnChange(Records record) {
        AttrAction action = new AttrAction();
        String code = record.getString("sn");
        Records workOrder = record.getRec("work_order_id");
        if (!workOrder.any()) {
            throw new ValidationException(record.l10n("工单不能为空"));
        }
        Environment env = record.getEnv();
        if (Utils.isNotEmpty(code)) {
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
            if (codes.length > 1) {
                Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
                String stockRule = material.getString("stock_rule");
                if ("sn".equals(stockRule)) {
                    Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                    Records bom = record.getEnv().get("mfg.work_order_bom").find(Criteria.equal("work_order_id", workOrder.getId())
                        .and("material_id", "=", material.getId()));
                    if (!bom.any()) {
                        throw new ValidationException(record.l10n("标签[%s]物料[%s]不在工单[%s]BOM中", label.get("sn"), material.get("code"), workOrder.get("code")));
                    }
                    String status = label.getString("status");
                    if (!"stock-out".equals(status) && !"to-return".equals(status) && !"new".equals(status)) {
                        throw new ValidationException(record.l10n("标签[%s]状态[%s]，不能退料", label.get("sn"), label.getSelection("status")));
                    }
                    action.setValue("qty", label.get("qty"));
                } else {
                    action.setValue("qty", Utils.toDouble(codes[codes.length - 1]));
                }
                action.setValue("material_id", material);
            }
        } else {
            action.setValue("material_id", null);
            action.setValue("qty", null);
        }
        return action;
    }

    @ServiceMethod(label = "查询物料", auth = "read", doc = "查找工单BOM中的物料")
    public Object findMaterial(Records records, List<String> fields, Criteria criteria, String workOrderId, Integer offset, Integer limit) {
        Cursor cr = records.getEnv().getCursor();
        cr.execute("select material_id from mfg_work_order_bom where work_order_id=%s", Utils.asList(workOrderId));
        List<Object[]> rows = cr.fetchAll();
        List<String> ids = rows.stream().map(r -> (String) r[0]).collect(Collectors.toList());
        criteria.and(Criteria.in("id", ids));
        return records.getEnv().get("md.material").searchLimit(fields, criteria, offset, limit, "code");
    }

    @ServiceMethod(label = "添加物料", auth = "read")
    public Object addMaterial(Records record, String code, String materialId, double qty) {
        Environment env = record.getEnv();
        Records workOrder = record.getRec("work_order_id");
        Records material = env.get("md.material", materialId);
        Records bom = env.get("mfg.work_order_bom").find(Criteria.equal("work_order_id", workOrder.getId())
            .and("material_id", "=", material.getId()));
        if (!bom.any()) {
            throw new ValidationException(record.l10n("物料[%s]不在工单[%s]BOM中", material.get("code"), workOrder.get("code")));
        }
        String stockRule = material.getString("stock_rule");
        String labelId = null;
        String lotNum = null;
        if (Utils.isNotEmpty(code)) {
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
            if (codes.length > 1) {
                if ("sn".equals(stockRule)) {
                    Records label = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                    if (!material.equals(label.getRec("material_id"))) {
                        throw new ValidationException(record.l10n("标签[%s]物料[%s]与物料[%s]不一致", code,
                            label.getRec("material_id").get("code"), material.get("code")));
                    }
                    String status = label.getString("status");
                    if (!"stock-out".equals(status) && !"to-return".equals(status) && !"new".equals(status)) {
                        throw new ValidationException(record.l10n("标签[%s]状态[%s]，不能退料", label.get("sn"), label.getSelection("status")));
                    }
                    labelId = label.getId();
                    lotNum = label.getString("lot_num");
                    label.set("qty", qty);
                    label.set("status", "to-stock");
                } else if ("lot".equals(stockRule)) {
                    if (!Utils.equals(material.get("code"), codes[1])) {
                        throw new ValidationException(record.l10n("标签[%s]物料[%s]与物料[%s]不一致", code, codes[1], material.get("code")));
                    }
                    lotNum = codes[2];
                }
            }
        }
        if ("sn".equals(stockRule) && Utils.isEmpty(labelId) || "lot".equals(stockRule) && Utils.isEmpty(lotNum)) {
            throw new ValidationException(record.l10n("[%s]物料[%s]请使用标签退料", material.getSelection("stock_rule"), material.get("code")));
        }
        String type = record.getString("return_type");
        env.get("mfg.material_return_details").create(new KvMap()
            .set("material_return_id", record.getId())
            .set("material_id", material.getId())
            .set("label_id", labelId)
            .set("lot_num", lotNum)
            .set("qty", qty)
            .set("status", "defect".equals(type) ? "to-inspect" : "to-stock"));
        return Action.reload(record.l10n("物料[%s]退料数量[%s]", material.get("code"), qty));
    }
}
