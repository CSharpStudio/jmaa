package org.jmaa.sdk.fields;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.sql.Timestamp;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;

import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.tools.*;

/**
 * 字段基类
 *
 * @author Eric Liang
 */
@SuppressWarnings("unchecked")
public class BaseField<T extends BaseField<T>> extends MetaField {
    static {
        Field.registerField(Constants.OBJECT, ObjectField.class, byte[].class);
        Field.registerField(Constants.BINARY, BinaryField.class, byte[].class);
        Field.registerField(Constants.BOOLEAN, BooleanField.class, Boolean.class);
        Field.registerField(Constants.CHAR, CharField.class, String.class);
        Field.registerField(Constants.DATE, DateField.class, java.sql.Date.class);
        Field.registerField(Constants.DATETIME, DateTimeField.class, Timestamp.class);
        Field.registerField(Constants.FLOAT, FloatField.class, Double.class);
        Field.registerField(Constants.HTML, HtmlField.class, String.class);
        Field.registerField(Constants.IMAGE, ImageField.class, byte[].class);
        Field.registerField(Constants.INTEGER, IntegerField.class, Integer.class);
        Field.registerField(Constants.MANY2MANY, Many2manyField.class, Records.class);
        Field.registerField(Constants.MANY2ONE, Many2oneField.class, Records.class);
        Field.registerField(Constants.ONE2MANY, One2manyField.class, Records.class);
        Field.registerField(Constants.SELECTION, SelectionField.class, String.class);
        Field.registerField(Constants.TEXT, TextField.class, String.class);
        Field.registerField(Constants.MANY2ONE_REFERENCE, Many2oneReferenceField.class, String.class);
    }

    /**
     * 存于数据库
     */
    public T store() {
        args.put(Constants.STORE, true);
        return (T) this;
    }

    /**
     * 是否存于数据库
     */
    public T store(boolean store) {
        args.put(Constants.STORE, store);
        return (T) this;
    }

    public T tracking() {
        args.put(Constants.TRACKING, true);
        return (T) this;
    }

    public T tracking(boolean tracking) {
        args.put(Constants.TRACKING, tracking);
        return (T) this;
    }

    /**
     * 数据库索引
     */
    public T index() {
        args.put(Constants.INDEX, true);
        return (T) this;
    }

    /**
     * 是否数据库索引
     */
    public T index(boolean index) {
        args.put(Constants.INDEX, index);
        return (T) this;
    }

    /**
     * 数据库唯一索引
     */
    public T unique() {
        args.put(Constants.UNIQUE, Constants.UNIQUE_MODE.Update);
        return (T) this;
    }

    /**
     * 是否数据库唯一索引
     */
    public T unique(boolean unique) {
        args.put(Constants.UNIQUE, unique ? Constants.UNIQUE_MODE.Unique : Constants.UNIQUE_MODE.None);
        return (T) this;
    }

    /**
     * 数据库唯一索引
     */
    public T unique(Constants.UNIQUE_MODE mode) {
        args.put(Constants.UNIQUE, mode);
        return (T) this;
    }

    /**
     * 复制模型时，复制字段
     */
    public T copy() {
        args.put(Constants.COPY, true);
        return (T) this;
    }

    /**
     * 复制模型时，是否复制字段
     */
    public T copy(boolean copy) {
        args.put(Constants.COPY, copy);
        return (T) this;
    }

    /**
     * 依赖的其它字段
     */
    public T depends(String... depends) {
        args.put(Constants.DEPENDS, Arrays.asList(depends));
        return (T) this;
    }

    /**
     * 指定计算方法，模型方法或者groovy脚本，示例:
     *
     * <pre>
     * static Field area = Field.Float().compute(Callable.method("areaCompute")).label("面积");
     * or
     * static Field area = Field.Float().compute(Callable.script("r->(double)r.get('height') * (double)r.get('width')")).label("面积");
     * </pre>
     */
    public T compute(Callable compute) {
        args.put(Constants.COMPUTE, compute);
        return (T) this;
    }

    public T compute(Callable compute, Callable inverse) {
        args.put(Constants.COMPUTE, compute);
        args.put(Constants.INVERSE, inverse);
        return (T) this;
    }

    /**
     * 指定计算方法, 方法示例:
     *
     * <pre>
     * public Object areaCompute(Records rec) {
     *     return (double) rec.get("height") * (double) rec.get("width");
     * }
     * </pre>
     */
    public T compute(String compute) {
        args.put(Constants.COMPUTE, Callable.method(compute));
        return (T) this;
    }

    public T compute(String compute, String inverse) {
        args.put(Constants.COMPUTE, Callable.method(compute));
        args.put(Constants.INVERSE, Callable.method(inverse));
        return (T) this;
    }

    /**
     * 指定查询条件处理方法, 只对store(false)或计算字段有效
     * 方法示例:
     *
     * <pre>
     * public Criteria codeSearch(Records rec, String op, Object value) {
     *     return Criteria.binary("code", op, value);
     * }
     * </pre>
     */
    public T search(String method) {
        args.put(Constants.SEARCH, Search.method(method));
        return (T) this;
    }

    /**
     * 标题
     */
    public T label(String caption) {
        args.put(Constants.LABEL, caption);
        return (T) this;
    }

    /**
     * 帮助说明
     */
    public T help(String help) {
        args.put(Constants.HELP, help);
        return (T) this;
    }

    /**
     * 只读
     */
    public T readonly() {
        args.put(Constants.READONLY, true);
        return (T) this;
    }

    /**
     * 是否只读
     */
    public T readonly(boolean readonly) {
        args.put(Constants.READONLY, readonly);
        return (T) this;
    }

    /**
     * 必填
     */
    public T required() {
        args.put(Constants.REQUIRED, true);
        return (T) this;
    }

    /**
     * 是否必填
     */
    public T required(boolean required) {
        args.put(Constants.REQUIRED, required);
        return (T) this;
    }

    /**
     * 是否自动，默认自动字段:id, create_uid, create_data, update_uid, update_date
     */
    public T automatic(boolean automatic) {
        args.put(Constants.AUTOMATIC, automatic);
        return (T) this;
    }

    /**
     * 上下文
     */
    public T context(String key, Object value) {
        Map<String, Object> ctx = new HashMap<>(1);
        ctx.put(key, value);
        args.put("context", ctx);
        return (T) this;
    }

    /**
     * 上下文
     */
    public T context(Map<String, Object> ctx) {
        args.put("context", ctx);
        return (T) this;
    }

    /**
     * 默认值
     */
    public T defaultValue(Default defaultValue) {
        args.put("defaultValue", defaultValue);
        return (T) this;
    }

    /**
     * 默认值
     */
    public T defaultValue(Object defaultValue) {
        if (defaultValue instanceof Default) {
            args.put("defaultValue", defaultValue);
        } else {
            if (defaultValue instanceof SelectionValue) {
                defaultValue = defaultValue.toString();
            }
            args.put("defaultValue", Default.value(defaultValue));
        }
        return (T) this;
    }

    /**
     * 关联
     */
    public T related(String related) {
        if (StringUtils.isNotBlank(related)) {
            args.put("related", related.split("\\."));
        }
        return (T) this;
    }

    /**
     * 需要授权
     */
    public T auth() {
        args.put("auth", true);
        return (T) this;
    }

    /**
     * 是否需要授权
     */
    public T auth(boolean auth) {
        args.put("auth", auth);
        return (T) this;
    }

    /**
     * 是否预加载
     */
    public T prefetch(boolean prefetch) {
        args.put("prefetch", prefetch);
        return (T) this;
    }
}
