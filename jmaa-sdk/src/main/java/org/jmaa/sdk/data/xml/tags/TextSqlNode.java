package org.jmaa.sdk.data.xml.tags;

import org.jmaa.sdk.data.xml.parsing.GenericTokenParser;
import org.jmaa.sdk.data.xml.parsing.TokenHandler;
import org.jmaa.sdk.exceptions.ValueException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class TextSqlNode implements SqlNode {
    private final String text;
    private final Pattern injectionFilter;

    public TextSqlNode(String text) {
        this(text, null);
    }

    public TextSqlNode(String text, Pattern injectionFilter) {
        this.text = text;
        this.injectionFilter = injectionFilter;
    }

    public boolean isDynamic() {
        DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
        GenericTokenParser parser = createParser(checker);
        parser.parse(text);
        return checker.isDynamic();
    }

    @Override
    public boolean apply(DynamicContext context) {
        GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
        context.appendSql(parser.parse(text));
        return true;
    }

    private GenericTokenParser createParser(TokenHandler handler) {
        return new GenericTokenParser("${", "}", handler);
    }

    private static class BindingTokenParser implements TokenHandler {

        private final DynamicContext context;
        private final Pattern injectionFilter;

        public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
            this.context = context;
            this.injectionFilter = injectionFilter;
        }

        @Override
        public String handleToken(String content) {
            Object parameter = context.getBindings().get("_parameter");
            if (parameter == null) {
                context.getBindings().put("value", null);
            } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
                context.getBindings().put("value", parameter);
            }
            Object value = OgnlCache.getValue(content, context.getBindings());
            String srtValue = value == null ? "" : String.valueOf(value); // issue #274 return "" instead of "null"
            checkInjection(srtValue);
            return srtValue;
        }

        private void checkInjection(String value) {
            if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
                throw new ValueException("Invalid input. Please conform to regex" + injectionFilter.pattern());
            }
        }
    }

    private static class DynamicCheckerTokenParser implements TokenHandler {

        private boolean isDynamic;

        public DynamicCheckerTokenParser() {
            // Prevent Synthetic Access
        }

        public boolean isDynamic() {
            return isDynamic;
        }

        @Override
        public String handleToken(String content) {
            this.isDynamic = true;
            return null;
        }
    }

    static class SimpleTypeRegistry {

        private static final Set<Class<?>> SIMPLE_TYPE_SET = new HashSet<>();

        static {
            SIMPLE_TYPE_SET.add(String.class);
            SIMPLE_TYPE_SET.add(Byte.class);
            SIMPLE_TYPE_SET.add(Short.class);
            SIMPLE_TYPE_SET.add(Character.class);
            SIMPLE_TYPE_SET.add(Integer.class);
            SIMPLE_TYPE_SET.add(Long.class);
            SIMPLE_TYPE_SET.add(Float.class);
            SIMPLE_TYPE_SET.add(Double.class);
            SIMPLE_TYPE_SET.add(Boolean.class);
            SIMPLE_TYPE_SET.add(Date.class);
            SIMPLE_TYPE_SET.add(Class.class);
            SIMPLE_TYPE_SET.add(BigInteger.class);
            SIMPLE_TYPE_SET.add(BigDecimal.class);
        }
        /*
         * Tells us if the class passed in is a known common type
         * @param clazz The class to check
         * @return True if the class is known
         */
        public static boolean isSimpleType(Class<?> clazz) {
            return SIMPLE_TYPE_SET.contains(clazz);
        }
    }
}
