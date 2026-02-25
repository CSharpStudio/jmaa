package org.jmaa.base.models;

import org.jmaa.sdk.*;

import java.util.*;

/**
 * 本地化翻译
 *
 * @author Eric Liang
 */
@Model.Meta(name = "res.localization", label = "本地化", order = "name")
@Model.UniqueConstraint(name = "name_lang_unique", fields = {"name", "lang_id"}, message = "名称不能重复")
public class ResLocalization extends Model {
    static Field name = Field.Char().label("名称").required().length(500);
    static Field value = Field.Char().label("翻译值").length(1000);
    static Field lang_id = Field.Many2one("res.lang").label("语言").required();

    @Override
    public Map<String, Integer> createOrUpdate(Records record, List<Map<String, Object>> values) {
        record.getEnv().getContext().put("update-version", false);
        Map<String, Integer> result = (Map<String, Integer>) record.callSuper(ResLocalization.class, "createOrUpdate", values);
        Set<String> langIds = new HashSet<>();
        for (Map<String, Object> row : values) {
            langIds.add((String) row.get("lang_id"));
        }
        if (langIds.size() > 0) {
            record.getEnv().getCursor().execute("UPDATE res_lang SET version=version+1 WHERE id in %s", Arrays.asList(langIds));
        }
        return result;
    }

    @Constrains("value")
    public void valueConstraint(Records records) {
        boolean updateVersion = Utils.toBoolean(records.getEnv().getContext().get("update-version"), true);
        if (updateVersion) {
            Set<String> langIds = new HashSet<>();
            for (Records record : records) {
                langIds.add(record.getRec(lang_id).getId());
            }
            if (langIds.size() > 0) {
                records.getEnv().getCursor().execute("UPDATE res_lang SET version=version+1 WHERE id in %s", Arrays.asList(langIds));
            }
        }
    }
}
