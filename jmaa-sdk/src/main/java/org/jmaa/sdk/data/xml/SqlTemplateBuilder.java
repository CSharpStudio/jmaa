package org.jmaa.sdk.data.xml;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.data.xml.parsing.PropertyParser;
import org.jmaa.sdk.data.xml.parsing.XPathParser;
import org.jmaa.sdk.data.xml.tags.*;
import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.exceptions.ValueException;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.xml.XmlDocument;
import org.jmaa.sdk.xml.XmlDocumentFactory;
import org.jmaa.sdk.xml.XmlElement;
import org.jmaa.sdk.xml.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class SqlTemplateBuilder {
    private Logger logger = LoggerFactory.getLogger(SqlTemplateBuilder.class);
    static SqlTemplateBuilder builder = new SqlTemplateBuilder();

    public static SqlTemplateBuilder getBuilder() {
        return builder;
    }

    public static void setBuilder(SqlTemplateBuilder builder) {
        SqlTemplateBuilder.builder = builder;
    }

    private final Configuration configuration;

    public SqlTemplateBuilder() {
        this.configuration = new Configuration();
    }

    public SqlTemplateBuilder(Configuration configuration) {
        this.configuration = configuration;
    }

    public void loadTemplates(Registry registry, String packageName, String[] scripts) {
        for (String file : scripts) {
            if (StringUtils.isNoneBlank(file)) {
                String path = PathUtils.combine(packageName.replaceAll("\\.", "/"), file);
                String pathLower = path.toLowerCase();
                if (pathLower.endsWith(".xml")) {
                    InputStream input = IoUtils.getResourceAsStream(path);
                    if (input != null) {
                        XmlReader reader = new XmlReader();
                        reader.setDocumentFactory(new XmlDocumentFactory());
                        XmlDocument doc = reader.readXml(input);
                        XmlElement root = doc.getRootXmlElement();
                        for (XmlElement el : root.xmlElements()) {
                            try {
                                if ("sql".equals(el.getName())) {
                                    loadTemplate(registry, el);
                                }
                            } catch (Exception ex) {
                                throw new PlatformException(String.format("解析[%s]第[%s]行元素[%s]发生错误：%s",
                                    path, el.getLineNumber(), el.getName(), ThrowableUtils.getCause(ex).getMessage()), ex);
                            }
                        }
                    } else {
                        logger.warn(packageName + "找不到script文件:" + file);
                    }
                }
            }
        }
    }

    protected void loadTemplate(Registry registry, XmlElement el) {
        boolean inherit = false;
        String id = el.getAttributeOr("id", null);
        if (Utils.isEmpty(id)) {
            id = el.getAttributeOr("inherit_id", null);
            inherit = true;
        }
        if (Utils.isEmpty(id)) {
            throw new ValueException("id或者inherit_id不能为空");
        }
        final boolean check = !inherit;
        BiConsumer<String, XmlElement> add = (key, node) -> {
            String script = node.getText();
            if (new StringTokenizer(script).hasMoreTokens()) {
                if (check && registry.getTemplates().containsKey(key)) {
                    throw new ValueException(String.format("[id=%s]重复，如果需要覆盖，请使用inherit_id", key));
                }
                FutureTask<SqlTemplate> task = new FutureTask<>(() -> createSqlTemplate(String.format("<script>%s</script>", script)));
                registry.getTemplates().put(key, task);
                task.run();
            }
        };
        add.accept(id, el);
        for (XmlElement node : el.xmlElements()) {
            String key = id + "Ø" + node.getName();
            add.accept(key, node);
        }
    }

    private final static Pattern scriptTagPatten = Pattern.compile("<script", Pattern.CASE_INSENSITIVE);
    private final static Pattern scriptPatten = Pattern.compile("<(trim|where|set|foreach|if|when|otherwise|bind)", Pattern.CASE_INSENSITIVE);

    public SqlTemplate createSqlTemplate(String script) {
        boolean isScript = false;
        if (scriptTagPatten.matcher(script).find()) {
            isScript = true;
        } else if (scriptPatten.matcher(script).find()) {
            isScript = true;
            script = "<script>" + script + "</script>";
        }
        if (isScript) {
            XPathParser parser = new XPathParser(script, false, configuration.getVariables(), new XMLEntityResolver());
            XMLScriptBuilder builder = new XMLScriptBuilder(configuration, parser.evalNode("/script"));
            return builder.parseScriptNode();
        }
        script = PropertyParser.parse(script, configuration.getVariables());
        TextSqlNode textSqlNode = new TextSqlNode(script);
        if (textSqlNode.isDynamic()) {
            return new DynamicSqlTemplate(configuration, textSqlNode);
        } else {
            return new RawSqlTemplate(configuration, script);
        }
    }
}
