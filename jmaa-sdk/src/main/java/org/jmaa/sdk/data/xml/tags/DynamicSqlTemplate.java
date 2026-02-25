package org.jmaa.sdk.data.xml.tags;

import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.data.xml.Configuration;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.data.xml.parsing.*;

public class DynamicSqlTemplate implements SqlTemplate {
    private final Configuration configuration;
    private final SqlNode rootSqlNode;

    public DynamicSqlTemplate(Configuration configuration, SqlNode rootSqlNode) {
        this.configuration = configuration;
        this.rootSqlNode = rootSqlNode;
    }


    @Override
    public SqlFormat process(Object data) {
        DynamicContext context = new DynamicContext(configuration, data);
        rootSqlNode.apply(context);
        String sql = context.getSql();
        GenericTokenParser parser = new GenericTokenParser("#{", "}",
                content -> {
                    Object value = OgnlCache.getValue(content, context.getBindings());
                    if (value == null) {
                        throw new RuntimeException("Can not found " + content + " value");
                    }
                    context.addParameter(value);
                    return "%s";
                });
        sql = parser.parse(sql);
        return new SqlFormat(sql, context.getParameter());
    }
}
