package jmaa.modules.md.resource.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.util.KvMap;

import java.util.Map;

@Model.Meta(name = "md.equipment", label = "设备台账", present = {"code"}, order = "seq")
public class Equipment extends Model {
    static Field code = Field.Char().label("设备编码").required().unique();
    static Field name = Field.Char().label("设备名称").required();
    static Field model_id = Field.Many2one("md.equipment_model").label("设备型号").required();
    static Field specification = Field.Char().related("model_id.specification").label("技术规格");
    static Field type_id = Field.Many2one("md.equipment_type").related("model_id.type_id").label("设备类型");
    static Field type_category = Field.Selection().related("model_id.type_id.category").label("设备类别");
    static Field image = Field.Image().label("设备图片").maxWidth(300).maxHeight(300);
    static Field enterprise_id = Field.Many2one("md.enterprise_model").label("组织归属");
    static Field attachments = Field.Binary().label("附件");
    static Field warranty_date = Field.Date().label("保修截止日期");
    static Field in_charge_uid = Field.Many2one("rbac.user").label("设备负责人");
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间");
    static Field resource_id = Field.Many2one("md.work_resource").label("产线");

    static Field seq = Field.Integer().label("显示顺序").defaultValue(10).required();
    static Field is_resource = Field.Boolean().label("是否制造资源");

    @ActionMethod
    public Action onModelChange(Records record) {
        AttrAction action = new AttrAction();
        Records model = record.getRec("model_id");
        action.setValue("specification", model.get("specification"));
        action.setValue("type_id", model.getRec("type_id"));
        action.setValue("type_category", model.get("type_category"));
        return action;
    }

    @ServiceMethod(label = "设置为资源")
    public Object setResource(Records records) {
        records.set("is_resource", true);
        return Action.success();
    }

    @OnSaved({"code", "name", "workshop_id", "seq"})
    public void onSaved(Records records) {
        Records resources = records.getEnv().get("md.work_resource")
            .find(Criteria.in("equipment_id", records.getIds()));
        for (Records record : records) {
            Records resource = resources.filter(r -> r.getRec("equipment_id").equals(record));
            if (resource.any()) {
                resource.set("code", record.get("code"));
                resource.set("name", record.get("name"));
                resource.set("seq", record.get("seq"));
                resource.set("workshop_id", record.get("workshop_id"));
            }
        }
    }

    @OnSaved("is_resource")
    public void onIsResourceSaved(Records records) {
        Records resources = records.getEnv().get("md.work_resource").find(Criteria.in("equipment_id", records.getIds()));
        for (Records record : records) {
            boolean isResource = record.getBoolean("is_resource");
            Records resource = resources.filter(r -> r.getRec("equipment_id").equals(record));
            if (isResource) {
                if (resource.any()) {
                    resource.set("active", true);
                } else {
                    resource.create(getResourceMap(record));
                }
            } else if (resource.any()) {
                resource.set("active", false);
            }
        }
    }

    public Map<String, Object> getResourceMap(Records record) {
        return new KvMap()
            .set("code", record.get("code"))
            .set("name", record.get("name"))
            .set("seq", record.get("seq"))
            .set("equipment_id", record.getId())
            .set("workshop_id", record.getRec("workshop_id").getId())
            .set("type", "equipment");
    }
}
