package org.jmaa.sdk.data.xml.tags;

import org.jmaa.sdk.exceptions.ValueException;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluator {
    public static final ExpressionEvaluator INSTANCE = new ExpressionEvaluator();

    public boolean evaluateBoolean(String expression, Object parameterObject) {
        if (expression.endsWith("?")) {
            expression = "@org.jmaa.sdk.Utils@isNotEmpty(" + expression.substring(0, expression.length() - 1) + ")";
        }
        Object value = OgnlCache.getValue(expression, parameterObject);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
        }
        return value != null;
    }

    /**
     *
     */
    public Iterable<?> evaluateIterable(String expression, Object parameterObject, boolean nullable) {
        Object value = OgnlCache.getValue(expression, parameterObject);
        if (value == null) {
            if (nullable) {
                return null;
            }
            throw new ValueException("The expression '" + expression + "' evaluated to a null value.");
        }
        if (value instanceof Iterable) {
            return (Iterable<?>) value;
        }
        if (value.getClass().isArray()) {
            // the array may be primitive, so Arrays.asList() may throw
            // a ClassCastException (issue 209). Do the work manually
            // Curse primitives! :) (JGB)
            int size = Array.getLength(value);
            List<Object> answer = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Object o = Array.get(value, i);
                answer.add(o);
            }
            return answer;
        }
        if (value instanceof Map) {
            return ((Map) value).entrySet();
        }
        throw new ValueException("Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
    }
}
