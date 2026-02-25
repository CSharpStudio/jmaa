package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.util.KvMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "md.sub_resource_model", label = "辅助资源型号")
public class SubResourceModel extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field specification = Field.Char().label("技术规格");
    static Field manufacturer = Field.Char().label("生产厂商");
    static Field type_id = Field.Many2one("md.sub_resource_type").label("类型").required();
    static Field work_resource_ids = Field.One2many("md.sub_work_resource", "model_id").label("制造资源");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field sub_resource_ids = Field.One2many("md.sub_resource", "model_id").label("辅助资源");

    public List<Map<String, Object>> getModelResources(Records records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Records record : records) {
            result.addAll(getModelResource(record));
        }
        return result;
    }

    public List<Map<String, Object>> getModelResource(Records record) {
        Records resources = record.getEnv().get("md.sub_work_resource").find(Criteria.equal("model_id", record.getId()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Records row : resources) {
            result.add(new KvMap()
                .set("priority", row.get("priority"))
                .set("work_resource_id", row.getRec("work_resource_id").getId())
                .set("model_id", row.getRec("model_id").getId())
                .set("craft_type_id", row.getRec("craft_type_id").getId()));
        }
        return result;
    }
}
