package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.util.KvMap;

import java.util.Map;

@Model.Meta(inherit = "md.equipment")
public class Equipment extends Model {
    public Map<String, Object> getResourceMap(Records record) {
        Records craft = record.getRec("model_id").getRec("type_id").getRec("craft_type_id");
        return new KvMap()
            .set("code", record.get("code"))
            .set("name", record.get("name"))
            .set("seq", record.get("seq"))
            .set("craft_type_id", craft.getId())
            .set("equipment_id", record.getId())
            .set("workshop_id", record.getRec("workshop_id").getId())
            .set("type", "equipment");
    }
}
