package jmaa.modules.tenant.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tenants.TenantService;
import org.jmaa.sdk.tools.DateUtils;
import org.jmaa.sdk.tools.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;

import static jmaa.modules.tenant.models.TenantDataSource.CODE_FORMAT_PATTERN;
import static jmaa.modules.tenant.models.TenantDataSource.USER_PWD_PATTERN;

@Model.Meta(name = "tenant.info")
public class TenantInfo extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field code = Field.Char().label("识别码").required().help("数字和小写字母").unique();
    static Field db = Field.Char().label("数据库");
    static Field enterprise = Field.Char().label("企业");
    static Field data_source = Field.Many2one("tenant.data_source").label("数据源").required();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field expiration = Field.Date().label("到期日期");

    @Override
    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        Map<String, Object> result = (Map<String, Object>) callSuper(rec, values);
        String code = (String) result.get("code");
        if (Utils.isNotEmpty(code)) {
            String lower = code.toLowerCase();
            if ("root".equals(lower)) {
                throw new ValidationException(rec.l10n("无效的识别码"));
            }
            result.put("code", lower);
        }
        return result;
    }

    public void loadTenants(Records rec) {
        updateTenants(rec.find(Criteria.equal("active", true)
            .and("expiration", ">=", DateUtils.addDays(new Date(), -1))));
    }

    public void updateTenants(Records records) {
        for (Records row : records) {
            String db = row.getString("db");
            String settings = row.getRec("data_source").getString("settings");
            String config = decodeConfig(settings).replaceAll("\\{db\\}", db);
            String code = row.getString("code");
            Properties prop = new Properties();
            try {
                prop.load(new ByteArrayInputStream(config.getBytes()));
                Tenant tenant = new Tenant(code, row.getString("name"), prop);
                TenantService.register(tenant);
            } catch (IOException e) {
                records.getLogger().error(String.format("加载租户[%s]失败", code), e);
            }
        }
    }

    private static String decodeConfig(String config) {
        Matcher matcher = USER_PWD_PATTERN.matcher(config);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String codedValue = matcher.group(2).trim();
            String lineEnd = matcher.group(3);
            Matcher codeMatcher = CODE_FORMAT_PATTERN.matcher(codedValue);
            if (codeMatcher.matches()) {
                String encryptedStr = codeMatcher.group(1);
                String plainStr = DigestUtils.DES.decode(encryptedStr);
                String newKeyValue = String.format("%s=%s%s",
                    key,
                    Matcher.quoteReplacement(plainStr),
                    lineEnd);
                matcher.appendReplacement(result, newKeyValue);
            } else {
                matcher.appendReplacement(result, matcher.group());
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Constrains("code")
    public void codeConstrains(Records records) {
        Cursor cr = records.getEnv().getCursor();
        for (Records row : records) {
            String code = row.getString("code");
            String db = "db_" + code;
            if (cr.getSqlDialect().databaseExists(cr, db)) {
                throw new ValidationException(row.l10n("无效的识别码"));
            }
            cr.getSqlDialect().createDatabase(cr, db);
            row.set("db", db);
        }
        updateTenants(records);
    }

    @Model.ServiceMethod(label = "重启租户")
    public Object resetTenant(Records rec) {
        Tenant tenant = TenantService.get(rec.getString("code"));
        Tenant newTenant = new Tenant(tenant.getKey(), tenant.getName(), tenant.getProperties());
        try {
            newTenant.getRegistry();
            TenantService.register(newTenant);
        } catch (Exception exc) {
            newTenant.close();
            throw exc;
        }
        //关闭释放原来的租户
        tenant.close();
        return Action.reload("已重启");
    }
}
