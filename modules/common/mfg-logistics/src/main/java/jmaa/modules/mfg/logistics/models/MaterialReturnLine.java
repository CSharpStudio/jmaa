package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

@Model.Meta(name = "mfg.material_return_line", label = "退料物料", authModel = "mfg.material_return", inherit = "mixin.material")
public class MaterialReturnLine extends Model {
    static Field material_return_id = Field.Many2one("mfg.material_return").label("生产退料");
    static Field status = Field.Selection(new Options(){{
        put("new", "待退料");
        put("returning", "退料中");
        put("returned", "已备齐");
        put("done", "已完成");
    }}).label("状态").readonly(true).defaultValue("new");
    static Field return_qty = Field.Float().label("退料数量").required().defaultValue(0);
    @ActionMethod
    public Action onMaterialChange(Records record) {
        AttrAction action = Action.attr();
        Records material = record.getRec("material_id");
        Records unit = material.getRec("unit_id");
        action.setValue("material_name_spec", material.get("name_spec"));
        action.setValue("material_category", material.get("category"));
        action.setValue("unit_id", unit);
        action.setAttr("return_qty", "data-decimals", unit.getInteger("accuracy"));
        return action;
    }
    /*@OnSaved("request_qty")
    public void onRequestQtySaved(Records records) {
        for (Records record : records) {
            Double requestQty = record.getDouble("request_qty");
            if (Utils.lessOrEqual(requestQty, 0)) {
                throw new ValidationException(record.l10n("物料[%s]需退数量必须大于0", record.getRec("material_id").get("code")));
            }
            Double returnedQty = record.getDouble("return_qty");
            if(Utils.less(requestQty, returnedQty)){
                throw new ValidationException(record.l10n("物料[%s]需退数量不能小于已退数量[%s]", record.getRec("material_id").get("code"), returnedQty));
            }
            if (Utils.largeOrEqual(returnedQty, requestQty)) {
                record.set("status", "returned");
            }
        }
    }*/

   /* @OnSaved("return_qty")
    public void onReturnedQtySaved(Records records) {
        // 放这里修改?每次都改不好吧
        for (Records record : records) {
            record.set("status", "returning");
        }
    }*/
}
