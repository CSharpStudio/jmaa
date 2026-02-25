package org.jmaa.sdk;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.jmaa.sdk.exceptions.CallException;

import groovy.lang.Closure;

import java.util.List;

/**
 * {@link Criteria}提供者
 *
 * @author Eric Liang
 */
public interface ICriteria {
    /**
     * 根据记录动态获取查询过滤
     *
     * @param rec
     * @return
     */
    Criteria get(Records rec);

    /**
     * 过滤方法, 动态生成过滤条件。
     *
     * @param method 指定模型中定义的方法，参数{@link Records}, 返回值{@link Criteria}
     * @return
     */
    static ICriteria method(String method) {
        return new MethodCriteria(method);
    }

    /**
     * 查询过滤，直接指定过滤的条件
     *
     * @param criteria
     * @return
     */
    static ICriteria criteria(Criteria criteria) {
        return new StaticCriteria(criteria);
    }

    /**
     * 查询脚本。
     * <p>
     * {@code r->r.criteria('create_uid','=',r.getEnv().getUserId())}
     * </p>
     *
     * @param script
     * @return
     */
    static ICriteria script(String script) {
        return new ScriptCriteria(script);
    }
}

class ScriptCriteria implements ICriteria {

    String script;
    Closure<?> closure;

    public ScriptCriteria(String script) {
        this.script = "{" + script + "}";
    }

    Closure<?> getClosure() {
        if (closure == null) {
            try {
                ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
                closure = (Closure<?>) engine.eval(script, new SimpleBindings());
            } catch (Exception e) {
                throw new CallException(String.format("脚本[%s]执行失败", script), e);
            }
        }
        return closure;
    }

    @Override
    public Criteria get(Records rec) {
        return (Criteria) getClosure().call(rec);
    }
}

class MethodCriteria implements ICriteria {

    String method;

    public MethodCriteria(String method) {
        this.method = method;
    }

    @Override
    public Criteria get(Records rec) {
        return (Criteria) rec.call(method);
    }
}

class StaticCriteria implements ICriteria {
    Criteria criteria;

    public StaticCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Criteria get(Records rec) {
        return Criteria.parse((List<Object>) criteria.clone());
    }
}
