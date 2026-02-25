package org.jmaa.base.models;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmaa.sdk.core.Loader;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.AbstractModel;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tenants.TenantService;
import org.jmaa.sdk.tools.HttpUtils;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.tools.SpringUtils;
import org.jmaa.sdk.util.License;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

/**
 * HTTP类
 *
 * @author Eric Liang
 */
@Model.Meta(name = "ir.http")
public class IrHttp extends AbstractModel {
    static final String COPYRIGHT = "JMAA LGPL v3";
    static final String VERSION = "V1.0";

    public void web(Records rec, String name, HttpServletRequest request, HttpServletResponse response) {
        Environment env = rec.getEnv();
        String key = name + (env.isDebug() ? ":debug" : "");
        String view = env.getTenantData(key,
            () -> ((String) env.get("ir.ui.view").call("loadWeb", name))
                .replace("{{COPYRIGHT}}", COPYRIGHT));
        view = view.replace("{{THEME}}", getTheme(env))
            .replace("{{USERNAME}}", (String) env.getUser().get("name"))
            .replace("{{LICENSE-EXPIRE}}", licenseExpire(rec))
            .replaceAll("\\{\\{TITLE\\}\\}", getTitle(env));
        HttpUtils.writeHtml(response, view);
    }

    public void login(Records rec, HttpServletRequest request, HttpServletResponse response) {
        Environment env = rec.getEnv();
        String key = "web.login" + (env.isDebug() ? ":debug" : "");
        String view = env.getTenantData(key,
            () -> ((String) env.get("ir.ui.view").call("loadWeb", "base.web_login"))
                .replace("{{COPYRIGHT}}", COPYRIGHT));
        view = view.replace("{{THEME}}", getTheme(env))
            .replaceAll("\\{\\{TITLE\\}\\}", getTitle(env));
        HttpUtils.writeHtml(response, view);
    }

    public void view(Records rec, String name, HttpServletRequest request, HttpServletResponse response) {
        Environment env = rec.getEnv();
        String key = name + (env.isDebug() ? ":debug" : "");
        String view = env.getTenantData(key,
            () -> ((String) env.get("ir.ui.view").call("loadWeb", name)));
        view = view.replace("{{THEME}}", getTheme(env))
            .replaceAll("\\{\\{TITLE\\}\\}", getTitle(env));
        HttpUtils.writeHtml(response, view);
    }

    String getTheme(Environment env) {
        String theme = env.getUser().getString("theme");
        if (Utils.isEmpty(theme)) {
            theme = "theme-jmaa";
        }
        return theme;
    }

    public void test(Records rec, HttpServletRequest request, HttpServletResponse response) {
        Environment env = rec.getEnv();
        String view = (String) env.get("ir.ui.view").call("loadWeb", "base.web_test");
        HttpUtils.writeHtml(response, view);
    }

    @Model.ServiceMethod()
    public String loadAbout(Records record) {
        String about = (String) record.getEnv().get("ir.ui.view").call("loadWeb", "base.web_about");
        about = about.replaceAll("\\{\\{LICENSE\\}\\}", Loader.get(License.class).getInfo());
        boolean loadDemo = ObjectUtils.toBoolean(SpringUtils.getProperty("loadDemo"));
        String debug = String.format("构建时间：%s<br/>启动时间：%s<br/>", getBuildTime(), TenantService.getStartTime());
        if (loadDemo) {
            debug += "加载示例数据";
        }
        about = about.replace("{{DEBUG}}", debug)
            .replace("{{VERSION}}", VERSION)
            .replaceAll("\\{\\{TITLE\\}\\}", getTitle(record.getEnv()))
            .replace("{{COPYRIGHT}}", COPYRIGHT);
        return about;
    }

    @Model.ServiceMethod()
    public void updateLicense(Records record, String company, String license) {
        boolean isAdmin = (Boolean) record.getEnv().get("rbac.security").call("isAdmin", record.getEnv().getUserId());
        if (!isAdmin) {
            throw new ValidationException("只有管理员可以更新授权");
        }
        Loader.get(License.class).update(company, license);
        record.getEnv().get("ir.module").call("resetTenant");
    }

    private String getBuildTime() {
        try {
            Properties properties = PropertiesLoaderUtils.loadAllProperties("maven.properties");
            String bt = properties.getProperty("maven.buildTime");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = simpleDateFormat.parse(bt);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.HOUR_OF_DAY, +8);
            return simpleDateFormat.format(calendar.getTime());
        } catch (Throwable e) {
            return "";
        }
    }

    private String getTitle(Environment env) {
        String key = env.getRegistry().getTenant().getKey();
        if ("root".equals(key)) {
            return env.l10n("企业微应用平台");
        }
        return env.l10n("企业微应用平台") + " - " + env.getRegistry().getTenant().getName();
    }

    private String licenseExpire(Records rec) {
        License license = Loader.getLoader().get(License.class);
        if (license.getStyle() == 1) {
            if (license.getDueDate().before(Utils.addDays(new Date(), 5))) {
                String message = license.getDueDate().before(new Date()) ? "授权已于%s过期" : "授权将于%s到期";
                return String.format("<div style='background:#ffc107;padding: 0 5px;color: darkred;'>%s</div>",
                    rec.l10n(message, new SimpleDateFormat("yyyy-MM-dd").format(license.getDueDate())));
            }
        }
        return "";
    }
}
