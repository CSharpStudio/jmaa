package org.jmaa.sdk;

/**
 * 默认值
 *
 * @author Eric Liang
 */
public interface Search {
    /**
     * 调用返回默认值
     *
     * @param rec
     * @param op
     * @param value
     * @return
     */
    Criteria call(Records rec, String op, Object value);

    /**
     * 方法返回默认值
     *
     * @param method
     * @return
     */
    static Search method(String method) {
        return new MethodSearch(method);
    }
}

class MethodSearch implements Search {
    String method;

    public MethodSearch(String method) {
        this.method = method;
    }

    @Override
    public Criteria call(Records rec, String op, Object value) {
        return (Criteria) rec.call(method, op, value);
    }
}
