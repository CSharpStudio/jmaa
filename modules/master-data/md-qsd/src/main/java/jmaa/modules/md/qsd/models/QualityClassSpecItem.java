package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.ThrowableUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "qsd.quality_class_spec_item", label = "分类检验项目", inherit = "qsd.inspect_item", authModel = "qsd.quality_class_spec")
@Model.UniqueConstraint(name = "class_item_name_unique", fields = {"name", "spec_id"})
public class QualityClassSpecItem extends Model {
    static Field mode = Field.Selection(new Options() {{
        put("normal", "正常");
    }}).label("检验方式").required().useCatalog();
    static Field spec_id = Field.Many2one("qsd.quality_class_spec").label("分类检验标准").ondelete(DeleteMode.Cascade);
    static Field category = Field.Selection(Selection.related("qsd.inspect_item", "category")).useCatalog(false);

    @Override
    public Map<String, Integer> createOrUpdate(Records record, List<Map<String, Object>> values) {
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> value : values) {
            try {
                Criteria criteria = new Criteria();
                for (String field : Utils.asList("name", "spec_id")) {
                    Object v = value.get(field);
                    criteria.and(Criteria.equal(field, v));
                }
                record = record.find(criteria);
                if (record.any()) {
                    record.update(value);
                    updated++;
                } else {
                    record.create(value);
                    created++;
                }
            } catch (Exception e) {
                Throwable t = ThrowableUtils.getCause(e);
                if (t instanceof ValidationException) {
                    errors.add(t.getMessage());
                } else {
                    throw e;
                }
            }
        }
        if (errors.size() > 0) {
            throw new ValidationException(errors.stream().collect(Collectors.joining("\r\n")));
        }
        HashMap<String, Integer> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        return result;
    }
}
