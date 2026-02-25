package jmaa.modules.print.models;


import org.jmaa.sdk.AbstractModel;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.exceptions.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "print.adapter.stimulsoft", label = "StimulSoft打印")
public class StimulSoftAdapter extends AbstractModel {
    /**
     * 执行打印标签
     *
     * @param rec
     * @param templateId 打印模板id
     */
    public Map<String, Object> print(Records rec, String templateId, Object fields, List<Map<String, Object>> data) {
        Records template = rec.getEnv().get("print.template", templateId);
        if (template.get("file") == null) {
            throw new ValidationException(rec.l10n("缺少打印模板，请上传！"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("type", "sti");
        result.put("data", data);
        result.put("template", String.format("/%s/print/%s", rec.getEnv().getRegistry().getTenant().getKey(), template.getId()));
        return result;
    }

    public Map<String, Object> design(Records rec, String templateId, Object fields, List<Map<String, Object>> data) {
        Map<String, Object> result = new HashMap<>();

        if (fields instanceof  List) {
            Map<String, Object> dataMap = new HashMap<>();
            for (String field : (List<String>)fields) {
                dataMap.put(field, field);
            }
            result.put("data",  dataMap);
        } else if (fields instanceof HashMap) {
            result.put("data", fields);
        }

        Records template = rec.getEnv().get("print.template", templateId);
        if (template.get("file") != null) {
            result.put("template", String.format("/%s/print/%s", rec.getEnv().getRegistry().getTenant().getKey(), template.getId()));
        }

        return result;
    }
}
