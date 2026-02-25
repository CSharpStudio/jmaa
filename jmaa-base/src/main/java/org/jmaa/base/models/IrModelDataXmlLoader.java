package org.jmaa.base.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmaa.base.utils.ImportErrorHandle;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValueException;
import org.jmaa.sdk.tools.IoUtils;
import org.jmaa.sdk.tools.ThrowableUtils;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.Tuple;
import org.jmaa.sdk.xml.XmlDocument;
import org.jmaa.sdk.xml.XmlDocumentFactory;
import org.jmaa.sdk.xml.XmlElement;
import org.jmaa.sdk.xml.XmlReader;
import org.dom4j.io.OutputFormat;
import org.springframework.util.ClassUtils;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Model.Meta(name = "ir.model.data.xml.loader", label = "模型数据XML加载器")
public class IrModelDataXmlLoader extends ValueModel {
    public void loadData(Records record, InputStream input, Map<String, Object> context, ImportErrorHandle onError) {
        XmlReader reader = new XmlReader();
        reader.setDocumentFactory(new XmlDocumentFactory());
        try {
            XmlDocument doc = reader.readXml(input);
            XmlElement root = doc.getRootXmlElement();
            boolean noUpdate = Utils.toBoolean(root.getAttributeOr("noupdate", null));
            for (XmlElement el : root.xmlElements()) {
                try {
                    loadElement(record, el, noUpdate, context, onError);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    handleError(onError, el, ex);
                }
            }
        } catch (Exception e) {
            handleError(onError, "读取xml失败", e);
        }
    }

    public void loadElement(Records record, XmlElement el, boolean noUpdate, Map<String, Object> context, ImportErrorHandle onError) {
        String module = (String) context.get("moduleName");
        String moduleId = (String) context.get("moduleId");
        String packageName = (String) context.get("packageName");
        if ("record".equals(el.getName())) {
            loadRecord(record, el, module, noUpdate, onError);
        } else if ("menu".equals(el.getName())) {
            loadMenu(record, el, packageName, module, null, "0", "ir.ui.menu");
        } else if ("m-menu".equals(el.getName())) {
            loadMenu(record, el, packageName, module, null, "0", "ir.ui.menu.mobile");
        } else if ("view".equals(el.getName())) {
            loadView(record, el, module, moduleId);
        } else if ("web".equals(el.getName())) {
            loadWeb(record, el, module, moduleId);
        }
    }

    private void loadWeb(Records record, XmlElement el, String module, String moduleId) {
        String id = getXmlId(el, module);
        String key = id;
        String inheritId = el.getAttributeOr("inherit_id", null);
        String mode = "primary";
        if (Utils.isNotEmpty(inheritId)) {
            if (!inheritId.contains(".")) {
                inheritId = module + "." + inheritId;
            }
            Records inherit = record.getEnv().getRef(inheritId);
            key = (String) inherit.get("key");
            inheritId = inherit.getId();
            mode = "extension";
        }
        String name = el.getAttribute("name");
        String priority = el.getAttributeOr("priority", "16");
        String arch = getXml(el, false);
        KvMap values = new KvMap()
            .set("key", key)
            .set("name", name)
            .set("arch", arch)
            .set("mode", mode)
            .set("module_id", moduleId)
            .set("inherit_id", inheritId)
            .set("priority", Integer.valueOf(priority))
            .set("type", "web");

        saveData(record, id, "ir.ui.view", values, false);
    }

    public static String getXml(XmlElement el, boolean expandEmpty) {
        String arch = "";
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        //xml节点自闭问题， jq处理自闭节点会格式错位
        format.setExpandEmptyElements(expandEmpty);
        format.setIndent("  ");
        for (XmlElement e : el.xmlElements()) {
            arch += e.asXML(format);
        }
        if (arch.startsWith("\n")) {
            arch = arch.substring(1);
        }
        return arch;
    }

    private void loadView(Records record, XmlElement el, String module, String moduleId) {
        String id = getXmlId(el, module);
        String name = el.getAttribute("name");
        String model = el.getAttribute("model");
        String inheritId = el.getAttributeOr("inherit_id", null);
        String key = el.getAttributeOr("key", null);
        String mode = "primary";
        String type = el.getAttributeOr("type", null);
        if (Utils.isNotEmpty(inheritId)) {
            if (!inheritId.contains(".")) {
                inheritId = module + "." + inheritId;
            }
            Records inherit = record.getEnv().getRef(inheritId);
            inheritId = inherit.getId();
            key = (String) inherit.get("key");
            mode = "extension";
            type = inherit.getString("type");
        }
        String priority = el.getAttributeOr("priority", "16");
        if (Utils.isEmpty(type)) {
            List<XmlElement> elements = el.xmlElements();
            if (elements.size() == 0) {
                return;
            }
            type = elements.get(0).getName();
        }
        String arch = getXml(el, true);
        KvMap values = new KvMap()
            .set("model", model)
            .set("name", name)
            .set("arch", arch)
            .set("mode", mode)
            .set("key", key)
            .set("module_id", moduleId)
            .set("inherit_id", inheritId)
            .set("priority", "primary".equals(mode) ? 0 : Integer.valueOf(priority))
            .set("type", type);

        saveData(record, id, "ir.ui.view", values, false);
    }

    private void loadMenu(Records record, XmlElement el, String packageName, String module, String parentId, String defaultSeq, String menuModel) {
        String id = getXmlId(el, module);
        String parent = el.getAttributeOr("parent", parentId);
        if (Utils.isNotEmpty(parent)) {
            if (!parent.contains(".")) {
                parent = module + "." + parent;
            }
            Records inherit = record.getEnv().getRef(parent);
            parent = inherit.getId();
        }
        String name = el.getAttribute("name");
        String seq = el.getAttributeOr("seq", defaultSeq);
        String url = el.getAttributeOr("url", null);
        String model = el.getAttributeOr("model", null);
        String view = el.getAttributeOr("view", null);
        String icon = el.getAttributeOr("icon", null);
        String target = el.getAttributeOr("target", null);
        String click = el.getAttributeOr("click", null);
        String topmenu = el.getAttributeOr("topmenu", null);
        if (Utils.isBlank(icon)) {
            icon = getIcon(packageName, name);
        }
        KvMap values = new KvMap()
            .set("name", name)
            .set("url", url)
            .set("model", model)
            .set("parent_id", parent)
            .set("icon", icon)
            .set("sequence", Integer.valueOf(seq))
            .set("view", view);
        if (Utils.isNotEmpty(click)) {
            values.put("click", click);
        }
        if (Utils.isNotEmpty(target)) {
            values.put("target", target);
        }
        if (Utils.isNotEmpty(topmenu)) {
            values.put("top_menu", Utils.toBoolean(topmenu));
        }
        saveData(record, id, menuModel, values, true);
        loadChildMenu(record, el, packageName, module, id, menuModel);
    }

    private String getIcon(String packageName, String name) {
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        if (loader != null) {
            String iconName = name.replaceAll("/", "") + ".png";
            String png = packageName.replaceAll("\\.", "/") + "/statics/icons/" + iconName;
            if (testResource(loader, png)) {
                return "/web/" + png;
            }
            png = "org/jmaa/web/statics/icons/menus/" + iconName;
            if (testResource(loader, png)) {
                return "/web/" + png;
            }
        }
        return null;
    }

    private boolean testResource(ClassLoader loader, String path) {
        try (InputStream is = loader.getResourceAsStream(path)) {
            return is != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadChildMenu(Records record, XmlElement el, String packageName, String module, String parentId, String menuModel) {
        Integer seq = 10;
        for (XmlElement child : el.xmlElements()) {
            loadMenu(record, child, packageName, module, parentId, seq.toString(), menuModel);
            seq += 10;
        }
    }

    public static String getXmlId(XmlElement el, String module) {
        String id = el.getAttribute("id");
        if (!id.contains(".")) {
            id = module + "." + id;
        } else {
            if (id.startsWith("@")) {
                id = id.substring(1);
            } else {
                String m = id.split("\\.")[0];
                if (!Utils.equals(m, module)) {
                    throw new ValueException(String.format("XmlId[%s]与模块[%s]不匹配", id, module));
                }
            }
        }
        return id;
    }

    private void loadRecord(Records record, XmlElement el, String module, boolean noUpdate, ImportErrorHandle onError) {
        String id = getXmlId(el, module);
        String model = el.getAttribute("model");
        String noupdate = el.getAttributeOr("noupdate", null);
        noUpdate = Utils.isNotEmpty(noupdate) ? Utils.toBoolean(noupdate) : noUpdate;
        KvMap values = new KvMap();
        for (XmlElement field : el.xmlElements("field")) {
            try {
                Tuple<String, Object> tuple = getField(record, field, module);
                values.put(tuple.getItem1(), tuple.getItem2());
            } catch (Exception ex) {
                ex.printStackTrace();
                handleError(onError, field, ex);
            }
        }
        saveData(record, id, model, values, noUpdate);
    }

    public Records saveData(Records record, String id, String model, Map<String, Object> values, boolean noUpdate) {
        Environment env = record.getEnv();
        Map<String, Object> data = (Map<String, Object>) env.get("ir.model.data").call("findData", id);
        if (data.isEmpty()) {
            Records rec = IrImport.findByValue(env, env.getRegistry().get(model), values);
            if (!rec.any()) {
                rec = rec.create(values);
            }
            String[] parts = id.split("\\.", 2);
            env.get("ir.model.data").create(new KvMap()
                .set("name", parts[1])
                .set("module", parts[0])
                .set("model", model)
                .set("res_id", rec.getId())
                .set("no_update", noUpdate));
            return rec;
        }
        if (!data.get("model").equals(model)) {
            throw new ValueException(String.format("id[%s]原模型[%s]与当前模型[%s]不一致", id, data.get("model"), model));
        }
        if (!Utils.toBoolean(data.get("no_update"), false)) {
            Records rec = env.get((String) data.get("model"), (String) data.get("res_id")).exists();
            if (rec.any()) {
                rec.update(values);
                return rec;
            }
            values.put("id", data.get("res_id"));
            return rec.create(values);
        }
        return env.get((String) data.get("model"), (String) data.get("res_id"));
    }

    private static final Pattern refPattern = Pattern.compile("ref\\((?<ref>\\S+?)\\)");
    private static final Pattern filePattern = Pattern.compile("file\\((?<file>\\S+?)\\)");

    private Object evalValue(String eval) {
        ObjectMapper map = new ObjectMapper();
        try {
            JsonNode node = map.readTree(eval);
            if (node.isBoolean()) {
                return node.asBoolean();
            } else if (node.isDouble()) {
                return node.asDouble();
            } else if (node.isInt()) {
                return node.asInt();
            } else if (node.isNull()) {
                return null;
            } else if (node.isTextual()) {
                return node.asText();
            } else if (node.isArray()) {
                return map.readValue(eval, List.class);
            }
            return map.readValue(eval, Map.class);
        } catch (Exception e) {
            throw new ValueException(String.format("eval的值[%s]解析失败", eval));
        }
    }

    private Tuple<String, Object> getField(Records record, XmlElement field, String module) {
        String name = field.getAttribute("name");
        String eval = field.getAttributeOr("eval", null);
        if (eval != null) {
            Matcher m = refPattern.matcher(eval);
            while (m.find()) {
                String group = m.group();
                String refVal = m.group("ref");
                Records refRec = record.getEnv().getRef(refVal);
                eval = eval.replace(group, "\"" + refRec.getId() + "\"");
            }
            m = filePattern.matcher(eval);
            while (m.find()) {
                String group = m.group();
                String path = m.group("file");
                InputStream input = IoUtils.getResourceAsStream(path);
                byte[] bytes = IoUtils.toByteArray(input);
                String base64data = Base64.getEncoder().encodeToString(bytes);
                String fileName = path.substring(path.lastIndexOf("/") + 1);
                String type = fileName.substring(fileName.lastIndexOf(".") + 1);
                Map<String, Object> file = new HashMap<>();
                file.put("name", fileName);
                file.put("data", base64data);
                file.put("type", type);
                String value = Utils.toJsonString(file);
                eval = eval.replace(group, value);
            }
            Object val = evalValue(eval);
            return new Tuple<>(name, val);
        }
        String ref = field.getAttributeOr("ref", null);
        if (ref != null) {
            if (!ref.contains(".")) {
                ref = module + "." + ref;
            }
            Records refRec = record.getEnv().getRef(ref);
            return new Tuple<>(name, refRec.getId());
        }
        String path = field.getAttributeOr("file", null);
        if (path != null) {
            InputStream input = IoUtils.getResourceAsStream(path);
            byte[] bytes = IoUtils.toByteArray(input);
            String base64data = Base64.getEncoder().encodeToString(bytes);
            String fileName = path.substring(path.lastIndexOf("/") + 1);
            String type = fileName.substring(fileName.lastIndexOf(".") + 1);
            Map<String, Object> file = new HashMap<>();
            file.put("name", fileName);
            file.put("data", base64data);
            file.put("type", type);
            return new Tuple<>(name, file);
        }
        return new Tuple<>(name, field.getText());
    }

    private void handleError(ImportErrorHandle onError, String message, Exception ex) {
        if (onError != null) {
            onError.handle(message, ex);
        }
    }

    private void handleError(ImportErrorHandle onError, XmlElement el, Exception ex) {
        if (onError != null) {
            onError.handle(String.format("解析第[%s]行元素[%s]发生错误：%s", el.getLineNumber(), el.getName(), ThrowableUtils.getCause(ex).getMessage()), ex);
        }
    }
}
