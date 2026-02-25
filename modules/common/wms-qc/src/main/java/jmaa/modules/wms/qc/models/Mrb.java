package jmaa.modules.wms.qc.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "qsd.mrb", label = "质量审核", inherit = {"mixin.order_status", "code.auto_code", "mixin.material", "mixin.company"})
@Model.Service(remove = {"create", "copy", "createBatch"})
public class Mrb extends Model {
    static Field code = Field.Char().label("单号").unique();
    static Field qty = Field.Float().label("送检数量").required();
    static Field type = Field.Selection(new Options() {{
        put("iqc.sheet", "来料检验");
    }}).label("检验类型").defaultValue("iqc.sheet");
    static Field related_code = Field.Char().label("相关单据");
    static Field related_model = Field.Char().label("单据模型");
    static Field related_id = Field.Many2oneReference("related_model").label("相关单据");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field inspect_uid = Field.Many2one("rbac.user").label("检验员");
    static Field inspect_time = Field.DateTime().label("检验时间");
    static Field result = Field.Selection(new Options() {{
        put("concession", "让步接收");
        put("return", "退货");
        put("pick", "挑选使用");
    }}).label("MRB结果");
    static Field attachments = Field.Binary().label("MRB审批单据");

    /*
     1.IQC不合格，不修改入库单的状态
     2.MRB结果让步接收审核时，修改入库单的状态为待入库
     3.挑选功能，创建挑选单
     4.MRB结果退供应商审核时，生成退供应商单, 并且将明细数据添加
    */
    @Model.ServiceMethod(label = "审核", doc = "审核单据，从提交状态改为已审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        // 审核以后不驳回了, 单据关联太多, 要驳回,提交以后驳回
        records.set("status", "done");
        return Action.success();
    }
}
