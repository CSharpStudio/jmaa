package org.jmaa.sdk;

import org.jmaa.sdk.fields.SelectionField;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生成SelectionField的选项值
 *
 * @author Eric Liang
 */
public interface Selection {
    /**
     * 获取选项
     *
     * @param rec
     * @return
     */
    Map<String, String> get(Records rec);

    /**
     * 扩展添加的选项
     *
     * @param toAdd
     */
    void add(Map<String, String> toAdd);

    /**
     * 方法返回选项值
     *
     * @param method
     * @return
     */
    static Selection method(String method) {
        return new MethodSelection(method);
    }

    /**
     * 从关联模型字段取选项值，关联的字段必须是selection类型
     *
     * @param model
     * @param field
     * @return
     */
    static Selection related(String model, String field) {
        return new RelatedSelection(model, field);
    }

    /**
     * 常量选项值
     *
     * @param value
     * @return
     */
    static Selection value(Map<String, String> value) {
        return new StaticSelection(value);
    }

    /**
     * 是否静态值
     *
     * @return
     */
    default boolean isStatic() {
        return false;
    }
}

class RelatedSelection implements Selection {
    String model;
    String field;
    Map<String, String> toAdd = new LinkedHashMap<>(0);

    public RelatedSelection(String model, String field) {
        this.model = model;
        this.field = field;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> get(Records rec) {
        Map<String, String> result = getOptions(rec, model, field);
        result.putAll(toAdd);
        return result;
    }

    @Override
    public void add(Map<String, String> toAdd) {
        this.toAdd.putAll(toAdd);
    }

    Map<String, String> getOptions(Records records, String model, String field) {
        SelectionField sf = (SelectionField) records.getEnv().get(model).getMeta().getField(field);
        return sf.getOptions(records);
    }
}

class MethodSelection implements Selection {
    String method;
    Map<String, String> toAdd = new LinkedHashMap<>(0);

    public MethodSelection(String method) {
        this.method = method;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> get(Records rec) {
        Map<String, String> result = (Map<String, String>) rec.call(method);
        result.putAll(toAdd);
        return result;
    }

    @Override
    public void add(Map<String, String> toAdd) {
        this.toAdd.putAll(toAdd);
    }
}

class StaticSelection implements Selection {
    Map<String, String> selection;
    Map<String, String> toAdd = new LinkedHashMap<>(0);

    public StaticSelection(Map<String, String> selection) {
        this.selection = selection;
    }

    @Override
    public Map<String, String> get(Records rec) {
        Map<String, String> result = new LinkedHashMap<>(selection);
        result.putAll(toAdd);
        return result;
    }

    @Override
    public void add(Map<String, String> toAdd) {
        this.toAdd.putAll(toAdd);
    }

    @Override
    public boolean isStatic() {
        return true;
    }
}
