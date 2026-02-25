package jmaa.modules.wms.qc.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Map;

@Model.Meta(name = "oqc.sheet", label = "出货检验单", inherit = {"code.auto_code", "mixin.material", "mixin.company"})
public class OqcSheet extends Model {
    static Field code = Field.Char().label("检验单号").unique();
    static Field qty = Field.Float().label("送检数量").required().min(0D).greaterThen(0D);
    static Field status = Field.Selection(new Options() {{
        put("to-inspect", "待检验");
        put("exempted", "免检");
        put("done", "已完成");
    }}).label("检验状态").defaultValue("to-inspect").required().tracking();
    static Field result = Field.Selection(new Options() {{
        put("ok", "合格");
        put("ng", "不合格");
    }}).label("检验结果").tracking();
    static Field related_code = Field.Char().label("送货单号");
    static Field sales_code = Field.Char().label("销售单号");
    static Field customer_id = Field.Many2one("md.customer").label("客户").required();
    static Field mrb_id = Field.Many2one("qsd.mrb").label("MRB").ondelete(DeleteMode.SetNull);

    @Model.ServiceMethod(label = "提交", doc = "提交检验的检验单")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        if (values != null) {
            records.update(values);
        }
        for (Records record : records) {
            if (!"to-inspect".equals(record.getString("status"))) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以提交", record.getSelection("status")));
            }
            if (Utils.isEmpty(record.get("result"))) {
                throw new ValidationException(records.l10n("检验项目未完成"));
            }
        }
        records.set("status", "done");
        return Action.success();
    }

    /**
     * 判断是否免检
     */
    public boolean isExempt(Records record, Records material, Records customer) {
        return false;
    }
}
