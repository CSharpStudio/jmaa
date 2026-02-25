package jmaa.modules.report.models;

import org.jmaa.base.models.IrModelDataXmlLoader;
import org.jmaa.base.utils.ImportErrorHandle;
import org.jmaa.sdk.*;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.xml.XmlElement;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model.Meta(inherit = "ir.model.data.xml.loader")
public class ReportXmlLoader extends ValueModel {
    public void loadElement(Records record, XmlElement el, boolean noUpdate, Map<String, Object> context, ImportErrorHandle onError) {
        String module = (String) context.get("moduleName");
        if ("report".equals(el.getName())) {
            loadReport(record, el, module, noUpdate, onError);
        } else if ("dataset".equals(el.getName())) {
            loadDataSet(record, el, module, noUpdate, onError);
        } else {
            callSuper(record, el, noUpdate, context, onError);
        }
    }

    public void loadReport(Records record, XmlElement el, String module, boolean noUpdate, ImportErrorHandle onError) {
        String id = IrModelDataXmlLoader.getXmlId(el, module);
        String noupdate = el.getAttributeOr("noupdate", null);
        noUpdate = Utils.isNotEmpty(noupdate) ? Utils.toBoolean(noupdate) : noUpdate;
        XmlElement menu = el.element("menu");
        String name = el.getAttribute("name");
        KvMap values = new KvMap()
            .set("code", id)
            .set("name", name)
            .set("status", "1");
        if (menu != null) {
            String menuId = createMenu(record, menu, id, name, module);
            values.set("menu_id", menuId);
            el.remove(menu);
        }
        List<Map<String, Object>> datasets = new ArrayList<>();
        for (XmlElement ds : el.xmlElements("dataset")) {
            datasets.add(createDataSet(record, ds));
            el.remove(ds);
        }
        String content = IrModelDataXmlLoader.getXml(el, true);
        values.set("content", content);
        Records report = (Records) record.call("saveData", id, "rpt.report", values, noUpdate);
        for (Map<String, Object> row : datasets) {
            row.put("report_id", report.getId());
        }
        record.getEnv().get("rpt.dataset").call("createOrUpdate", datasets);
    }

    public Map<String, Object> createDataSet(Records record, XmlElement ds) {
        String common = null;
        String dialectSql = null;
        String dialect = record.getEnv().getCursor().getSqlDialect().getName();
        for (XmlElement sql : ds.xmlElements("sql")) {
            String d = sql.getAttributeOr("dialect", null);
            if (Utils.isEmpty(d)) {
                common = asXML(sql);
            } else if (Utils.equals(dialect, d)) {
                dialectSql = asXML(sql);
            }
        }
        KvMap map = new KvMap()
            .set("code", ds.getAttribute("id"))
            .set("name", ds.getAttributeOr("name", null))
            .set("content", Utils.isNotEmpty(dialectSql) ? dialectSql : common);
        return map;
    }

    public String createMenu(Records record, XmlElement menu, String id, String name, String module) {
        String menuId = menu.getAttributeOr("id", id + "#menu");
        if (!menuId.contains(".")) {
            menuId = module + "." + menuId;
        }
        String parent = menu.getAttributeOr("parent", null);
        if (Utils.isNotEmpty(parent)) {
            if (!parent.contains(".")) {
                parent = module + "." + parent;
            }
            Records inherit = record.getEnv().getRef(parent);
            parent = inherit.getId();
        }
        String menuName = menu.getAttributeOr("name", name);
        String seq = menu.getAttributeOr("seq", "0");
        String icon = menu.getAttributeOr("icon", null);
        String target = menu.getAttributeOr("target", null);
        if (Utils.isBlank(icon)) {
            icon = "/web/jmaa/modules/report/statics/menu.png";
        }
        KvMap menuValues = new KvMap()
            .set("name", menuName)
            .set("url", "/{tenant}/report#" + id)
            .set("parent_id", parent)
            .set("icon", icon)
            .set("sequence", Integer.valueOf(seq));
        if (Utils.isNotEmpty(target)) {
            menuValues.put("target", target);
        }
        Records m = (Records) record.call("saveData", menuId, "ir.ui.menu", menuValues, true);
        return m.getId();
    }

    private void loadDataSet(Records record, XmlElement el, String module, boolean noUpdate, ImportErrorHandle onError) {
        String id = IrModelDataXmlLoader.getXmlId(el, module);
        String noupdate = el.getAttributeOr("noupdate", null);
        noUpdate = Utils.isNotEmpty(noupdate) ? Utils.toBoolean(noupdate) : noUpdate;
        String dialect = record.getEnv().getCursor().getSqlDialect().getName();
        String common = null;
        String dialectSql = null;
        for (XmlElement sql : el.xmlElements("sql")) {
            String d = sql.getAttributeOr("dialect", null);
            if (Utils.isEmpty(d)) {
                common = asXML(sql);
            } else if (Utils.equals(dialect, d)) {
                dialectSql = asXML(sql);
            }
        }
        KvMap values = new KvMap();
        values.set("name", el.getAttributeOr("name", id))
            .set("content", Utils.isNotEmpty(dialectSql) ? dialectSql : common);
        record.call("saveData", id, "rpt.dataset", values, noUpdate);
    }

    private String asXML(XmlElement el) {
        String arch = "";
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        format.setIndent("  ");
        for (int i = 0; i < el.nodeCount(); i++) {
            Node node = el.node(i);
            arch += asXML(node, format);
        }
        arch = arch.trim();
        if (arch.startsWith("\n")) {
            arch = arch.substring(1);
        }
        return arch;
    }

    private String asXML(Node node, OutputFormat format) {
        try {
            StringWriter out = new StringWriter();
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(node);
            writer.flush();
            return out.toString();
        } catch (IOException var3) {
            throw new RuntimeException("IOException while generating textual representation: " + var3.getMessage());
        }
    }
}
