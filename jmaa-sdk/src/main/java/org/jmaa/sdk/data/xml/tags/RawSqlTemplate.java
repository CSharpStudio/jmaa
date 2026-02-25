package org.jmaa.sdk.data.xml.tags;

import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.data.xml.Configuration;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.data.xml.parsing.GenericTokenParser;

import java.util.List;

public class RawSqlTemplate implements SqlTemplate {
    private final String sql;
    private final Configuration configuration;

    public RawSqlTemplate(Configuration configuration, SqlNode rootSqlNode) {
        this.configuration = configuration;
        this.sql = getSql(rootSqlNode);
    }

    public RawSqlTemplate(Configuration configuration, String sql) {
        this.configuration = configuration;
        this.sql = sql;
    }

    private String getSql(SqlNode rootSqlNode) {
        DynamicContext context = new DynamicContext(configuration, null);
        rootSqlNode.apply(context);
        return context.getSql();
    }

    @Override
    public SqlFormat process(Object data) {
        DynamicContext context = new DynamicContext(configuration, data);
        GenericTokenParser parser = new GenericTokenParser("#{", "}",
                content -> {
                    Object value = OgnlCache.getValue(content, context.getBindings());
                    if (value == null) {
                        throw new RuntimeException("Can not found " + content + " value");
                    }
                    context.addParameter(value);
                    return "?";
                });
        String statement = parser.parse(sql);
        return new SqlFormat(statement, context.getParameter());
    }
}
