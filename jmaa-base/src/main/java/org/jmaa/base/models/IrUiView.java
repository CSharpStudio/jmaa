package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.exceptions.UserException;
import org.jmaa.sdk.fields.Many2oneField;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.util.KvMap;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI视图，保存xml中声明的界面视图内容。
 * 每个模型提供多种类型的视图，例如查询search、表格grid、表单form等。
 * 每种类型的视图包含一个主视图，多个扩展视图(可选)，读取视图结构时，会把所有扩展视图合并到主视图。
 * <p>
 * key:视图的识别，区分不同场景显示不同的内容
 * priority:控制视图合并的顺序
 *
 * @author Eric Liang
 */
@Model.Meta(name = "ir.ui.view", label = "视图", order = "model, mode desc, priority")
public class IrUiView extends Model {
    final static String RESOURCE = "resource";
    final static String PRIMARY = "primary";
    final static String EXTENSION = "extension";

    static Field name = Field.Char().label("视图名称").required();
    static Field model = Field.Char().label("模型").help("没指定模型的视图直接通过识别码加载").index();
    static Field key = Field.Char().label("识别码").help("用于区分视图组");
    static Field priority = Field.Integer().label("加载顺序").defaultValue(16).required();
    static Field type = Field.Selection().label("视图类型").selection(new Options() {{
        put("grid", "表格");
        put("form", "表单");
        put("card", "卡片");
        put("search", "查询");
        put("lookup", "下拉");
        put("custom", "自定义");
        put("resource", "资源");
        put("web", "页面");
        put("pda", "PDA");
        put("mobile", "移动端");
    }});
    static Field arch = Field.Text().label("视图结构");
    static Field module_id = Field.Many2one("ir.module").label("来源");
    static Field module = Field.Char().related("module_id.name").label("应用");
    static Field inherit_id = Field.Many2one("ir.ui.view").label("继承视图");
    static Field mode = Field.Selection().label("视图继承模式").selection(new Options() {{
        put(PRIMARY, "基础视图");
        put(EXTENSION, "扩展视图");
    }}).defaultValue(PRIMARY).required();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);

    public String loadWeb(Records rec, String key) {
        Records views = rec.find(Criteria.equal("key", key)
            .and("active", "=", true));
        Records primary = null;
        List<Records> extension = new ArrayList<>();
        for (Records view : views) {
            if (primary == null && PRIMARY.equals(view.get("mode"))) {
                primary = view;
            }
            if (EXTENSION.equals(view.get("mode"))) {
                extension.add(view);
            }
        }
        if (primary == null) {
            throw new UserException(rec.l10n("找不到视图:%s", key));
        }
        Document doc = Jsoup.parse((String) primary.get("arch"), Parser.xmlParser());
        Elements base = doc.children();
        for (Records ext : extension) {
            Document arch = Jsoup.parse((String) ext.get("arch"), Parser.xmlParser());
            Elements data = getData(arch);
            combined(base, data);
        }
        if (rec.getEnv().isDebug()) {
            doc.select("[debug=false]").remove();
        } else {
            doc.select("[debug=true]").remove();
        }
        // dom4j无法解析<!DOCTYPE>
        return "<!DOCTYPE html>\r\n" + doc;
    }

    @Model.ServiceMethod(ids = false, doc = "加载属性", auth = Constants.ANONYMOUS)
    public Object loadFields(Records rec, @Doc(doc = "模型名称") String model) {
        KvMap result = new KvMap();
        result.put("fields", getFields(rec.getEnv(), model));
        result.put("present", rec.getEnv().get(model).getMeta().getPresent());
        return result;
    }

    @Model.ServiceMethod(ids = false, doc = "加载lookup视图", auth = Constants.ANONYMOUS)
    @Doc(doc = "视图lookup配置", sample = "{\"model\":\"\",\"fields\":[],\"views\":[]}")
    public Object loadLookup(Records rec, @Doc(doc = "模型名称") String model, @Doc(doc = "识别码") String key) {
        // 视图类型固定
        Records rows = rec.find(Criteria.equal("model", model)
            .and("type", "=", "lookup")
            .and("key", "=", key)
            .and("active", "=", true));
        Optional<Records> primary = rows.stream()
            .filter(v -> PRIMARY.equals(v.get("mode"))).findFirst();
        Map<String, Object> result = new HashMap<>();
        if (primary.isPresent()) {
            List<Records> extension = rows.stream()
                .filter(v -> EXTENSION.equals(v.get("mode")))
                .collect(Collectors.toList());
            Elements base = toElements((String) primary.get().get("arch"), extension, rec.getEnv().isDebug());
            result.put("arch", base.toString());
        }
        MetaModel meta = rec.getEnv().getRegistry().get(model);
        result.put("present", meta.getPresent());
        result.put("fields", getFields(rec.getEnv(), model));
        return result;
    }

    @Model.ServiceMethod(ids = false, doc = "加载视图", auth = Constants.ANONYMOUS)
    @Doc(doc = "视图配置", sample = "{\"model\":\"\",\"fields\":[],\"views\":[]}")
    public Object loadView(Records rec, @Doc(doc = "模型名称") String model, @Doc(doc = "视图类型") String type,
                           @Doc(doc = "视图组") String key) {
        List<String> types = Utils.asList(type.split(","));
        types.add(RESOURCE);
        if (types.contains("grid") || types.contains("card")) {
            types.add("search");
        }
        Records rows = rec.find(Criteria.equal("model", model)
            .and("type", "in", types)
            .and("key", "=", key)
            .and("active", "=", true));
        MetaModel meta = rec.getEnv().getRegistry().get(model);
        KvMap result = new KvMap();
        result.put("model", model);
        result.put("module", meta.getModule());
        result.put("present", meta.getPresent());
        result.put("auths", getAuth(rec, model));
        result.put("fields", getFields(rec.getEnv(), model));
        KvMap views = new KvMap();
        result.put("views", views);
        for (String t : types) {
            if (RESOURCE.equals(t)) {
                String resource = loadResource(rows);
                if (resource != null) {
                    result.put(t, resource);
                }
            } else {
                KvMap view = new KvMap();
                Optional<Records> primary = rows.stream()
                    .filter(v -> t.equals(v.get("type")) && PRIMARY.equals(v.get("mode"))).findFirst();
                if (!primary.isPresent()) {
                    if ("search".equals(t)) {
                        view.put("arch", "");
                        view.put("view_id", "");
                    } else {
                        throw new UserException(rec.l10n("模型[%s]的视图[%s]不存在", model, t));
                    }
                } else {
                    List<Records> extension = rows.stream()
                        .filter(v -> t.equals(v.get("type")) && EXTENSION.equals(v.get("mode")))
                        .collect(Collectors.toList());
                    Elements base = toElements((String) primary.get().get("arch"), extension, rec.getEnv().isDebug());
                    view.put("arch", base.toString());
                    view.put("view_id", primary.get().getId());
                }

                views.put(t, view);
            }
        }
        return result;
    }

    Object getAuth(Records rec, String model) {
        Records per = rec.getEnv().get("rbac.permission");
        List<Object> auth = (List<Object>) per.call("loadModelAuth", model);
        for (String childModel : rec.getEnv().getRegistry().get(model).getAuthChildren()) {
            auth.add(new HashMap<String, Object>() {{
                put(childModel, per.call("loadModelAuth", childModel));
            }});
        }
        return auth;
    }

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Object>> getFields(Environment env, String model) {
        Records rec = env.get(model);
        ObjectMapper m = new ObjectMapper();
        //没有权限的字段
        Set<String> deny = (Set<String>) env.get("rbac.permission").call("loadModelDenyFields", model);
        MetaModel metaModel = rec.getMeta();
        List<String> coModels = metaModel.getFields().values().stream().filter(f -> f instanceof Many2oneField).map(f -> ((Many2oneField) f).getComodel()).collect(Collectors.toList());
        Set<String> allowRead = coModels.isEmpty() ? Collections.emptySet() : (Set<String>) env.get("rbac.permission").call("getAllowReadModels", coModels);
        Set<Entry<String, MetaField>> fields = rec.getMeta().getFields().entrySet();
        Map<String, Map<String, Object>> result = new HashMap<>(fields.size());
        for (Entry<String, MetaField> e : fields) {
            try {
                MetaField field = e.getValue();
                Map<String, Object> data = (Map<String, Object>) m.treeToValue(m.valueToTree(field), Map.class);
                data.put("defaultValue", field.getDefault(rec));
                if (field instanceof SelectionField) {
                    MetaField related = field.getRelatedField();
                    if (related != null) {
                        data.put("options", ((SelectionField) related).getOptions(rec));
                    } else {
                        data.put("options", ((SelectionField) field).getOptions(rec));
                    }
                }
                if (field instanceof Many2oneField) {
                    data.put("link", allowRead.contains(((Many2oneField) field).getComodel()));
                }
                if (deny.contains(field.getName())) {
                    data.put("deny", true);
                }
                result.put(field.getName(), data);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    String loadResource(Records rec) {
        Optional<Records> primary = rec.stream()
            .filter(v -> RESOURCE.equals(v.get("type")) && PRIMARY.equals(v.get("mode"))).findFirst();
        if (!primary.isPresent()) {
            return null;
        }
        List<Records> extension = rec.stream()
            .filter(v -> RESOURCE.equals(v.get("type")) && EXTENSION.equals(v.get("mode")))
            .collect(Collectors.toList());
        Elements base = toElements((String) primary.get().get("arch"), extension, rec.getEnv().isDebug());
        if (base.size() == 1 && RESOURCE.equals(base.get(0).tagName())) {
            return base.html();
        } else {
            return base.toString();
        }
    }

    Elements toElements(String primary, List<Records> extension, boolean isDebug) {
        Document doc = Jsoup.parse("<root>" + primary + "</root>", Parser.xmlParser());
        Elements base = doc.children();
        for (Records ext : extension) {
            Document arch = Jsoup.parse((String) ext.get("arch"), Parser.xmlParser());
            Elements data = getData(arch);
            combined(base, data);
        }
        if (isDebug) {
            base.select("[debug=false]").remove();
        } else {
            base.select("[debug=true]").remove();
        }
        return base.first().children();
    }

    void combined(Elements base, Elements data) {
        for (Element el : data) {
            if ("xpath".equals(el.tagName())) {
                String has = el.attr("has");
                if (Utils.isNotEmpty(has)) {
                    Elements found = base.select(has);
                    if (found.size() == 0) {
                        continue;
                    }
                }
                String hasNot = el.attr("not");
                if (Utils.isNotEmpty(hasNot)) {
                    Elements found = base.select(hasNot);
                    if (found.size() > 0) {
                        continue;
                    }
                }
                String expr = el.attr("expr");
                Elements selects = base.select(expr);
                for (Element select : selects) {
                    Node[] nodes = new Node[el.childNodeSize()];
                    for (int i = 0; i < el.childNodeSize(); i++) {
                        nodes[i] = el.childNode(i).clone();
                    }
                    combined(base, el, select, nodes);
                }
            } else {
                for (Element select : base) {
                    combined(base, el, select, el.clone());
                }
            }
        }
    }

    void combined(Elements base, Element el, Element target, Node... nodes) {
        String position = el.attr("position");
        if (Utils.isEmpty(position)) {
            position = "inside";
        }
        if ("before".equals(position)) {
            for (Node node : nodes) {
                target.before(node);
            }
        } else if ("after".equals(position)) {
            Node current = target;
            for (Node node : nodes) {
                current.after(node);
                current = node;
            }
        } else if ("replace".equals(position)) {
            boolean first = true;
            Node prev = null;
            for (Node node : nodes) {
                if (first) {
                    first = false;
                    target.replaceWith(node);
                    prev = node;
                } else {
                    prev.after(node);
                }
            }
        } else if ("inside".equals(position)) {
            target.appendChildren(Arrays.asList(nodes));
        } else if ("attribute".equals(position)) {
            for (Node node : nodes) {
                if (node instanceof TextNode) {
                    continue;
                }
                target.attributes().addAll(node.attributes());
            }
        } else if ("remove".equals(position)) {
            target.remove();
        } else if ("move".equals(position)) {
            Node moveTo = el.childNode(0);
            String positionMove = moveTo.attr("position");
            String exprMove = moveTo.attr("expr");
            Elements move = base.select(exprMove);
            for (Element select : move) {
                if ("before".equals(positionMove)) {
                    int idx = select.siblingIndex();
                    select.parent().insertChildren(idx - 1, target);
                } else if ("after".equals(positionMove)) {
                    int idx = select.siblingIndex();
                    select.parent().insertChildren(idx + 1, target);
                } else if ("inside".equals(positionMove)) {
                    select.appendChildren(Arrays.asList(target));
                }
            }
        }
    }

    Elements getData(Document doc) {
        for (Element el : doc.children()) {
            if ("data".equals(el.tagName())) {
                return el.children();
            }
        }
        return doc.children();
    }
}
