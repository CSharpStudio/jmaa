package org.jmaa.sdk.data.xml.tags;

public class IfSqlNode implements SqlNode {
    private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;
    private final String test;
    private final SqlNode contents;

    public IfSqlNode(SqlNode contents, String test) {
        this.test = test;
        this.contents = contents;
    }

    @Override
    public boolean apply(DynamicContext context) {
        if (evaluator.evaluateBoolean(test, context.getBindings())) {
            contents.apply(context);
            return true;
        }
        return false;
    }
}
