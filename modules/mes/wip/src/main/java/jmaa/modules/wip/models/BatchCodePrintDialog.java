package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

import java.util.Collection;
import java.util.Map;

@Model.Meta(name = "wip.batch_code_print_dialog", label = "批次条码打印对话框")
public class BatchCodePrintDialog extends ValueModel {
    static Field work_order_id = Field.Many2one("mfg.work_order").label("生产工单").required();
    static Field work_order_qty = Field.Float().label("工单数量").readonly();
    static Field material_id = Field.Many2one("md.material").label("产品编码").readonly();
    static Field material_name_spec = Field.Char().label("规格型号").readonly();
    static Field unit_id = Field.Many2one("md.unit").label("单位").readonly();
    static Field print_qty = Field.Float().label("打印数量").greaterThen(0d).required();
    static Field batch_rule_id = Field.Many2one("wip.batch_rule").label("批次规则").required();
    static Field batch_qty = Field.Float().label("批次数量").greaterThen(0d).required();
    static Field template_id = Field.Many2one("print.template").label("打印模板").required();
    static Field coding_id = Field.Many2one("code.coding").label("编码规则").required();
    static Field supplier_id = Field.Many2one("md.supplier").label("生产商").defaultValue(Default.method("defaultSupplier"));
    static Field product_date = Field.Date().label("生产日期").required();

    public Object defaultSupplier(Records record) {
        return record.getEnv().getCompany().getRec("supplier_ids").firstOrDefault().getPresent();
    }

    @Override
    public Map<String, Object> searchByField(Records rec, String relatedField, Criteria criteria, Integer offset, Integer limit, Collection<String> fields, String order) {
        if ("supplier_id".equals(relatedField)) {
            criteria.and(Criteria.in("id", rec.getEnv().getCompany().getRec("supplier_ids").getIds()));
        }
        return (Map<String, Object>) callSuper(rec, relatedField, criteria, offset, limit, fields, order);
    }

    @ActionMethod
    public Action onWorkOrderChange(Records record) {
        Records workOrder = record.getRec("work_order_id");
        Records material = workOrder.getRec("material_id");
        AttrAction action = new AttrAction();
        action.setValue("work_order_qty", workOrder.get("plan_qty"));
        action.setValue("material_id", material.getPresent());
        action.setValue("material_name_spec", material.get("name_spec"));
        action.setValue("unit_id", material.getRec("unit_id").getPresent());
        return action;
    }

    @ActionMethod
    public Action onBatchRuleChange(Records record) {
        Records rule = record.getRec("batch_rule_id");
        AttrAction action = new AttrAction();
        action.setValue("batch_qty", rule.get("qty"));
        action.setValue("coding_id", rule.getRec("coding_id").getPresent());
        action.setValue("template_id", rule.getRec("template_id").getPresent());
        return action;
    }
}
