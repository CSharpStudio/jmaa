package org.jmaa.sdk.fields;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.jmaa.sdk.Criteria;
import org.jmaa.sdk.ICriteria;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.MetaModel;

/**
 * 关联
 *
 * @author Eric Liang
 */
public class RelationalField<T extends BaseField<T>> extends BaseField<T> {

    @JsonIgnore
    ICriteria criteria;
    @JsonIgnore
    Boolean autoJoin = false;
    @Related
    Map<String, Object> context;
    /**
     * corresponding model name
     */
    @Related
    String comodelName;

    /**
     * 获取上下文
     *
     * @return
     */
    public Map<String, Object> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }
        return context;
    }

    /**
     * 指定上下文
     *
     * @param ctx
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public T context(Map<String, Object> ctx) {
        args.put("context", ctx);
        return (T) this;
    }

    /**
     * 是否使用Join查询
     *
     * @return
     */
    public Boolean getAutoJoin() {
        return autoJoin;
    }

    /**
     * 关联的模型
     *
     * @return
     */
    public String getComodel() {
        return comodelName;
    }

    /**
     * 使用join查询
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public T autoJoin() {
        args.put("autoJoin", true);
        return (T) this;
    }

    /**
     * 指定是否使用join查询，否则查询两次，先查询关联的id，再使用in查询
     *
     * @param autoJoin
     * @return
     */
    @SuppressWarnings("unchecked")
    public T autoJoin(boolean autoJoin) {
        args.put("autoJoin", autoJoin);
        return (T) this;
    }

    /**
     * 指定关联字段数据源的查询条件，跟criteria效果一样
     *
     * @param lookup
     * @return
     */
    @SuppressWarnings("unchecked")
    public T lookup(ICriteria lookup) {
        args.put(Constants.CRITERIA, lookup);
        return (T) this;
    }

    /**
     * 指定关联字段数据源的查询条件，跟criteria效果一样
     *
     * @param method
     * @return
     */
    @SuppressWarnings("unchecked")
    public T lookup(String method) {
        args.put(Constants.CRITERIA, ICriteria.method(method));
        return (T) this;
    }

    /**
     * 指定关联字段数据源的查询条件，跟criteria效果一样
     *
     * @param lookup
     * @return
     */
    @SuppressWarnings("unchecked")
    public T lookup(Criteria lookup) {
        args.put(Constants.CRITERIA, ICriteria.criteria(lookup));
        return (T) this;
    }

    /**
     * 获取关联字段数据源的查询条件
     *
     * @param model
     * @return
     */
    public Criteria getCriteria(Records model) {
        if (criteria != null) {
            return criteria.get(model);
        }
        return new Criteria();
    }

    @Override
    protected void setupNonRelated(MetaModel model) {
        super.setupNonRelated(model);
        if (!model.getRegistry().contains(comodelName)) {
            logger.warn("字段[{}]使用了未知引用模型{}", this, comodelName);
            comodelName = "_unknown";
        }
    }
}
