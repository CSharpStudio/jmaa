package jmaa.modules.wms.qc.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.DateUtils;
import org.jmaa.sdk.util.KvMap;

import java.util.Date;
import java.util.Map;

/**
 * 来料检验。
 * 定义在主数据，可以从wms和iqc模块扩展
 */
@Model.Meta(name = "iqc.sheet", label = "来料检验单", inherit = {"code.auto_code", "mixin.material", "mixin.company"})
public class IqcSheet extends Model {
    static Field code = Field.Char().label("检验单号").unique();
    static Field qty = Field.Float().label("送检数量").required().min(0D).greaterThen(0D);
    static Field delivery_note = Field.Char().label("送货单号");
    static Field status = Field.Selection(new Options() {{
        put("to-inspect", "待检验");
        put("exempted", "免检");
        put("done", "已完成");
    }}).label("检验状态").defaultValue("to-inspect").required().tracking();
    static Field result = Field.Selection(new Options() {{
        put("ok", "合格");
        put("ng", "不合格");
    }}).label("检验结果").tracking();
    static Field related_code = Field.Char().label("收货单号");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field mrb_id = Field.Many2one("qsd.mrb").label("MRB").ondelete(DeleteMode.SetNull);

    @Model.ActionMethod
    public Action onMaterialChange(Records record) {
        Records material = record.getRec("material_id");
        Records unit = material.getRec("unit_id");
        return Action.attr().setValue("material_name_spec", material.get("name_spec"))
            .setValue("material_category", material.get("category"))
            .setValue("unit_id", material.getRec("unit_id").getPresent())
            .setAttr("qty", "data-decimals", unit.getInteger("accuracy"));
    }

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

    @Model.ServiceMethod(label = "创建MRB")
    public Object createMrb(Records record) {
        Records mrb = record.getRec("mrb_id");
        if (mrb.any()) {
            throw new ValidationException(record.l10n("已存在MRB[%s]，不可以重复创建", mrb.get("present")));
        }
        KvMap map = new KvMap()
            .set("material_id", record.getRec("material_id").getId())
            .set("qty", record.get("qty"))
            .set("type", "iqc.sheet")
            .set("related_code", record.get("code"))
            .set("related_model", record.getMeta().getName())
            .set("related_id", record.getId())
            .set("supplier_id", record.getRec("supplier_id").getId());
        mrb = mrb.create(map);
        record.set("mrb_id", mrb.getId());
        return Action.success();
    }

    /**
     * 判断是否免检
     */
    public boolean isExempt(Records record, Records material, Records supplier) {
        Records exempt = record.getEnv().get("iqc.exemption_list")
            .find(Criteria.equal("material_id", material.getId())
                .and("supplier_id", "=", supplier.getId())
                .and("begin_date", "<=", new Date())
                .and("end_date", ">=", DateUtils.addDays(new Date(), -1)));
        return exempt.any();
    }
}
