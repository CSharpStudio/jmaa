package org.jmaa.sdk.data.xml.tags;

import ognl.*;

import java.io.StringReader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.ReflectPermission;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OgnlCache {
    private static final OgnlMemberAccess MEMBER_ACCESS = new OgnlMemberAccess();
    private static final OgnlClassResolver CLASS_RESOLVER = new OgnlClassResolver();
    private static final Map<String, Node> expressionCache = new ConcurrentHashMap<String, Node>();

    public static Object getValue(String expression, Object root) {
        try {
            OgnlContext context = Ognl.createDefaultContext(root, MEMBER_ACCESS, CLASS_RESOLVER, null);
            return Ognl.getValue(parseExpression(expression), context, root);
        } catch (OgnlException e) {
            throw new RuntimeException("Error evaluating expression '"
                + expression + "'. Cause: " + e, e);
        }
    }

    private static Object parseExpression(String expression)
        throws OgnlException {
        try {
            Node node = expressionCache.get(expression);
            if (node == null) {
                node = new OgnlParser(new StringReader(expression))
                    .topLevelExpression();
                expressionCache.put(expression, node);
            }
            return node;
        } catch (ParseException e) {
            throw new ExpressionSyntaxException(expression, e);
        } catch (TokenMgrError e) {
            throw new ExpressionSyntaxException(expression, e);
        }
    }

    static class OgnlClassResolver extends DefaultClassResolver {

        @Override
        protected Class toClassForName(String className) throws ClassNotFoundException {
            if ("org.jmaa.sdk.Utils".equals(className)) {
                return org.jmaa.sdk.Utils.class;
            }
            //限制不能创建类，防止注入
            return null;
        }

    }

    static class OgnlMemberAccess implements MemberAccess {

        private final boolean canControlMemberAccessible;

        OgnlMemberAccess() {
            this.canControlMemberAccessible = canControlMemberAccessible();
        }

        public Object setup(OgnlContext context, Object target, Member member, String propertyName) {
            Object result = null;
            if (isAccessible(context, target, member, propertyName)) {
                AccessibleObject accessible = (AccessibleObject) member;
                if (!accessible.isAccessible()) {
                    result = Boolean.FALSE;
                    accessible.setAccessible(true);
                }
            }
            return result;
        }

        public void restore(OgnlContext context, Object target, Member member, String propertyName, Object state) {
            // Flipping accessible flag is not thread safe. See #1648
        }

        public boolean isAccessible(OgnlContext context, Object target, Member member, String propertyName) {
            return canControlMemberAccessible;
        }

        public static boolean canControlMemberAccessible() {
            try {
                SecurityManager securityManager = System.getSecurityManager();
                if (null != securityManager) {
                    securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
                }
            } catch (SecurityException e) {
                return false;
            }
            return true;
        }
    }
}
