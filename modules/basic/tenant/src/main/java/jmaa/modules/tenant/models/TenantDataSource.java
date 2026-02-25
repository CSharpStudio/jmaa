package jmaa.modules.tenant.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.tools.DigestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Model.Meta(name = "tenant.data_source")
public class TenantDataSource extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field server = Field.Char().label("服务器");
    static Field settings = Field.Char().label("配置").length(4000).help("数据库名称使用占位符{db}");
    public static final Pattern USER_PWD_PATTERN = Pattern.compile("(username|password)=(.*?)(\\r?\\n|$)");
    public static final Pattern CODE_FORMAT_PATTERN = Pattern.compile("^<code\\|(.+?)>$");

    @OnSaved("settings")
    public void onSave(Records records) {
        for (Records record : records) {
            String setting = record.getString("settings");
            String encode = encodeConfig(setting);
            if (!Utils.equals(setting, encode)) {
                record.set("settings", encode);
            }
        }
    }

    private static String encodeConfig(String config) {
        Matcher matcher = USER_PWD_PATTERN.matcher(config);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            String lineEnd = matcher.group(3);
            if (CODE_FORMAT_PATTERN.matcher(value).matches()) {
                matcher.appendReplacement(result, matcher.group());
            } else {
                String encrypted = DigestUtils.DES.encode(value);
                String codedValue = String.format("<code|%s>", encrypted);
                String newKeyValue = String.format("%s=%s%s",
                    key,
                    Matcher.quoteReplacement(codedValue),
                    lineEnd);
                matcher.appendReplacement(result, newKeyValue);
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
