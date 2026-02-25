package jmaa.modules.iqc.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.util.KvMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Model.Meta(inherit = "qsd.mrb")
public class Mrb extends Model {
    static Field inspection_item_ids = Field.One2many("qsd.mrb_inspect_item", "mrb_id").label("检验项目").store(false);
    @Model.ServiceMethod(label = "统计检验项目数量", auth = "read")
    public long countInspectItems(Records rec, Criteria criteria) {
        Records related = rec.getRec("related_id");
        if (Utils.isNotEmpty(related)) {
            return (long) related.call("countByField", "inspection_item_ids", criteria);
        }
        return 0;
    }

    @Model.ServiceMethod(label = "读取检验项目", auth = "read")
    public Map<String, Object> searchInspectItems(Records rec, Criteria criteria, Integer offset, Integer limit, Collection<String> fields, String order) {
        Records related = rec.getRec("related_id");
        if (Utils.isNotEmpty(related)) {
            return (Map<String, Object>) related.call("searchByField", "inspection_item_ids", criteria, offset, limit, fields, order);
        }
        return new KvMap().set("values", Collections.emptyList());
    }
}
