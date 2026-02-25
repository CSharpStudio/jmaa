package org.jmaa.sdk.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.DbColumn;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.exceptions.TypeException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.fields.RelationalField;
import org.jmaa.sdk.util.Cache;
import org.jmaa.sdk.util.ToUpdate;
import org.jmaa.sdk.util.Tuple;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.Callable;
import org.jmaa.sdk.Default;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Search;
import org.jmaa.sdk.exceptions.ModelException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The field descriptor contains the field definition, and manages accesses
 * and assignments of the corresponding field on records.
 *
 * @author Eric Liang
 */
public class MetaField implements org.jmaa.sdk.Field {

    protected Logger logger = LoggerFactory.getLogger(MetaField.class);

    static int globalSeq = 0;
    boolean setupDone;

    String name;
    protected String type;
    @Related
    protected String label;
    @Related
    String help;
    protected boolean readonly;
    boolean store = true;
    boolean required;
    @JsonIgnore
    Constants.UNIQUE_MODE unique;
    @JsonIgnore
    int sequence;
    @JsonIgnore
    protected HashMap<String, Object> args = new HashMap<>();
    @JsonIgnore
    protected ColumnType columnType = ColumnType.None;
    @JsonIgnore
    protected boolean prefetch = true;
    @JsonIgnore
    MetaField relatedField;
    @JsonIgnore
    boolean automatic;
    @JsonIgnore
    boolean index;
    @JsonIgnore
    boolean copy = true;
    @JsonIgnore
    boolean manual;
    @JsonIgnore
    String modelName;
    @JsonIgnore
    Callable compute;
    @JsonIgnore
    Callable inverse;
    @JsonIgnore
    Search search;
    @JsonIgnore
    Default defaultValue;
    @JsonIgnore
    Collection<String> depends;
    @JsonIgnore
    String[] related;
    @JsonIgnore
    boolean auth;
    @JsonIgnore
    String module;
    @JsonIgnore
    boolean inherited;
    @JsonIgnore
    MetaField inheritedField;
    @JsonIgnore
    boolean tracking;
    protected boolean sortable = true;

    /**
     * 获取定义字段的模块
     *
     * @return
     */
    public String getModule() {
        return module;
    }

    /**
     * 设置字段的模块
     */
    public MetaField setModule(String module) {
        this.module = module;
        return this;
    }

    /**
     * 构造实例
     */
    public MetaField() {
        sequence = globalSeq++;
    }

    /**
     * 根据当前类型创建新实例
     */
    public MetaField newInstance() {
        try {
            MetaField field = getClass().getConstructor().newInstance();
            field.args = new HashMap<>(args);
            field.module = module;
            return field;
        } catch (Exception e) {
            throw new TypeException("创建MetaField失败:" + getClass().getName());
        }
    }

    /**
     * 获取默认值
     */
    public Object getDefault(Records rec) {
        if (defaultValue != null) {
            return defaultValue.call(rec);
        }
        return null;
    }

    /**
     * 是否可以排序
     */
    public boolean isSortable() {
        return sortable;
    }

    /**
     * 是否需要授权
     */
    public boolean isAuth() {
        return auth;
    }

    /**
     * 是否自动注入的字段（如create_uid,create_date,update_uid,update_date）
     */
    @JsonIgnore
    public boolean isAuto() {
        return automatic;
    }

    /**
     * 设置参数
     */
    public void setArgs(Map<String, Object> attrs) {
        args.putAll(attrs);
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    /**
     * 设置字段名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取字段名
     */
    public String getName() {
        return name;
    }

    /**
     * 是否自定义的字段
     */
    public boolean isManual() {
        return manual;
    }

    /**
     * 获取关联字段，如 user_id.company_id.name 返回 ["user_id", "company_id", "name"]
     */
    public String[] getRelated() {
        if (related == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return related;
    }

    /**
     * 相应的关联字段，如 user_id.company_id.name 返回 res.company 的 name 字段
     */
    public MetaField getRelatedField() {
        return relatedField;
    }

    /**
     * 如果是委托继承的字段，返回被继承的字段，否则返回当前字段
     */
    @JsonIgnore
    public MetaField getBaseField() {
        return inherited ? inheritedField.getBaseField() : this;
    }

    /**
     * 获取模型名称
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 获取用户可见的字段标题，如果没设置，返回字段的名称
     */
    public String getLabel() {
        if (StringUtils.isEmpty(label)) {
            return name;
        }
        return label;
    }

    /**
     * 是否保存到数据库，默认是 true，计算字段是 false
     */
    public boolean isStore() {
        return store;
    }

    /**
     * 是否继承
     */
    public boolean isInherited() {
        return inherited;
    }

    /**
     * 用户可见的帮助信息
     */
    public String getHelp() {
        return help;
    }

    /**
     * 是否只读
     */
    public boolean isReadonly() {
        return readonly;
    }

    /**
     * 字段的值是否必填 (默认: false)
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 获取字段的计算方法 compute(recs)
     */
    public Callable getCompute() {
        return compute;
    }

    /**
     * search(recs, operator, value) searches on self
     */
    public Search getSearch() {
        return search;
    }

    /**
     * 是否数据库索引字段. 注意: 对不影射数据库的字段无效，默认值 false
     */
    public boolean isIndex() {
        return index;
    }

    /**
     * return whether the field value should be copied when the record
     * is duplicated (default: true for normal fields, false for
     * one2many and computed fields, including property fields and
     * related fields)
     */
    public boolean isCopy() {
        return copy;
    }

    public boolean isTracking() {
        return tracking;
    }

    /**
     * return type of the field
     */
    public String getType() {
        return type;
    }

    /**
     * return column type that can be converted to database column type with
     * {@link SqlDialect}
     */
    public ColumnType getColumnType() {
        return columnType;
    }

    public int getSequence() {
        return sequence;
    }

    /**
     * return database column type
     */
    public String getDbColumnType(SqlDialect sqlDialect) {
        return sqlDialect.getColumnType(getColumnType());
    }

    protected void setName(MetaModel model, String name) {
        if (StringUtils.hasUpperCase(name)) {
            throw new ModelException(String.format("模型[%s]字段[%s]不能包含大写字母", model.getName(), name));
        }
        setupAttrs(model, name);
    }

    protected void setup(MetaModel model) {
        if (!setupDone) {
            if (getRelated().length > 0) {
                setupRelated(model);
            } else {
                setupNonRelated(model);
            }
            setupDone = true;
        }
    }

    protected void setupNonRelated(MetaModel model) {

    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Related {
    }

    protected void setupRelated(MetaModel model) {
        String relModel = this.modelName;
        MetaField field = null;
        for (String fname : getRelated()) {
            field = model.getRegistry().get(relModel).findField(fname);
            if (field == null) {
                throw new ModelException(String.format("关联引用字段%s的字段%s不存在", this, fname));
            }
            if (!field.setupDone) {
                field.setup(model.getRegistry().get(relModel));
            }
            if (field instanceof RelationalField) {
                relModel = ((RelationalField<?>) field).getComodel();
            }
            if (!field.sortable) {
                sortable = false;
            }
        }
        relatedField = field;
        // check type consistency
        if (field == null || !getType().equals(field.getType())) {
            throw new TypeException(String.format("关联字段[%s]的类型与[%s]不一致", this, field));
        }

        // TODO searchable

        if (depends == null) {
            depends = new ArrayList<>();
            depends.add(StringUtils.join(getRelated(), "."));
        }

        Class<?> clazz = getClass();
        for (Field f : getFields(clazz).values()) {
            if (f.isAnnotationPresent(Related.class)) {
                if (args.containsKey(f.getName())) {
                    continue;
                }
                f.setAccessible(true);
                try {
                    f.set(this, f.get(field));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void setupAttrs(MetaModel model, String name) {
        Map<String, Object> attrs = getAttrs(model, name);
        Map<String, Field> fields = getFields(getClass());
        for (String key : attrs.keySet()) {
            try {
                Field field = fields.get(key);
                field.setAccessible(true);
                field.set(this, attrs.get(key));
            } catch (Exception e) {
                logger.warn("Field {}.{} unknown parameter {}", model.getName(), name, key);
            }
        }
    }

    Map<String, Field> getFields(Class<?> clazz) {
        Map<String, Field> result = new HashMap<>(16);
        Class<?> current = clazz;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                String name = f.getName();
                result.putIfAbsent(name, f);
            }
            current = current.getSuperclass();
        }
        return result;
    }

    protected Map<String, Object> getAttrs(MetaModel model, String name) {
        Boolean automatic = (Boolean) args.getOrDefault(Constants.AUTOMATIC, false);
        Boolean manual = (Boolean) args.getOrDefault(Constants.MANUAL, false);
        Map<String, Object> attrs = new HashMap<>(16);
        if (!automatic && !manual) {
            for (MetaField field : resolveMro(model, name)) {
                attrs.putAll(field.args);
            }
        }
        attrs.putAll(this.args);
        attrs.put(Constants.ARGS, this.args);
        attrs.put(Constants.MODEL_NAME, model.getName());
        attrs.put(Constants.NAME, name);

        if (attrs.get(Constants.COMPUTE) instanceof Callable) {
            // 计算字段不保存
            attrs.put(Constants.STORE, false);
            if (attrs.get(Constants.INVERSE) == null) {
                attrs.put(Constants.READONLY, attrs.getOrDefault(Constants.READONLY, true));
            }
            sortable = false;
        }

        if (Constants.STATE.equals(name)) {
            // by default, `state` fields should be reset on copy
            attrs.put(Constants.COPY, attrs.getOrDefault(Constants.COPY, false));
        }
        if (attrs.containsKey(Constants.RELATED)) {
            attrs.put(Constants.STORE, false);
            attrs.put(Constants.COPY, attrs.getOrDefault(Constants.COPY, false));
            attrs.put(Constants.READONLY, attrs.getOrDefault(Constants.READONLY, true));
        }
        return attrs;
    }

    protected List<MetaField> resolveMro(MetaModel model, String name) {
        List<MetaField> result = new ArrayList<>();
        for (MetaModel cls : model.getMro()) {
            if (cls.registry == null) {
                MetaField field = cls.findField(name);
                if (field != null) {
                    result.add(field);
                }
            }
        }
        return result;
    }

    /**
     * Convert value from the write format to the SQL format.
     */
    public Object convertToColumn(Object value, Records record, boolean validate) {
        return value;
    }

    /**
     * Convert value to the cache format; value may come from an
     * assignment, or have the format of methods
     * {@link BaseModel#read(Records, Collection)} or
     * {@link BaseModel#update(Records, Map)}. If the value represents a recordset,
     * it should be added for prefetching on record
     *
     * @param validate when True, field-specific validation of value will be
     *                 performed
     */
    public Object convertToCache(Object value, Records rec, boolean validate) {
        return value;
    }

    /**
     * Convert value from the cache format to the record format.
     * If the value represents a recordset, it should share the prefetching of
     * record.
     */
    public Object convertToRecord(Object value, Records rec) {
        return value;
    }

    /**
     * 调用方法{@link BaseModel#read(Records, Collection)}时，从数据集格式转为读的格式
     *
     * @param usePresent when True, the value's display name will be computed
     *                   using {@link BaseModel#getPresent(Records)},
     *                   if relevant for the field
     */
    public Object convertToRead(Object value, Records rec, boolean usePresent) {
        return value;
    }

    /**
     * Convert value from any format to the format of method
     * {@link BaseModel#update(Records, Map)}.
     */
    public Object convertToWrite(Object value, Records rec) {
        Object cacheValue = convertToCache(value, rec, true);
        Object recordValue = convertToRecord(cacheValue, rec);
        return convertToRead(recordValue, rec, true);
    }

    /**
     * Convert value from the record format to the export format.
     */
    public Object convertToExport(Object value, Records rec) {
        return value == null ? "" : value;
    }

    /**
     * Convert value from the record format to a suitable display name.
     */
    public Object convertToPresent(Object value, Records rec) {
        return value == null ? "" : value.toString();
    }

    // update database schema

    /**
     * Update the database schema to implement this field
     */
    protected void updateDb(Records model, Map<String, DbColumn> columns) {
        if (getColumnType() == ColumnType.None) {
            return;
        }
        DbColumn column = columns.get(name);
        updateDbColumn(model, column);
        updateDbNotNull(model, column);
    }

    protected void updateDbNotNull(Records model, DbColumn column) {
        boolean hasNotNull = column != null && !column.isNullable();
        boolean needInitColumn = isRequired() && !hasNotNull;
        if (needInitColumn) {
            // TODO init if table has rows
            Cursor cr = model.getEnv().getCursor();
            SqlDialect sd = cr.getSqlDialect();
            String sql = String.format("SELECT 1 FROM %s", model.getMeta().getTable());
            cr.execute(sd.getPaging(sql, 1, 0));
            if (cr.getRowCount() > 0) {
                try {
                    model.call("initColumn", getName());
                } catch (Exception exc) {
                    logger.warn("initColumn失败", exc);
                }
            }
        }
        if (isRequired() && !hasNotNull) {
            model.getEnv().getRegistry().addPostInit(e -> {
                Cursor cr = e.getCursor();
                SqlDialect sd = cr.getSqlDialect();
                sd.setNotNull(cr, model.getMeta().getTable(), getName(), getDbColumnType(sd));
            });
        } else if (!isRequired() && hasNotNull) {
            Cursor cr = model.getEnv().getCursor();
            SqlDialect sd = cr.getSqlDialect();
            sd.dropNotNull(cr, model.getMeta().getTable(), getName(), getDbColumnType(sd));
        }
    }

    /**
     * Create/update the column corresponding to this field
     */
    protected void updateDbColumn(Records model, DbColumn column) {
        Cursor cr = model.getEnv().getCursor();
        SqlDialect sd = cr.getSqlDialect();
        if (column == null) {
            sd.createColumn(cr, model.getMeta().getTable(), name, getDbColumnType(sd), label, !required);
            return;
        }

        // TODO rename or recreate column
    }

    // ############################################################################
    // #
    // # Alternatively stored fields: if fields don't have a {@link ColumnType}}
    // (not stored as regular db columns) they go through a read/create/write
    // # protocol instead
    // #

    /**
     * Read the value of this field on records, and store it in cache.
     */
    public void read(Records records) {
        throw new UnsupportedOperationException(String.format("Method read() undefined on %s", this));
    }

    /**
     * Write the value of this field on the given records, which have just
     * been created.
     */
    public void create(List<Tuple<Records, Object>> recordValues) {

    }

    /**
     * Write the value of this field on records. This method must update
     * the cache and prepare database updates.
     */
    public Records write(Records records, Object value) {
        Cache cache = records.getEnv().getCache();
        Object cacheValue = convertToCache(value, records, true);
        records = cache.getRecordsDifferentFrom(records, this, cacheValue);
        if (!records.any()) {
            return records;
        }
        if (isInherited()) {
            String[] paths = getRelated();
            for (Records r : records) {
                Records rel = r;
                for (int i = 0; i < paths.length - 1; i++) {
                    rel = rel.getRec(paths[i]);
                }
                rel.set(relatedField.getName(), value);
            }
        } else {
            Object[] values = new Object[records.size()];
            Arrays.fill(values, cacheValue);
            cache.update(records, this, Arrays.asList(values));

            // update toupdate
            if (isStore()) {
                ToUpdate.IdValues toupdate = records.getEnv().getToUpdate().get(records.getMeta().getName());
                Records record = records.browse(records.getIds()[0]);
                Object writeValue = convertToWrite(cacheValue, record);
                Object columnValue = convertToColumn(writeValue, record, true);
                for (String id : records.getIds()) {
                    toupdate.get(id).put(getName(), columnValue);
                }
            }
        }

        return records;
    }

    /**
     * return the value of field this on record
     */
    public Object get(Records record) {
        if (!record.any()) {
            Object value = convertToCache(null, record, false);
            return convertToRecord(value, record);
        }
        record.ensureOne();
        if (getRelated().length > 0) {
            return computeRelated(record);
        }
        Environment env = record.getEnv();
        Cache cache = env.getCache();
        Object value = cache.get(record, this, Void.class);
        if (value == Void.class) {
            if (getCompute() != null) {
                value = getCompute().call(record);
            } else if (isStore() && StringUtils.isNotEmpty(record.getId())) {
                Records rec = inCacheWithout(record, this, 0);
                rec.call("fetchField", this);
                if (!cache.contains(record, this) && !record.exists().any()) {
                    throw new ValidationException(record.l10n("记录 %s 不存在或者已被删除", record));
                }
                value = cache.get(record, this);
            } else {
                value = null;
            }
        }
        return convertToRecord(value, record);
    }

    Object computeRelated(Records record) {
        Records rec = record;
        Object value = null;
        for (String name : getRelated()) {
            value = null;
            if (rec.size() == 1) {
                value = rec.get(name);
            } else {
                for (Records r : rec) {
                    value = r.get(name);
                    break;
                }
            }
            if (value instanceof Records) {
                rec = (Records) value;
            }
        }
        return value;
    }

    Records inCacheWithout(Records rec, MetaField field, Integer limit) {
        if (limit == null) {
            limit = 1000;
        }

        Collection<String> ids = expandIds(rec.getId(), rec.getPrefetchIds());
        ids = rec.getEnv().getCache().getMissingIds(rec.browse(ids), field);
        if (limit > 0 && ids.size() > limit) {
            ids = ids.stream().limit(limit).collect(Collectors.toList());
        }
        return rec.browse(ids);
    }

    Collection<String> expandIds(String id, Supplier<String[]> prefetchIds) {
        Set<String> ids = new HashSet<>();
        ids.add(id);
        if (prefetchIds != null) {
            ids.addAll(Arrays.asList(prefetchIds.get()));
        }
        return ids;
    }

    /**
     * set the value of field this on records
     */
    public void set(Records records, Object value) {
        Object writeValue = convertToWrite(value, records);
        records.update(new HashMap<String, Object>(1) {
            {
                put(getName(), writeValue);
            }
        });

    }

    @Override
    public String toString() {
        return String.format("MetaField(%s.%s) - %s", modelName, name, type);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }

    @Override
    public int hashCode() {
        return (modelName + name).hashCode();
    }
}
