package jmaa.modules.print.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author eric
 */
@Model.Meta(name = "print.adapter.client", label = "标签打印")
public class ClientAdapter extends AbstractModel {

    /**
     * 执行打印标签
     *
     * @param rec
     * @param templateId 打印模板id
     */
    public Map<String, Object> print(Records rec, String templateId, List<String> fields, List<Map<String, Object>> data) {
        Records template = rec.getEnv().get("print.template", templateId);
        if (template.get("file") == null) {
            throw new ValidationException(rec.l10n("缺少打印模板，请上传！"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("data", toData(fields, data));
        result.put("type", "lpt");
        result.put("title", template.getString("name"));
        result.put("hash", template.getString("check_sum"));
        result.put("template", String.format("/%s/print/%s", rec.getEnv().getRegistry().getTenant().getKey(), template.getId()));
        return result;
    }

    String toData(List<String> fields, List<Map<String, Object>> data) {
        StringBuilder sb = new StringBuilder();
        //cvs表头
        sb.append(Utils.join(fields)).append("\r\n");
        for (Map<String, Object> row : data) {
            StringJoiner joiner = new StringJoiner(",");
            for (String field : fields) {
                Object value = row.get(field);
                joiner.add(Utils.toString(value));
            }
            sb.append(joiner);
            sb.append("\r\n");
        }
        return sb.toString();
    }

    public Map<String, Object> design(Records rec, String templateId, List<String> fields, List<Map<String, Object>> data) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", toData(fields, data));
        Records template = rec.getEnv().get("print.template", templateId);
        if (template.get("file") != null) {
            result.put("hash", template.getString("check_sum"));
            result.put("template", String.format("/%s/print/%s", rec.getEnv().getRegistry().getTenant().getKey(), template.getId()));
        }
        result.put("type", "lpt");
        result.put("title", template.getString("name"));
        return result;
    }

}
