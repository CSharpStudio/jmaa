package jmaa.modules.api.doc.controllers;

import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import org.jmaa.sdk.Manifest;
import org.jmaa.sdk.core.BaseService;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.core.MetaModule;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.fields.RelationalField;
import org.jmaa.sdk.tenants.TenantService;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.tools.HttpUtils;
import org.jmaa.sdk.tools.ThrowableUtils;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * API控制器
 *
 * @author 梁荣振
 */
@org.springframework.stereotype.Controller
public class ApiController {
    @RequestMapping(value = "/{tenant}/api", method = RequestMethod.GET)
    public void handler(@PathVariable("tenant") String tenant, HttpServletRequest request,
                        HttpServletResponse response) {
        try {
            Registry registry = TenantService.get(tenant).getRegistry();
            Map<String, List<MetaModel>> moduleModels = new HashMap<>();
            Map<String, Map<String, Object>> data = new HashMap<>();
            for (MetaModel model : registry.getModels().values()) {
                if (!model.isAbstract() && model.getService().size() > 0) {
                    List<MetaModel> models = moduleModels.get(model.getModule());
                    if (models == null) {
                        models = new ArrayList<>();
                        moduleModels.put(model.getModule(), models);
                    }
                    models.add(model);
                }
                data.put(model.getName(), new KvMap() {{
                    put("label", model.getLabel());
                    put("description", model.getDescription());
                    put("auth", model.getAuthModel());
                    put("fields", model.getFields().values().stream().map(f -> new KvMap() {{
                        put("name", f.getName());
                        put("label", f.getLabel());
                        put("type", f.getType());
                        put("store", f.isStore());
                        put("required", f.isRequired());
                        put("comodel", (f instanceof RelationalField) ? ((RelationalField) f).getComodel() : "");
                        put("help", f.getHelp());
                    }}).collect(Collectors.toList()));
                    put("uniques", model.getUniques().stream().map(u -> new KvMap() {{
                        put("name", u.getName());
                        put("fields", Arrays.stream(u.getFields()).collect(Collectors.joining(", ")));
                        put("message", u.getMessage());
                    }}).collect(Collectors.toList()));
                    put("services", model.getService().values().stream().map(s -> new KvMap() {{
                        put("name", s.getName());
                        put("label", s.getLabel());
                        put("desc", s.getDescription());
                        put("auth", s.getAuth());
                        put("args", s.getArgsDoc(model));
                        put("result", s.getResultDoc(model));
                    }}).collect(Collectors.toMap(m -> m.get("name"), m -> m)));
                }});
            }
            String menu = getMenu(registry, moduleModels);
            String html = getHtml(menu, JSON.toJSONString(data), tenant, registry);
            HttpUtils.writeHtml(response, html);
        } catch (Exception e) {
            HttpUtils.writeHtml(response, ThrowableUtils.getDebug(e));
        }
    }

    String getMenu(Registry registry, Map<String, List<MetaModel>> moduleModels) {
        StringBuilder sb = new StringBuilder(1000);
        for (MetaModule module : registry.getModules().values()) {
            Manifest manifest = module.getManifest();
            List<MetaModel> models = moduleModels.get(manifest.name());
            if (models != null) {
                appendLine(sb, "<div class='menu-item'>");
                appendLine(sb, "<div class='menu-title' title='%s (%s)'>", manifest.label(), manifest.name());
                appendLine(sb, "<span class='menu-check'>></span>%s (%s)", manifest.label(), manifest.name());
                appendLine(sb, "</div>");
                appendLine(sb, "<div class='menu-subs'>%s", getModelMenu(models));
                appendLine(sb, "</div>");
                appendLine(sb, "</div>");
            }
        }
        return sb.toString();
    }

    String getModelMenu(List<MetaModel> models) {
        StringBuilder sb = new StringBuilder(1000);
        for (MetaModel model : models) {
            appendLine(sb, "<div class='menu-title' data-model='%s' title='%s (%s)'>", model.getName(), model.getLabel(), model.getName());
            appendLine(sb, "<span class='menu-check'>></span>%s (%s)", model.getLabel(), model.getName());
            appendLine(sb, "</div>");
            appendLine(sb, "<div class='menu-subs'>%s", getServiceMenu(model));
            appendLine(sb, "</div>");
        }
        return sb.toString();
    }

    String getServiceMenu(MetaModel model) {
        StringBuilder sb = new StringBuilder(1000);
        for (BaseService service : model.getService().values()) {
            appendLine(sb, "<div class='menu-title' data-model='%s' data-service='%s' title='%s (%s)'>", model.getName(), service.getName(), service.getLabel(), service.getName());
            appendLine(sb, "<span class='menu-point'>○</span>%s (%s)", service.getLabel(), service.getName());
            appendLine(sb, "</div>");
        }
        return sb.toString();
    }

    void appendLine(StringBuilder sb, String str, Object... formatArgs) {
        if (str != null) {
            if (formatArgs.length > 0) {
                sb.append(String.format(str, formatArgs));
            } else {
                sb.append(str);
            }
        }
        sb.append("\r\n");
    }

    String getHtml(String menu, String data, String tenant, Registry registry) {
        StringBuilder sb = new StringBuilder(1000);
        appendLine(sb, "<html>");
        appendLine(sb, "    <head>");
        appendLine(sb, "        <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
        appendLine(sb, "        <title>API Document</title>");
        appendLine(sb, "        <script src='/web/org/jmaa/web/statics/plugins/jquery/jquery.min.js?etag=3.6.0'></script>");
        appendLine(sb, "        <script src='/web/jmaa/modules/api/doc/statics/js/api-doc.js'></script>");
        appendLine(sb, "        <link rel='stylesheet' type='text/css' href='/web/jmaa/modules/api/doc/statics/style/api-doc.css'>");
        appendLine(sb, "    </head>");
        appendLine(sb, "    <body>");
        appendLine(sb, "        <div class='header'>");
        appendLine(sb, "            <div class='title'>");
        appendLine(sb, "                <a>API 文档</a>");
        appendLine(sb, "            </div>");
        appendLine(sb, "            <div class='login'>");
        appendLine(sb, "                <a href='/%s/login?url=/%s/api'>登录</a>", tenant, tenant);
        appendLine(sb, "            </div>");
        appendLine(sb, "        </div>");
        appendLine(sb, "        <div class='aside'>%s</div>", menu);
        appendLine(sb, "        <div class='content'><div><div class='main'></div></div></div>");
        appendLine(sb, "        <script type='text/javascript'>let tenant='%s', data = %s</script>", tenant, data);
        appendLine(sb, "    </body>");
        appendLine(sb, "</html>");
        return sb.toString();
    }
}
