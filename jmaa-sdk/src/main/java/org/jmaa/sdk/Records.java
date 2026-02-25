package org.jmaa.sdk;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.tools.ArrayUtils;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.exceptions.ModelException;
import org.jmaa.sdk.exceptions.ValueException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 记录集
 *
 * @author Eric Liang
 */
@JsonSerialize(using = RecordsJsonSerializer.class)
public class Records implements Iterable<Records> {
    private final static Logger logger = LoggerFactory.getLogger(Records.class);
    private String[] ids;
    private Supplier<String[]> prefetchIds;
    private Environment env;
    private MetaModel meta;

    public boolean any() {
        return ids != null && ids.length > 0;
    }

    /**
     * 获取所有id
     */
    public String[] getIds() {
        return ids;
    }

    /**
     * 获取所有id
     */
    public Supplier<String[]> getPrefetchIds() {
        return prefetchIds;
    }

    public int size() {
        return ids.length;
    }

    /**
     * 获取id, 当前记录集必需有且只有一条记录
     */
    public String getId() {
        if (!any()) {
            return null;
        }
        ensureOne();
        return ids[0];
    }

    /**
     * 获取环境参数
     *
     * @return
     */
    public Environment getEnv() {
        return env;
    }

    /**
     * 获取模型元数据
     *
     * @return
     */
    public MetaModel getMeta() {
        return meta;
    }

    public Records(Environment env, MetaModel meta, String[] ids, Supplier<String[]> prefetchIds) {
        this.env = env;
        this.meta = meta;
        this.ids = ids;
        this.prefetchIds = prefetchIds;
    }

    public Records browse(Collection<String> ids) {
        return new Records(env, meta, ids.toArray(ArrayUtils.EMPTY_STRING_ARRAY), null);
    }

    public Records browse(String... ids) {
        return new Records(env, meta, ids, null);
    }

    public Records withPrefetch(Supplier<String[]> prefetchIds) {
        return new Records(env, meta, ids, prefetchIds);
    }

    public void ensureOne() {
        if (ids == null || ids.length != 1) {
            throw new ValueException(String.format("期望单一记录: %s", this));
        }
    }

    public Records withContext(String key, Object value) {
        Map<String, Object> context = new HashMap<>(env.getContext());
        context.put(key, value);
        return withEnv(new Environment(env.getRegistry(), env.getCursor(), env.getUserId(), context));
    }

    public Records withContext(Map<String, Object> ctx) {
        Map<String, Object> context = new HashMap<>(env.getContext());
        context.putAll(ctx);
        return withEnv(new Environment(env.getRegistry(), env.getCursor(), env.getUserId(), context));
    }

    public Records withNewContext(Map<String, Object> ctx) {
        Map<String, Object> context = new HashMap<>(ctx);
        return withEnv(new Environment(env.getRegistry(), env.getCursor(), env.getUserId(), context));
    }

    public Records withEnv(Environment env) {
        return getMeta().browse(env, ids, prefetchIds);
    }

    public Records withUser(String uid) {
        return withEnv(new Environment(env.getRegistry(), env.getCursor(), uid, env.getContext()));
    }

    /**
     * 获取字段值
     *
     * @param field 字段名
     * @return 字段值
     */
    public Object get(String field) {
        MetaField f = getMeta().getField(field);
        return f.get(this);
    }

    /**
     * 获取关系字段（RelationalField）的数据集
     *
     * @param field Many2many/Many2one/One2many类型的字段名
     * @return
     */
    public Records getRec(String field) {
        return (Records) get(field);
    }

    /**
     * 获取关系字段（RelationalField）的数据集
     *
     * @param field Many2many/Many2one/One2many类型的字段名
     * @return
     */
    public Records getRec(Field field) {
        return (Records) get(field);
    }

    /**
     * 获取Char/Text/Selection类型字段的值
     *
     * @param field
     * @return
     */
    public String getString(String field) {
        return (String) get(field);
    }

    /**
     * 获取选择字段(SelectionField)的显示值
     *
     * @param field
     * @return
     */
    public String getSelection(String field) {
        String value = getString(field);
        if (ObjectUtils.isNotEmpty(value)) {
            SelectionField sf = (SelectionField) getMeta().getField(field);
            return sf.getOptions(this).get(value);
        }
        return value;
    }

    /**
     * 获取Char/Text/Selection类型字段的值
     *
     * @param field
     * @return
     */
    public String getString(Field field) {
        return (String) get(field);
    }

    /**
     * 获取Integer类型字段的值
     *
     * @param field
     * @return
     */
    public int getInteger(String field) {
        return ObjectUtils.toInt(get(field));
    }

    /**
     * 获取Integer类型字段的值
     *
     * @param field
     * @return
     */
    public int getInteger(Field field) {
        return getInteger(field.getName());
    }

    public Integer getNullableInteger(String field) {
        return (Integer) get(field);
    }

    public Integer getIntegerNullable(Field field) {
        return (Integer) get(field);
    }

    /**
     * 获取boolean类型字段的值
     *
     * @param field
     * @return
     */
    public boolean getBoolean(Field field) {
        return getBoolean(field.getName());
    }

    /**
     * 获取Boolean类型字段的值，可能返回null
     *
     * @param field
     * @return
     */
    public Boolean getBooleanNullable(Field field) {
        return (Boolean) get(field);
    }

    /**
     * 获取boolean类型字段的值
     *
     * @param field
     * @return
     */
    public boolean getBoolean(String field) {
        return ObjectUtils.toBoolean(get(field));
    }

    /**
     * 获取Boolean类型字段的值，可能返回null
     *
     * @param field
     * @return
     */
    public Boolean getBooleanNullable(String field) {
        return (Boolean) get(field);
    }

    /**
     * 获取Float类型字段的值，null时默认为0
     *
     * @param field
     * @return
     */
    public double getDouble(String field) {
        return ObjectUtils.toDouble(get(field));
    }

    /**
     * 获取Float类型字段的值，可能返回null
     *
     * @param field
     * @return
     */
    public Double getDoubleNullable(String field) {
        return (Double) get(field);
    }

    /**
     * 获取Float类型字段的值
     *
     * @param field
     * @return
     */
    public double getDouble(Field field) {
        return getDouble(field.getName());
    }

    /**
     * 获取Float类型字段的值，可能返回null
     *
     * @param field
     * @return
     */
    public Double getDoubleNullable(Field field) {
        return (Double) get(field);
    }

    /**
     * 获取Date类型字段的值
     *
     * @param field
     * @return
     */
    public Date getDate(String field) {
        return (Date) get(field);
    }

    /**
     * 获取Date类型字段的值
     *
     * @param field
     * @return
     */
    public Date getDate(Field field) {
        return (Date) get(field);
    }

    /**
     * 获取DateTime类型字段的值
     *
     * @param field
     * @return
     */
    public Timestamp getDateTime(String field) {
        return (Timestamp) get(field);
    }

    /**
     * 获取DateTime类型字段的值
     *
     * @param field
     * @return
     */
    public Timestamp getDateTime(Field field) {
        return (Timestamp) get(field);
    }

    /**
     * 获取字段值
     *
     * @param field
     * @return 字段值
     */
    public Object get(Field field) {
        // 不能直接使用field，因为field在setup后已经不是原来的field，需要根据名称重新从元数据中获取field
        return get(field.getName());
    }

    /**
     * 设置字段值
     *
     * @param field 字段名
     * @param value 要设置的值
     */
    public void set(String field, Object value) {
        MetaField f = getMeta().getField(field);
        f.set(this, value);
    }

    /**
     * 设置字段值
     *
     * @param field
     * @param value
     */
    public void set(Field field, Object value) {
        // 不能直接使用field，因为field在setup后已经不是原来的field，需要根据名称重新从元数据中获取field
        set(field.getName(), value);
    }

    /**
     * 获取日志记录实例
     *
     * @return
     */
    public Logger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        int len = getIds().length;
        if (len > 5) {
            return String.format("%s[%s]", meta.getName(), len);
        }
        return String.format("%s[%s]", meta.getName(), Utils.join(getIds()));
    }

    /**
     * 调用模型的方法
     *
     * @param method
     * @param args
     * @return
     */
    public Object call(String method, Object... args) {
        try {
            return meta.invoke(this, method, args);
        } catch (Exception e) {
            throw new ModelException(String.format("模型[%s]调用方法[%s]失败", meta.getName(), method), e);
        }
    }

    /**
     * 调用父模型的方法
     *
     * @param current
     * @param method
     * @param args
     * @return
     */
    public Object callSuper(Class<?> current, String method, Object... args) {
        try {
            return meta.invokeSupper(current, this, method, args);
        } catch (Exception e) {
            throw new ModelException(String.format("模型[%s]调用方法[%s]失败", meta.getName(), method), e);
        }
    }

    /**
     * 本地化翻译
     *
     * @param format
     * @param args
     * @return
     */
    public String l10n(String format, Object... args) {
        return getEnv().l10n(format, args);
    }

    /**
     * 创建
     *
     * @param values
     * @return
     */
    public Records create(Map<String, Object> values) {
        return (Records) call("createBatch", this, Arrays.asList(values));
    }

    /**
     * 批量创建
     *
     * @param valuesList
     * @return
     */
    public Records createBatch(List<Map<String, Object>> valuesList) {
        return (Records) call("createBatch", this, valuesList);
    }

    /**
     * 读取数据
     *
     * @param fields
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> read(Collection<String> fields) {
        return (List<Map<String, Object>>) call("read", this, fields);
    }

    /**
     * 查询数据
     *
     * @param fields
     * @param criteria
     * @param offset
     * @param limit
     * @param order
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(Collection<String> fields, Criteria criteria, Integer offset,
                                            Integer limit, String order) {
        return (List<Map<String, Object>>) call("search", this, fields, criteria, offset, limit, order);
    }

    /**
     * 查询数据
     *
     * @param fields
     * @param criteria
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(Collection<String> fields, Criteria criteria) {
        return (List<Map<String, Object>>) call("search", this, fields, criteria, 0, 0, null);
    }

    /**
     * 查找数据集
     *
     * @param criteria
     * @param offset
     * @param limit
     * @param order
     * @return
     */
    public Records find(Criteria criteria, Integer offset, Integer limit, String order) {
        return (Records) call("find", this, criteria, offset, limit, order);
    }

    /**
     * 查找数据集
     *
     * @param criteria
     * @return
     */
    public Records find(Criteria criteria) {
        return (Records) call("find", this, criteria, 0, 0, null);
    }

    /**
     * 统计数据集数量
     *
     * @param criteria
     * @return
     */
    public long count(Criteria criteria) {
        return (Long) call("count", this, criteria);
    }

    /**
     * 加载数据，不触发更新
     *
     * @param values
     */
    public void load(Map<String, Object> values) {
        call("load", this, values);
    }

    /**
     * 更新记录集
     *
     * @param values
     */
    public void update(Map<String, Object> values) {
        call("update", this, values);
    }

    /**
     * 提交所有变更
     */
    public void flush() {
        call("flush", this);
    }

    /**
     * 获取记录的呈现，[id, present]
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public Object[] getPresent() {
        if (!any()) {
            return null;
        } else {
            ensureOne();
            return ((List<Object[]>) call("getPresent", this)).get(0);
        }
    }

    /**
     * 删除记录
     */
    public void delete() {
        call("delete", this);
    }

    /**
     * 创建查询条件
     *
     * @param criteria
     * @return
     */
    public Criteria criteria(String criteria) {
        return Criteria.parse(criteria);
    }

    /**
     * 创建查询条件
     *
     * @param field
     * @param op
     * @param value
     * @return
     */
    public Criteria criteria(String field, String op, Object value) {
        return Criteria.binary(field, op, value);
    }

    /**
     * 返回当前记录集中存在于数据库的记录
     *
     * @return
     */
    public Records exists() {
        return (Records) call("exists");
    }

    /**
     * 创建流
     *
     * @return
     */
    public Stream<Records> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 过滤符合条件的数据集
     *
     * @param predicate
     */
    public Records filter(Predicate<Records> predicate) {
        String[] ids = stream().filter(predicate).map(p -> p.getId()).toArray(String[]::new);
        return getMeta().browse(getEnv(), ids, prefetchIds);
    }

    /**
     * 返回数据集第一条记录
     *
     * @throws ValueException 数据集为空
     */
    public Records first() {
        if (!any()) {
            throw new ValueException(String.format("数据集为空: %s", this));
        }
        if (ids.length == 1) {
            return this;
        }
        return iterator().next();
    }

    /**
     * 返回数据集第一条记录或者空集合
     */
    public Records firstOrDefault() {
        if (!any()) {
            return this;
        }
        if (ids.length == 1) {
            return this;
        }
        return iterator().next();
    }

    /**
     * 把两个集合合并
     *
     * @param other
     */
    public Records union(Records other) {
        if (!getMeta().getName().equals(other.getMeta().getName())) {
            throw new ValueException(String.format("模型[%s]与模型[%s]不能合并", this, other));
        }
        if (other.any()) {
            Set<String> idSet = Utils.hashSet(this.ids);
            idSet.addAll(Arrays.asList(other.getIds()));
            this.ids = idSet.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        }
        return this;
    }

    /**
     * 是否包含另一个集合所有的id
     *
     * @param other
     */
    public boolean contains(Records other) {
        Set<String> idSet = Utils.hashSet(getIds());
        return idSet.containsAll(Utils.asList(other.getIds()));
    }

    /**
     * 查询分页数据
     *
     * @param fields
     * @param criteria
     * @param offset
     * @param limit
     * @param order
     * @return { hasNext:true, values:[]}
     */
    public Map<String, Object> searchLimit(Collection<String> fields, List<Object> criteria, Integer offset,
                                           Integer limit, String order) {
        Integer size = limit + 1;
        List<Map<String, Object>> data = search(fields, Criteria.parse(criteria), offset, size, order);
        Map<String, Object> result = new HashMap<>(2);
        if (data.size() > limit) {
            data.remove(data.size() - 1);
            result.put("hasNext", true);
        } else {
            result.put("hasNext", false);
        }
        result.put("values", data);
        return result;
    }

    @Override
    public int hashCode() {
        return meta.hashCode() * 31 + ids.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Records) {
            Records other = (Records) obj;
            return other.meta.equals(meta) && SetUtils.hashSet(other.ids).equals(SetUtils.hashSet(ids));
        }
        return false;
    }

    @Override
    public Iterator<Records> iterator() {
        return new RecordsIterator();
    }

    public final int PREFETCH_MAX = 1000;

    class RecordsIterator implements Iterator<Records> {
        int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor < ids.length;
        }

        @Override
        public Records next() {
            if (prefetchIds != null) {
                return getMeta().browse(getEnv(), new String[]{ids[cursor++]}, prefetchIds);
            }
            if (ids.length > PREFETCH_MAX) {
                int idx = cursor / PREFETCH_MAX;
                int from = idx * PREFETCH_MAX;
                return getMeta().browse(getEnv(), new String[]{ids[cursor++]},
                    () -> Arrays.copyOfRange(ids, from, from + PREFETCH_MAX));
            }
            return getMeta().browse(getEnv(), new String[]{ids[cursor++]}, () -> ids);
        }
    }

    public void debug() {
        List<String> fields = new ArrayList<>(getMeta().getFields().keySet());
        List<Map<String, Object>> rows = read(fields);
        System.out.println(getMeta().getName());
        System.out.println(JSONObject.toJSONString(rows, SerializerFeature.PrettyFormat));
    }
}

class RecordsJsonSerializer extends JsonSerializer<Records> {
    @Override
    public void serialize(Records value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        gen.writeObject(value.getIds());
    }
}
