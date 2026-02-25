package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Model.Meta(name = "md.sub_resource", label = "辅助资源台账")
public class SubResource extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field model_id = Field.Many2one("md.sub_resource_model").label("型号").required();
    static Field specification = Field.Char().related("model_id.specification").label("技术规格");
    static Field type_id = Field.Many2one("md.equipment_type").related("model_id.type_id").label("类型");
    static Field usable_begin = Field.Date().label("可用开始时间").required();
    static Field usable_end = Field.Date().label("可用结束时间").required();
    static Field usable = Field.Boolean().label("是否可用").defaultValue(true);
    static Field product_ids = Field.One2many("md.sub_resource_product", "sub_resource_id").label("产品适配");
    static Field work_resource_ids = Field.One2many("md.sub_work_resource", "").label("制造资源").compute("computeWorkResource");

    public Object computeWorkResource(Records record) {
        return Collections.emptyList();
    }

    @ServiceMethod(label = "读取制造资源", auth = "read")
    public Object searchWorkResource(Records record) {
        List<Map<String, Object>> result = (List<Map<String, Object>>) record.getRec("model_id").call("getModelResources");
        List<String> resourceIds = result.stream().map(r -> (String) r.get("work_resource_id")).collect(Collectors.toList());
        Set<String> craftTypeIds = result.stream().map(r -> (String) r.get("craft_type_id")).collect(Collectors.toSet());
        Records resource = record.getEnv().get("md.work_resource", resourceIds);
        Records craftType = record.getEnv().get("md.craft_type", craftTypeIds);
        for (Map<String, Object> row : result) {
            Records res = resource.filter(r -> Utils.equals(r.getId(), row.get("work_resource_id"))).first();
            Records craft = craftType.filter(r -> Utils.equals(r.getId(), row.get("craft_type_id"))).first();
            row.put("work_resource_id", res.getPresent());
            row.put("type", res.get("type"));
            row.put("workshop_id", res.getRec("workshop_id").getPresent());
            row.put("craft_type_id", craft.getPresent());
        }
        return result;
    }

    @ActionMethod
    public Object onModelChange(Records record) {
        Records model = record.getRec("model_id");
        return Action.attr().setValue("specification", model.get("specification"))
            .setValue("type_id", model.getRec("type_id"));
    }
}
