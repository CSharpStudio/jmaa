package org.jmaa.sdk.core;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.*;
import org.jmaa.sdk.exceptions.*;
import org.jmaa.sdk.fields.*;
import org.jmaa.sdk.services.*;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.util.*;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/***
 * 模型基类，提供基础模型方法
 *
 * @author Eric Liang
 */

@Model.Service(name = "read", label = "查看", description = "读取记录集指定字段的值", type = ReadService.class)
@Model.Service(name = "create", label = "创建", description = "为模型创建新记录", type = CreateService.class)
@Model.Service(name = "createBatch", label = "批量新建", auth = "create", description = "批量创建新记录", type = BatchCreateService.class)
@Model.Service(name = "find", label = "查找", auth = "read", description = "根据参数搜索记录", type = FindService.class)
@Model.Service(name = "search", label = "查询", auth = "read", description = "搜索并读取记录集指定字段的值", type = SearchService.class)
@Model.Service(name = "count", label = "计数", auth = "read", description = "统计匹配条件的记录数", type = CountService.class)
@Model.Service(name = "delete", label = "删除", description = "删除当前集合的记录", type = DeleteService.class)
@Model.Service(name = "update", label = "编辑", description = "使用提供的值更新当前集中的所有记录", type = UpdateService.class)
@Model.Service(name = "onchange", label = "字段变更", description = "字段变更获取数据", auth = "read", type = OnChangeService.class)
@Model.Service(name = "action", label = "动作", description = "操作动作", auth = "read", type = ActionService.class)
public class BaseModel {
    private final static Logger logger = LoggerFactory.getLogger(BaseModel.class);
    protected final static List<String> LOG_ACCESS_COLUMNS = Arrays.asList(Constants.CREATE_DATE, Constants.CREATE_UID, Constants.UPDATE_DATE, Constants.UPDATE_UID);

    /**
     * 是否自动创建数据库表，如果设置false，可以重写{@link BaseModel#init(Records)}方法手动创建数据库表。
     * {@link Model}默认是true, {@link AbstractModel}默认是false。
     */
    protected boolean isAuto;
    protected boolean isAbstract = true;
    protected boolean isTransient;
    protected boolean custom;

    public boolean isAuto() {
        return isAuto;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isTransient() {
        return isTransient;
    }

    public boolean isCustom() {
        return custom;
    }


    /**
     * 模型可以使用查询视图做映射，重写些方法，提供视图sql
     *
     * @param rec 数据集
     * @return sql语句
     */
    public String getTableQuery(Records rec) {
        return null;
    }

    /**
     * This method may be overridden to create or modify a model's database schema.
     *
     * @param rec 数据集
     */
    public void init(Records rec) {

    }

    /**
     * 批量创建模型的记录，根据提供的字段值字典列表，批量创建数据库记录
     *
     * @param rec        记录集
     * @param valuesList 新值列表
     * @return 新记录
     */
    @SuppressWarnings("AlibabaMethodTooLong")
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        if (valuesList.isEmpty()) {
            return rec.browse();
        }
        Set<String> badNames = new HashSet<>();
        if (rec.getMeta().getRegistry().loaded) {
            if (ObjectUtils.toBoolean(rec.getEnv().getContext().get("#autoId"), true)) {
                badNames.add(Constants.ID);
            }
            badNames.add("parent_path");
            if (rec.getMeta().isLogAccess()) {
                badNames.addAll(LOG_ACCESS_COLUMNS);
            }
        }
        List<NewData> dataList = new ArrayList<>();
        Set<MetaField> inversedFields = new HashSet<>();
        for (Map<String, Object> values : valuesList) {
            values = addMissingDefaultValues(rec, values);

            for (String badName : badNames) {
                values.remove(badName);
            }
            // set magic fields
            if (rec.getMeta().isLogAccess()) {
                values.putIfAbsent(Constants.CREATE_UID, rec.getEnv().getUserId());
                values.putIfAbsent(Constants.CREATE_DATE, new ServerDate());
                values.putIfAbsent(Constants.UPDATE_UID, rec.getEnv().getUserId());
                values.putIfAbsent(Constants.UPDATE_DATE, new ServerDate());
            }

            // distribute fields into sets for various purposes
            NewData data = new NewData();
            data.stored = new HashMap<>(values.size());
            data.inherited = new HashMap<>();
            data.inversed = new HashMap<>();
            for (String key : values.keySet()) {
                Object val = values.get(key);
                MetaField field = rec.getMeta().findField(key);
                if (field == null) {
                    logger.warn("{}.create()遇到未知字段{}", rec.getMeta().getName(), key);
                    continue;
                }

                if (field.isStore()) {
                    data.stored.put(key, val);
                }

                if (field.isInherited()) {
                    Map<String, Object> m = data.inherited.computeIfAbsent(field.relatedField.modelName, k -> new HashMap<>());
                    m.put(key, val);
                } else if (field.inverse != null) {
                    data.inversed.put(key, val);
                    inversedFields.add(field);
                }
            }
            dataList.add(data);
        }
        Environment env = rec.getEnv();
        // create or update parent records
        for (Entry<String, String> e : rec.getMeta().delegates.entrySet()) {
            String modelName = e.getKey();
            String parentName = e.getValue();
            List<NewData> parentDataList = new ArrayList<>();
            for (NewData data : dataList) {
                if (!data.stored.containsKey(parentName)) {
                    parentDataList.add(data);
                } else if (data.inherited.containsKey(modelName)) {
                    Records parent = env.get(modelName).browse((String) data.stored.get(parentName));
                    parent.update(data.inherited.get(modelName));
                }
            }
            if (parentDataList.size() > 0) {
                List<Map<String, Object>> toCreate = new ArrayList<>();
                for (NewData d : parentDataList) {
                    toCreate.add(d.inherited.get(modelName));
                }
                Records parents = env.get(modelName).createBatch(toCreate);
                String[] ids = parents.getIds();
                for (int i = 0; i < parentDataList.size(); i++) {
                    parentDataList.get(i).stored.put(parentName, ids[i]);
                }
            }
        }

        // create records with stored fields
        Records records = doCreate(rec, dataList);
        Cache cache = rec.getEnv().getCache();
        for (NewData data : dataList) {
            for (MetaField field : inversedFields) {
                if (data.inversed.containsKey(field.name)) {
                    Object cacheValue = field.convertToCache(data.inversed.get(field.name), rec, false);
                    cache.set(data.records, field, cacheValue);
                }
            }
            for (MetaField field : inversedFields) {
                field.inverse.call(data.records);
            }
            for (MetaField field : inversedFields) {
                cache.remove(data.records, field);
            }

        }

        // TODO validateConstraints
        return records;
    }

    /**
     * 获取字段默认值
     *
     * @param rec    数据集
     * @param fields 字段列表
     * @return 默认值
     */
    @Model.ServiceMethod(auth = "read", label = "获取默认值", doc = "获取属性默认值")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDefaultValue(Records rec, Collection<String> fields) {
        KvMap defaults = new KvMap();
        MetaModel model = rec.getMeta();
        Map<String, List<String>> parentFields = new HashMap<>();
        for (String name : fields) {
            // TODO look up context
            // TODO look up ir.default
            MetaField field = model.getField(name);
            // look up field.default
            if (field.isStore() && field.defaultValue != null) {
                Object value = field.getDefault(rec);
                value = field.convertToCache(value, rec, false);
                value = field.convertToWrite(value, rec);
                defaults.put(name, value);
            }
            // delegate to parent model
            if (field.isInherited()) {
                field = field.relatedField;
                List<String> list = parentFields.computeIfAbsent(field.getModelName(), k -> new ArrayList<>());
                list.add(field.getName());
            }
        }
        for (Entry<String, List<String>> entry : parentFields.entrySet()) {
            defaults.putAll((Map<String, Object>) rec.getEnv().get(entry.getKey()).call("getDefaultValue", entry.getValue()));
        }
        return defaults;
    }

    static class NewData {
        public Map<String, Object> stored;
        public Map<String, Map<String, Object>> inherited;
        public Map<String, Object> inversed;
        public Records records;
    }

    /**
     * 验证参数值（必填、唯一）
     *
     * @param rec    数据集
     * @param values 值
     * @param create 是否创建中
     */
    public void validateValues(Records rec, Map<String, Object> values, boolean create) {
        validateRequired(rec, values, create);
        validateUnique(rec, values, create);
    }

    /**
     * 验证必填, 当字段使用{@link MetaField#isRequired()}声明为必需时，验证提供的值为非空值(字符串非empty)
     *
     * @param rec    数据集
     * @param values 值
     * @param create 是否创建状态
     */
    public void validateRequired(Records rec, Map<String, Object> values, boolean create) {
        List<String> errors = new ArrayList<>();
        Object defaultValue = create ? null : true;
        for (MetaField f : rec.getMeta().getFields().values()) {
            if (f.isStore() && f.isRequired()) {
                Object value = values.getOrDefault(f.getName(), defaultValue);
                boolean isEmpty = value == null || (value instanceof String && StringUtils.isEmpty((String) value));
                if (isEmpty) {
                    errors.add(rec.l10n(f.getLabel()));
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(rec.l10n("%s 不能为空", StringUtils.join(errors)));
        }
    }

    /**
     * 验证使用{@link UniqueConstraint}声明的唯一约束,
     * 判断条件后执行{@link BaseModel#validateUniqueConstraint}
     *
     * @param records 数据集
     * @param values  值
     * @param create  是否创建状态
     */
    public void validateUnique(Records records, Map<String, Object> values, boolean create) {
        for (UniqueConstraint constraint : records.getMeta().getUniques()) {
            boolean match = false;
            for (String field : constraint.getFields()) {
                if (values.containsKey(field)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                if (create) {
                    validateUniqueConstraint(records, constraint, values, create);
                } else {
                    for (Records rec : records) {
                        validateUniqueConstraint(rec, constraint, values, create);
                    }
                }
            }
        }
    }

    /**
     * 验证使用{@link UniqueConstraint}声明的唯一约束
     *
     * @param record     数据集
     * @param constraint 约束
     * @param values     值
     * @param create     是否创建
     */
    public void validateUniqueConstraint(Records record, UniqueConstraint constraint, Map<String, Object> values, boolean create) {
        Criteria criteria = new Criteria();
        Map<String, Object> uniqueValues = new HashMap<>();
        for (String field : constraint.getFields()) {
            Object value;
            if (!create && !values.containsKey(field)) {
                value = record.get(field);
                if (value instanceof Records) {
                    MetaField f = record.getMeta().getField(field);
                    if (f instanceof Many2oneField) {
                        value = ((Records) value).getId();
                    }
                }
            } else {
                value = values.get(field);
            }
            uniqueValues.put(field, value);
            if (Utils.isNotEmpty(value)) {
                criteria.and(Criteria.equal(field, value));
            }
        }
        if (criteria.size() > 0) {
            if (!create) {
                criteria.and(Criteria.binary("id", "!=", record.getId()));
            }
            boolean exists = record.count(criteria) > 0;
            if (exists) {
                String message = constraint.getMessage();
                List<String> errors = new ArrayList<>();
                for (String field : constraint.getFields()) {
                    MetaField mf = record.getMeta().getField(field);
                    String fieldLabel = mf.getLabel();
                    Object fieldValue = uniqueValues.get(field);
                    if (ObjectUtils.isNotEmpty(fieldValue)) {
                        if (mf instanceof Many2oneField) {
                            fieldValue = ((Records) mf.convertToRecord(fieldValue, record)).get("present");
                        } else if (mf instanceof SelectionField) {
                            SelectionField sf = (SelectionField) mf;
                            fieldValue = sf.getOptions(record).get(fieldValue);
                        } else if (mf instanceof Many2oneReferenceField) {
                            Many2oneReferenceField rf = (Many2oneReferenceField) mf;
                            String key = rf.getModelField();
                            String model = (String) values.get(key);
                            if (Utils.isNotEmpty(model)) {
                                fieldValue = record.getEnv().get(model, (String) fieldValue).get("present");
                            } else if (!create) {
                                model = record.getString(key);
                                fieldValue = record.getEnv().get(model, (String) fieldValue).get("present");
                            }
                        }
                    }
                    errors.add(record.l10n(fieldLabel) + String.format("[%s]", ObjectUtils.defaultIfNull(fieldValue, "")));
                }
                logger.warn(String.format("模型[%s]:%s 已存在，不能重复", record.getMeta().getName(), StringUtils.join(errors)));
                if (StringUtils.isNotEmpty(message)) {
                    String msg = record.l10n(message) + ":" + StringUtils.join(errors);
                    throw new ValidationException(msg);
                }
                throw new ValidationException(record.l10n("%s 已存在，不能重复", StringUtils.join(errors)));
            }
        }
    }

    /**
     * 验证使用{@link Model.Constrains}声明的约束
     *
     * @param rec    数据集
     * @param fields 字段集合
     */
    public void validateConstraints(Records rec, Collection<String> fields) {
        Set<String> called = new HashSet<>();
        for (String field : fields) {
            Collection<String> methods = rec.getMeta().getConstrains(field);
            for (String method : methods) {
                if (called.contains(method)) {
                    continue;
                }
                rec.call(method);
                called.add(method);
            }
        }
    }

    Records doCreate(Records rec, List<NewData> dataList) {
        Cursor cr = rec.getEnv().getCursor();
        List<String> newIds = new ArrayList<>();
        Set<MetaField> otherFields = new HashSet<>();
        Set<MetaField> translatedFields = new HashSet<>();
        for (NewData data : dataList) {
            Map<String, Object> stored = data.stored;
            validateValues(rec, stored, true);
            List<String> columns = new ArrayList<>();
            List<String> formats = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            if (!stored.containsKey(Constants.ID)) {
                String id = IdWorker.nextId();
                columns.add(cr.quote(Constants.ID));
                formats.add("%s");
                values.add(id);
                newIds.add(id);
            } else {
                newIds.add((String) stored.get(Constants.ID));
            }

            for (String name : stored.keySet().stream().sorted().toArray(String[]::new)) {
                MetaField field = rec.getMeta().getField(name);
                if (field.getColumnType() != ColumnType.None) {
                    Object val = stored.get(name);
                    if (val instanceof AsSql) {
                        columns.add(cr.quote(name));
                        formats.add(((AsSql) val).getValue());
                    } else if (val instanceof ServerDate) {
                        columns.add(cr.quote(name));
                        formats.add(cr.getSqlDialect().getNowUtc());
                    } else {
                        Object colVal = field.convertToColumn(val, rec, true);
                        columns.add(cr.quote(name));
                        formats.add("%s");
                        values.add(colVal);
                        if (field instanceof StringField && ((StringField<?>) field).isTranslate()) {
                            translatedFields.add(field);
                        }
                    }
                } else {
                    otherFields.add(field);
                }
            }
            String sql = String.format("INSERT INTO %s(%s) VALUES (%s)", cr.quote(rec.getMeta().getTable()), StringUtils.join(columns), StringUtils.join(formats));
            cr.execute(sql, values);
        }

        // put the new records in cache, and update inverse fields, for many2one
        // cachetoclear is an optimization to avoid modified()'s cost until other_fields
        // are processed
        Records records = putNewInCache(rec, newIds, otherFields, dataList);
        Set<String> fields = new HashSet<>();
        for (NewData data : dataList) {
            fields.addAll(data.stored.keySet());
        }
        validateConstraints(records, fields);
        checkAccessRule(records, "create");

        // TODO add translations
        // if(rec.getEnv().getLang() != "zh_CN"){

        // }
        return records;
    }

    static Records putNewInCache(Records rec, List<String> newIds, Set<MetaField> otherFields, List<NewData> dataList) {
        Records records = rec.browse(newIds.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        Cache cache = rec.getEnv().getCache();
        List<Tuple<Records, MetaField>> cachetoclear = new ArrayList<>();
        for (List<?> datas : ArrayUtils.zip(dataList, records)) {
            NewData data = (NewData) datas.get(0);
            Records record = (Records) datas.get(1);
            data.records = record;
            Map<String, Object> stored = data.stored;
            for (MetaField field : rec.getMeta().getFields().values()) {
                if ("one2many".equals(field.type) || "many2many".equals(field.type)) {
                    cache.set(record, field, Collections.emptyList());
                }
            }
            for (Entry<String, Object> entry : stored.entrySet()) {
                String fname = entry.getKey();
                Object value = entry.getValue();
                MetaField field = rec.getMeta().getField(fname);
                if (value instanceof AsSql || "one2many".equals(field.type) || "many2many".equals(field.type)) {
                    cachetoclear.add(new Tuple<>(record, field));
                } else {
                    Object cacheValue = field.convertToCache(value, rec, true);
                    cache.set(record, field, cacheValue);
                }
            }
        }

        if (!otherFields.isEmpty()) {
            Records others = records.withContext(clearContext(rec.getEnv().getContext()));
            // TODO sort by sequence
            for (MetaField field : otherFields) {
                List<Tuple<Records, Object>> recordValues = new ArrayList<>();
                for (List<Object> x : ArrayUtils.zip(others, dataList)) {
                    NewData data = (NewData) x.get(1);
                    Map<String, Object> stored = data.stored;
                    if (stored.containsKey(field.getName())) {
                        Records other = (Records) x.get(0);
                        Object value = stored.get(field.getName());
                        recordValues.add(new Tuple<>(other, value));
                    }
                }
                field.create(recordValues);
            }
        }
        for (Tuple<Records, MetaField> tuple : cachetoclear) {
            if (cache.contains(tuple.getItem1(), tuple.getItem2())) {
                cache.remove(tuple.getItem1(), tuple.getItem2());
            }
        }
        return records;
    }

    static Map<String, Object> clearContext(Map<String, Object> ctx) {
        Map<String, Object> result = new HashMap<>(ctx.size());
        for (String key : ctx.keySet()) {
            if (!key.startsWith("default_")) {
                result.put(key, ctx.get(key));
            }
        }
        return result;
    }

    void collectModelsToAvoid(Set<String> avoidModels, Records model, Map<String, Object> vals) {
        for (Entry<String, String> entry : model.getMeta().delegates.entrySet()) {
            if (vals.containsKey(entry.getValue())) {
                avoidModels.add(entry.getKey());
            } else {
                collectModelsToAvoid(avoidModels, model.getEnv().get(entry.getKey()), vals);
            }
        }
    }

    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        // avoid overriding inherited values when parent is set
        Set<String> avoidModels = new HashSet<>();
        collectModelsToAvoid(avoidModels, rec, values);
        Set<String> missingDefaults = new HashSet<>();
        Predicate<MetaField> avoid = field -> {
            if (avoidModels.size() > 0) {
                while (field.inherited) {
                    field = field.relatedField;
                    if (avoidModels.contains(field.modelName)) {
                        return true;
                    }
                }
            }
            return false;
        };
        for (Entry<String, MetaField> entry : rec.getMeta().getFields().entrySet()) {
            if (!values.containsKey(entry.getKey()) && !avoid.test(entry.getValue())) {
                missingDefaults.add(entry.getKey());
            }
        }
        Map<String, Object> defaults = getDefaultValue(rec, missingDefaults);
        defaults.putAll(values);
        return defaults;
    }

    /**
     * 读取记录集指定字段的数据
     *
     * @param rec    记录集
     * @param fields 字段集合
     * @return 字段值列表
     */
    public List<Map<String, Object>> read(Records rec, Collection<String> fields) {
        Set<String> storedFields = new HashSet<>(fields.size());
        for (String name : fields) {
            MetaField field = rec.getMeta().findField(name);
            if (field == null) {
                throw new ValueException(String.format("模型[%s]的字段[%s]无效", rec.getMeta().getName(), name));
            }
            if (field.isStore()) {
                storedFields.add(name);
            }
        }
        doRead(rec, storedFields);
        List<Map<String, Object>> result = new ArrayList<>();
        Object usePresentContext = rec.getEnv().getContext().get("usePresent");
        Function<MetaField, Boolean> usePresent = f -> false;
        if (usePresentContext instanceof Boolean && (Boolean) usePresentContext) {
            usePresent = f -> !(f instanceof RelationalMultiField);
        } else if (usePresentContext instanceof List) {
            List<Object> usePresentFields = (List<Object>) usePresentContext;
            usePresent = f -> usePresentFields.contains(f.getName());
        }
        for (Records r : rec) {
            Map<String, Object> values = new HashMap<>(fields.size());
            values.put(Constants.ID, r.getId());
            for (String fname : fields) {
                MetaField field = rec.getMeta().getField(fname);
                values.put(fname, field.convertToRead(r.get(fname), r, usePresent.apply(field)));
            }
            result.add(values);
        }
        return result;
    }

    public Records exists(Records rec) {
        if (!rec.any()) {
            return rec;
        }
        Cursor cr = rec.getEnv().getCursor();
        String query = String.format("SELECT id FROM %s WHERE id IN %%s", cr.quote(rec.getMeta().getTable()));
        Set<String> set = new HashSet<>();
        for (Object[] subIds : cr.splitForInConditions(rec.getIds())) {
            cr.execute(query, Collections.singletonList(subIds));
            set.addAll(cr.fetchAll().stream().map(row -> (String) row[0]).collect(Collectors.toList()));
        }
        List<String> ids = new ArrayList<>();
        for (String id : rec.getIds()) {
            if (set.contains(id)) {
                ids.add(id);
            }
        }
        return rec.browse(ids);
    }

    public void fetchField(Records rec, MetaField field) {
        List<String> fieldNames = new ArrayList<>();
        if ((Boolean) rec.getEnv().getContext().getOrDefault(Constants.PREFETCH_FIELDS, true) && field.prefetch) {
            for (MetaField f : rec.getMeta().getFields().values()) {
                if (f.prefetch) {
                    fieldNames.add(f.getName());
                }
            }
        } else {
            fieldNames.add(field.getName());
        }
        doRead(rec, fieldNames);
    }

    @Model.ServiceMethod(auth = "read", label = "根据关联字段统计数量", ids = false)
    public long countByField(Records rec,
                             @Doc(doc = "关联的字段") String relatedField,
                             @Doc(doc = "查询条件") Criteria criteria) {
        MetaField f = rec.getMeta().findField(relatedField);
        if (f == null) {
            throw new ModelException(String.format("模型[%s]不存在字段[%s]", rec.getMeta().getName(), relatedField));
        }
        if (rec.any() && f instanceof Many2manyField) {
            Many2manyField m2m = (Many2manyField) f;
            return m2m.count(rec);
        }
        Environment env = rec.getEnv();
        Records comodel;
        RelationalField<?> relational = null;
        Criteria filter;
        if (f instanceof Many2oneReferenceField) {
            String m = (String) env.getContext().get("comodel");
            if (Utils.isEmpty(m)) {
                return 0;
            }
            comodel = env.get(m);
            filter = new Criteria();
        } else if (f instanceof RelationalField) {
            relational = (RelationalField<?>) f;
            comodel = env.get(relational.getComodel());
            filter = relational.getCriteria(rec);
        } else {
            throw new ModelException(String.format("模型[%s]的字段[%s]不是关联字段", rec.getMeta().getName(), relatedField));
        }
        if (!(f instanceof RelationalField)) {
            throw new ModelException(String.format("模型[%s]的字段[%s]不是关联字段", rec.getMeta().getName(), relatedField));
        }
        if (criteria != null && !criteria.isEmpty()) {
            filter.and(criteria);
        }
        Boolean activeTest = ObjectUtils.toBoolean(env.getContext().get("active_test"), true);
        if (activeTest && comodel.getMeta().getFields().containsKey("active")) {
            filter.and(Criteria.equal("active", true));
        }
        if (rec.any() && relational instanceof One2manyField) {
            filter.and(Criteria.in(((One2manyField) relational).getInverseName(), rec.getIds()));
        }
        return comodel.count(filter);
    }

    @Model.ServiceMethod(auth = "read", label = "根据关联字段查询数据", ids = false)
    public Map<String, Object> searchByField(Records rec,
                                             @Doc(doc = "关联的字段") String relatedField,
                                             @Doc(doc = "查询条件") Criteria criteria,
                                             @Doc(doc = "偏移量") Integer offset,
                                             @Doc(doc = "行数") Integer limit,
                                             @Doc(doc = "读取的字段") Collection<String> fields,
                                             @Doc(doc = "排序") String order) {
        MetaField f = rec.getMeta().findField(relatedField);
        if (f == null) {
            throw new ModelException(String.format("模型[%s]不存在字段[%s]", rec.getMeta().getName(), relatedField));
        }
        if (!(f instanceof RelationalField) && !(f instanceof Many2oneReferenceField)) {
            throw new ModelException(String.format("模型[%s]的字段[%s]不是关联字段", rec.getMeta().getName(), relatedField));
        }
        Environment env = rec.getEnv();
        Records comodel;
        RelationalField<?> relational = null;
        Criteria filter;
        if (f instanceof Many2oneReferenceField) {
            String m = (String) env.getContext().get("comodel");
            if (Utils.isEmpty(m)) {
                return new KvMap() {{
                    put("values", Collections.emptyList());
                }};
            }
            comodel = env.get(m);
            filter = new Criteria();
        } else if (f instanceof RelationalField) {
            relational = (RelationalField<?>) f;
            comodel = env.get(relational.getComodel());
            filter = relational.getCriteria(rec);
        } else {
            throw new ModelException(String.format("模型[%s]的字段[%s]不是关联字段", rec.getMeta().getName(), relatedField));
        }
        if (criteria != null && !criteria.isEmpty()) {
            filter.and(criteria);
        }
        Map context = env.getContext();
        Boolean activeTest = Utils.toBoolean(context.get("active_test"), true);
        if (activeTest && comodel.getMeta().getFields().containsKey("active")) {
            filter.and(Criteria.equal("active", true));
        }
        Boolean companyTest = Utils.toBoolean(context.get("company_test"), true);
        boolean companyLess = Utils.toBoolean(context.get("#companyLess"));
        if (companyTest && !companyLess && comodel.getMeta().hasBase("mixin.company")) {
            filter.and(Criteria.equal("company_id", env.getCompany().getId()));
        }
        if (companyTest && !companyLess && comodel.getMeta().hasBase("mixin.companies")) {
            filter.and(Criteria.equal("company_ids", env.getCompany().getId()));
        }
        if (fields == null) {
            fields = Collections.singletonList(Constants.PRESENT);
        }
        if (limit == null) {
            limit = 10;
        }
        if (rec.any() && relational instanceof Many2manyField) {
            Many2manyField m2m = (Many2manyField) relational;
            List<String> ids = m2m.find(rec, limit + 1, offset);
            filter.and(Criteria.in("id", ids));
            offset = 0;
        }
        if (rec.any() && relational instanceof One2manyField) {
            filter.and(Criteria.in(((One2manyField) relational).getInverseName(), rec.getIds()));
        }
        return comodel.searchLimit(fields, filter, offset, limit, order);
    }

    @Model.ServiceMethod(auth = "read", label = "获取展示值", doc = "读取记录的展示值")
    @Doc(doc = "id/名称的列表", sample = "[[\"01ogtcwblyuio\", \"name\"]]")
    public List<Object[]> getPresent(Records rec) {
        List<Object[]> result = new ArrayList<>();
        MetaModel meta = rec.getMeta();
        String[] present = meta.getPresent();
        for (Records r : rec) {
            String display = "";
            if (present.length > 0) {
                String format = meta.getPresentFormat();
                if (StringUtils.isEmpty(format)) {
                    for (String fieldName : present) {
                        MetaField field = meta.getField(fieldName);
                        if (display.length() > 0) {
                            display += ",";
                        }
                        display += field.convertToPresent(r.get(field), r);
                    }
                } else {
                    display = format;
                    for (String fieldName : present) {
                        MetaField field = meta.getField(fieldName);
                        String fieldPresent = (String) field.convertToPresent(r.get(field), r);
                        display = display.replaceAll("\\{" + fieldName + "}", Matcher.quoteReplacement(fieldPresent));
                    }
                }
            } else {
                display = String.format("%s,%s", meta.getName(), r.getId());
            }
            result.add(new Object[]{r.getId(), display});
        }
        return result;
    }

    void doRead(Records rec, Collection<String> fields) {
        if (!rec.any()) {
            return;
        }
        // TODO flush fields only
        rec.flush();

        Set<MetaField> fieldsRead = new HashSet<>();
        Set<MetaField> fieldsPre = new HashSet<>();
        for (String name : fields) {
            if (Constants.ID.equals(name)) {
                continue;
            }
            MetaField field = rec.getMeta().getField(name);
            if (field.isStore()) {
                if (field.getColumnType() == ColumnType.None) {
                    fieldsRead.add(field);
                    // else if (!(field instanceof StringField) || !((StringField<?>)
                    // field).getTranslate()) {
                    // TODO 翻译处理
                } else {
                    fieldsPre.add(field);
                }
            }
        }
        List<Object[]> result = new ArrayList<>();
        if (!fieldsPre.isEmpty()) {
            Cursor cr = rec.getEnv().getCursor();
            Query query = new Query(cr, rec.getMeta().getTable(), getTableQuery(rec));
            // TODO _apply_ir_rules(rec, query, "read");
            List<String> qualNames = new ArrayList<>();
            qualNames.add(qualify(rec, rec.getMeta().getField(Constants.ID), query));
            for (MetaField field : fieldsPre) {
                qualNames.add(qualify(rec, field, query));
            }

            query.addWhere(String.format("%s.%s IN %%s", cr.quote(rec.getMeta().getTable()), cr.quote(Constants.ID)));
            Query.SelectClause sql = query.select(qualNames);
            for (Object[] subIds : cr.splitForInConditions(rec.getIds())) {
                List<Object> params = new ArrayList<>(sql.getParams());
                params.add(Arrays.asList(subIds));
                cr.execute(sql.getQuery(), params);
                result.addAll(cr.fetchAll());
            }

        } else {
            checkAccessRule(rec, "read");
            for (String id : rec.getIds()) {
                result.add(new Object[]{id});
            }
        }

        Records fetched = rec.browse();
        if (!result.isEmpty()) {
            List<List<Object>> col = ArrayUtils.zip(result.toArray(new Object[0][0]));
            int next = 0;
            List<Object> ids = col.get(next++);
            fetched = rec.browse(ids.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
            Cache cache = rec.getEnv().getCache();
            for (MetaField field : fieldsPre) {
                List<Object> values = col.get(next++);
                // TODO translate
                cache.update(fetched, field, values);
            }
            for (MetaField field : fieldsRead) {
                field.read(fetched);
            }
        }

        // TODO missing
    }

    static String qualify(Records rec, MetaField field, Query query) {
        String col = field.getName();
        String res = inheritsJoinCalc(rec, rec.getMeta().getTable(), field.getName(), query);
//        if (Constants.BINARY.equals(field.type)) {
//
//        }
        return String.format("%s as %s", res, rec.getEnv().getCursor().quote(col));
    }

    static String inheritsJoinCalc(Records rec, String alias, String fname, Query query) {
        Environment env = rec.getEnv();
        Cursor cr = env.getCursor();
        Records model = rec;
        MetaField field = rec.getMeta().getField(fname);
        while (field.isInherited()) {
            Records parentModel = env.get(field.getRelatedField().getModelName());
            String parentFname = field.getRelated()[0];
            String parentAlias = query.leftJoin(alias, parentFname, parentModel.getMeta().getTable(), "id", parentFname);
            model = parentModel;
            alias = parentAlias;
            field = field.getRelatedField();
        }
        if (field instanceof Many2manyField) {
            Many2manyField m2m = (Many2manyField) field;
            String relAlias = query.leftJoin(alias, "id", m2m.getRelation(), m2m.getColumn1(), field.getName());
            return String.format("%s.%s", cr.quote(relAlias), cr.quote(m2m.getColumn2()));
        } else if (field instanceof StringField && ((StringField<?>) field).isTranslate()) {
            return generateTranslatedField(model, alias, fname, query);
        }
        return String.format("%s.%s", cr.quote(alias), cr.quote(fname));
    }

    static String generateTranslatedField(Records rec, String tableAlias, String field, Query query) {
        Cursor cr = rec.getEnv().getCursor();
        return String.format("%s.%s", cr.quote(tableAlias), cr.quote(field));
    }

    /**
     * 根据查询条件查找记录集，根据指定参数查询满足条件的记录ID集合，返回记录集
     *
     * @param rec      记录集
     * @param criteria 查询条件
     * @param offset   记录开始位置
     * @param limit    记录限制数
     * @param order    记录排序顺序
     * @return 记录集
     */
    public Records find(Records rec, Criteria criteria, Integer offset, Integer limit, String order) {
        List<String> ids = doFind(rec, criteria, offset, limit, order);
        return rec.browse(ids);
    }

    List<String> doFind(Records rec, Criteria criteria, Integer offset, Integer limit, String order) {
        if (Expression.isFalse(rec, criteria)) {
            return Collections.emptyList();
        }
        flushSearch(rec, criteria, null, order);

        Query query = whereCalc(rec, criteria, true);
        // _apply_ir_rules

        query.setOrder(generateOrderBy(rec, order, query)).setLimit(limit).setOffset(offset);

        Cursor cr = rec.getEnv().getCursor();
        Query.SelectClause select = query.select();
        cr.execute(select.getQuery(), select.getParams());
        List<String> ids = new ArrayList<>();
        for (Object[] row : cr.fetchAll()) {
            ids.add((String) row[0]);
        }
        return ids;
    }

    static List<String> generateM2oOrderBy(Records rec, String alias, String[] m2oPath, Query query, boolean reverseDirection, Set<String> seen) {
        String orderField = m2oPath[0];
        MetaField field = rec.getMeta().getField(orderField);
        if (field.isInherited()) {
            String qualifiedField = inheritsJoinCalc(rec, alias, orderField, query);
            String[] arr = qualifiedField.split("\\.");
            // TODO remove quote
            alias = arr[0];
            orderField = arr[1];
            field = field.getBaseField();
        }
        if (!field.isStore()) {
            return Collections.emptyList();
        }
        Many2oneField m2o = (Many2oneField) field;
        Records destModel = rec.getEnv().get(m2o.getComodel());
        String m2oOrder;
        if (m2oPath.length == 1) {
            m2oOrder = String.join(",", destModel.getMeta().getPresent());
            if (StringUtils.isBlank(m2oOrder)) {
                m2oOrder = destModel.getMeta().getOrder();
                if (m2oOrder.contains(Constants.COMMA)) {
                    // _order is complex, can't use it here, so we default to id
                    m2oOrder = "id";
                }
            }
        } else {
            m2oOrder = Arrays.stream(m2oPath).skip(1).collect(Collectors.joining("."));
        }
        String destAlias = query.leftJoin(alias, orderField, destModel.getMeta().getTable(), Constants.ID, orderField);
        return generateOrderByInner(destModel, destAlias, m2oOrder, query, reverseDirection, seen);
    }

    static List<String> generateOrderByInner(Records rec, String alias, String orderSpec, Query query, boolean reverseDirection, Set<String> seen) {
        if (seen == null) {
            seen = new HashSet<>();
        }
        // TODO _check_qorder
        List<String> orderByElements = new ArrayList<>();
        Cursor cr = rec.getEnv().getCursor();
        for (String orderPart : orderSpec.split(Constants.COMMA)) {
            String[] orderSplit = orderPart.trim().split(" ");
            String orderField = orderSplit[0].trim();
            String orderDirection = orderSplit.length == 2 ? orderSplit[1].trim().toUpperCase() : "";
            if (reverseDirection) {
                orderDirection = "DESC".equals(orderDirection) ? "ASC" : "DESC";
            }
            boolean doReverse = "DESC".equals(orderDirection);
            String[] m2oPath = orderField.split("\\.");
            if (m2oPath.length > 0) {
                orderField = m2oPath[0];
            }
            MetaField field = rec.getMeta().getField(orderField);
            String[] related = field.getRelated();
            if (related.length > 0) {
                List<String> path = new ArrayList<>();
                do {
                    orderField = related[0];
                    for (int i = related.length - 1; i >= 0; i--) {
                        path.add(related[i]);
                    }
                    field = rec.getMeta().getField(orderField);
                    related = field.getRelated();
                } while (related.length > 0);
                for (int i = m2oPath.length - 1; i > 0; i--) {
                    path.add(m2oPath[i]);
                }
                Collections.reverse(path);
                m2oPath = path.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            }
            if (Constants.ID.equals(orderField)) {
                orderByElements.add(String.format("%s.%s %s", cr.quote(alias), cr.quote(orderField), orderDirection));
            } else {
                if (field.isInherited()) {
                    field = field.getBaseField();
                }
                if (field.isStore() && "many2one".equals(field.type)) {
                    Many2oneField m2o = (Many2oneField) field;
                    String key = field.modelName + "|" + m2o.getComodel() + "|" + orderField;
                    if (!seen.contains(key)) {
                        seen.add(key);
                        orderByElements.addAll(generateM2oOrderBy(rec, alias, m2oPath, query, doReverse, seen));
                    }
                } else if (field.isStore() && field.getColumnType() != ColumnType.None) {
                    String qualifieldName = inheritsJoinCalc(rec, alias, orderField, query);
                    if ("boolean".equals(field.type)) {
                        qualifieldName = String.format("COALESCE(%s, %s)", qualifieldName, cr.getSqlDialect().prepareObject(false));
                    }
                    orderByElements.add(String.format("%s %s", qualifieldName, orderDirection));
                } else {
                    logger.warn("模型{}不能按字段{}排序", rec.getMeta().getName(), orderField);
                }
            }
        }
        return orderByElements;
    }

    public static String generateOrderBy(Records rec, String orderSpec, Query query) {
        String orderByClause = "";
        if (StringUtils.isBlank(orderSpec)) {
            orderSpec = rec.getMeta().getOrder();
        }
        if (StringUtils.isNotBlank(orderSpec)) {
            List<String> orderByElements = generateOrderByInner(rec, rec.getMeta().getTable(), orderSpec, query, false, null);
            if (!orderByElements.isEmpty()) {
                orderByClause = StringUtils.join(orderByElements);
            }
        }
        return orderByClause;
    }

    public static Query whereCalc(Records rec, Criteria criteria, boolean activeTest) {
        // TODO active criteria
        if (!criteria.isEmpty()) {
            return new Expression(criteria, rec, null, null).getQuery();
        } else {
            return new Query(rec.getEnv().getCursor(), rec.getMeta().getTable(), (String) rec.call("getTableQuery"));
        }
    }

    /**
     * 查询并读取，返回字段值列表
     *
     * @param rec      记录集
     * @param fields   字段列表
     * @param criteria 查询条件
     * @param offset   记录开始位置
     * @param limit    记录限制数
     * @param order    记录排序顺序
     * @return 字段值列表
     */
    public List<Map<String, Object>> search(Records rec, Collection<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        return read(find(rec, criteria, offset, limit, order), fields);
    }

    /**
     * 按present字段查询
     *
     * @param rec     数据集
     * @param fields  要查询的字段
     * @param keyword 查询关键字
     * @param offset  记录开始位置
     * @param limit   记录限制数
     * @param order   记录排序顺序
     * @return 数据列表
     */
    @Model.ServiceMethod(auth = "read", label = "展示值查询", doc = "按记录展示字段查询")
    public List<Map<String, Object>> presentSearch(Records rec, Collection<String> fields, String keyword, Integer offset, Integer limit, String order) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotEmpty(keyword)) {
            String[] present = rec.getMeta().getPresent();
            for (String fieldName : present) {
                criteria.or(Criteria.like(fieldName, keyword));
            }
        }
        return read(find(rec, criteria, offset, limit, order), fields);
    }

    public String computePresent(Records rec) {
        return (String) rec.getPresent()[1];
    }

    static Pattern presentPattern = Pattern.compile("\\((?<present>\\S+?)\\)");

    public Criteria searchPresent(Records rec, String op, Object value) {
        Criteria criteria = new Criteria();
        boolean hasValue = ObjectUtils.isNotEmpty(value);
        if (hasValue) {
            for (String field : rec.getMeta().getPresent()) {
                criteria.or(field, op, value);
            }
            if (value instanceof String) {
                String text = (String) value;
                Matcher m = presentPattern.matcher(text);
                if (m.find()) {
                    String txt = m.group("present");
                    for (String field : rec.getMeta().getPresent()) {
                        criteria.or(field, op, txt);
                    }
                }
            }
        }
        return criteria;
    }

    /**
     * 统计记录集的数量
     *
     * @param rec      记录集
     * @param criteria 查询条件
     * @return 条数
     */
    public long count(Records rec, Criteria criteria) {
        if (Expression.isFalse(rec, criteria)) {
            return 0;
        }
        flushSearch(rec, criteria, null, null);

        Query query = whereCalc(rec, criteria, true);
        // _apply_ir_rules

        Cursor cr = rec.getEnv().getCursor();
        Query.SelectClause select = query.select("count(1)");
        cr.execute(select.getQuery(), select.getParams());
        return ObjectUtils.toLong(cr.fetchOne()[0]);
    }

    /**
     * 更新所有记录的值
     *
     * @param records 记录集
     * @param values  字段值字典
     */
    public void update(Records records, Map<String, Object> values) {
        if (!records.any()) {
            return;
        }
        checkAccessRule(records, "write");

        MetaModel meta = records.getMeta();
        Set<String> badNames = new HashSet<>(5);
        badNames.add(Constants.ID);
        if (meta.isLogAccess()) {
            badNames.addAll(LOG_ACCESS_COLUMNS);
        }
        for (String badName : badNames) {
            values.remove(badName);
        }
        // set magic fields
        if (meta.isLogAccess()) {
            values.putIfAbsent(Constants.UPDATE_UID, records.getEnv().getUserId());
            values.putIfAbsent(Constants.UPDATE_DATE, new ServerDate());
        }

        // X2many fields must be written last, because they flush other fields when
        // deleting lines.
        List<MetaField> fields = values.keySet().stream().map(meta::getField).collect(Collectors.toList());
        fields.stream().sorted(Comparator.comparing(f -> (f instanceof RelationalMultiField) ? 0 : 20)).forEach(field -> field.write(records, values.get(field.getName())));

        for (MetaField field : fields) {
            if (field.inverse != null) {
                field.inverse.call(records);
            }
        }
        Cache cache = records.getEnv().getCache();
        for (MetaField field : fields) {
            if (field instanceof RelationalMultiField) {
                cache.remove(records, field);
            }
        }

        validateConstraints(records, values.keySet());
    }

    void doUpdate(Records rec, Map<String, Object> vals) {
        if (!rec.any()) {
            return;
        }
        validateValues(rec, vals, false);
        // 并发检查
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        Cursor cr = rec.getEnv().getCursor();

        if (rec.getMeta().isLogAccess()) {
            vals.putIfAbsent(Constants.UPDATE_UID, rec.getEnv().getUserId());
            vals.putIfAbsent(Constants.UPDATE_DATE, new ServerDate());
        }

        vals.entrySet().stream().sorted(Entry.comparingByKey()).forEach(entry -> {
            String fieldName = entry.getKey();
            Object val = entry.getValue();
            if (rec.getMeta().isLogAccess() && LOG_ACCESS_COLUMNS.contains(fieldName) && val == null) {
                return;
            }
            MetaField field = rec.getMeta().getField(fieldName);
            assert field.isStore();
            if (val instanceof AsSql) {
                columns.add(String.format("%s=%s", cr.quote(fieldName), ((AsSql) val).getValue()));
            } else if (val instanceof ServerDate) {
                columns.add(String.format("%s=%s", cr.quote(fieldName), cr.getSqlDialect().getNowUtc()));
            } else {
                columns.add(String.format("%s=%%s", cr.quote(fieldName)));

                values.add(val);
            }
        });

        if (!columns.isEmpty()) {
            String sql = String.format("UPDATE %s SET %s WHERE id IN %%s", cr.quote(rec.getMeta().getTable()), StringUtils.join(columns));
            for (Object[] subIds : cr.splitForInConditions(rec.getIds())) {
                List<Object> params = new ArrayList<>(values);
                params.add(Arrays.asList(subIds));
                cr.execute(sql, params);
                if (cr.getRowCount() < 1) {
                    logger.warn(String.format("更新模型[%s]受影响行数为0,id[%s]", rec.getMeta().getName(), StringUtils.join(subIds)));
                }
                // TODO check rowcount == sub_ids.length
            }
        }
    }

    /**
     * 更新记录集的值
     *
     * @param rec    记录集
     * @param values 字段值字典
     */
    public void load(Records rec, Map<String, Object> values) {
        Cache cache = rec.getEnv().getCache();
        for (Entry<String, Object> entry : values.entrySet()) {
            MetaField field = rec.getMeta().getField(entry.getKey());
            cache.update(rec, field, Collections.singletonList(entry.getValue()));
        }
    }

    public void checkDeleteParent(Records rec) {
        MetaField f = rec.getMeta().findField("parent_id");
        if (f instanceof Many2oneField) {
            Many2oneField pid = (Many2oneField) f;
            if (DeleteMode.Restrict.equals(pid.getOnDelete())) {
                if (rec.count(Criteria.in("parent_id", Arrays.asList(rec.getIds()))) > 0) {
                    throw new ValidationException(rec.l10n("删除失败, 请先删除子") + rec.l10n(rec.getMeta().getLabel()));
                }
            }
        }
    }

    public boolean delete(Records rec) {
        if (!rec.any()) {
            return false;
        }
        for (Tuple<String, Boolean> tuple : rec.getMeta().onDelete) {
            if (tuple.getItem2() || !Utils.toBoolean(rec.getEnv().getContext().get("uninstall"))) {
                rec.call(tuple.getItem1());
            }
        }
        flush(rec);
        checkDeleteParent(rec);
        Cursor cr = rec.getEnv().getCursor();
        for (Object[] subIds : cr.splitForInConditions(rec.getIds())) {
            // log delete data
            String sql = "DELETE FROM " + cr.quote(rec.getMeta().getTable()) + " WHERE id IN %s";
            try {
                cr.execute(sql, Collections.singletonList(Arrays.asList(subIds)));
            } catch (SqlConstraintException e) {
                String constraint = e.getConstraint();
                String model = (String) rec.getEnv().get("ir.model.constraint").find(Criteria.equal("name", constraint)).get("message");
                if (StringUtils.isNotEmpty(model)) {
                    String msg = rec.l10n("删除失败, 数据被 %s 引用", rec.l10n(rec.getEnv().getRegistry().get(model).getLabel()));
                    throw new ValidationException(msg);
                }
                throw new ValidationException(rec.l10n("删除失败, 数据被引用"));
            }
        }
        return true;
    }

    /**
     * 把当前模型待更新的所有数据保存到数据库
     *
     * @param rec 记录集
     */
    public void flushModel(Records rec, Collection<String> fieldNames) {
        // TODO related_field
        ToUpdate.IdValues idValues = rec.getEnv().getToUpdate().remove(rec.getMeta().getName());
        processFlush(rec.getEnv(), rec.getMeta().getName(), idValues);
    }

    /**
     * 把待更新的所有数据保存到数据库
     *
     * @param rec 数据集
     */
    public void flush(Records rec) {
        ToUpdate toUpdate = rec.getEnv().getToUpdate();
        Environment env = rec.getEnv();
        for (String model : toUpdate.getModels().toArray(ArrayUtils.EMPTY_STRING_ARRAY)) {
            ToUpdate.IdValues idValues = toUpdate.remove(model);
            processFlush(env, model, idValues);
        }
    }

    public void flushSearch(Records rec, Criteria criteria, Collection<String> fields, String order) {
        // TODO
        flushModel(rec, fields);
    }

    void processFlush(Environment env, String model, ToUpdate.IdValues idValues) {
        // group record ids by valuesIds, to update in batch when possible
        Map<Map<String, Object>, List<String>> valuesIds = new HashMap<>(idValues.size());
        for (Entry<String, Map<String, Object>> e : idValues.entrySet()) {
            Map<String, Object> values = e.getValue();
            List<String> ids = valuesIds.computeIfAbsent(values, k -> new ArrayList<>());
            ids.add(e.getKey());
        }
        for (Entry<Map<String, Object>, List<String>> e : valuesIds.entrySet()) {
            Map<String, Object> values = e.getKey();
            List<String> ids = e.getValue();
            Records recs = env.get(model, ids);
            doUpdate(recs, values);
        }
    }

    /**
     * 复制记录
     *
     * @param rec 记录集
     * @return 新记录集
     */
    @Model.ServiceMethod(auth = "create", label = "复制记录", doc = "复制记录的数据并保存")
    public Records copy(Records rec, Map<String, Object> defaultValues) {
        rec.ensureOne();
        Map<String, Object> values = copyData(rec, rec.withContext("active_test", false), defaultValues);
        return rec.create(values);
    }

    public Map<String, Object> copyData(Records root, Records rec, Map<String, Object> defaultValues) {
        Map<String, Object> defaults = new HashMap<>(16);
        if (defaultValues != null) {
            defaults.putAll(defaultValues);
        }
        for (Entry<String, MetaField> e : rec.getMeta().getFields().entrySet()) {
            String fieldName = e.getKey();
            MetaField field = e.getValue();
            if (Constants.ID.equals(fieldName) || LOG_ACCESS_COLUMNS.contains(fieldName) || defaults.containsKey(fieldName) || !field.isCopy()) {
                continue;
            }
            if (Constants.ONE2MANY.equals(field.getType())) {
                Records o2m = (Records) rec.get(fieldName);
                List<List<Object>> lines = new ArrayList<>();
                for (Records r : o2m) {
                    lines.add(Arrays.asList(0, 0, copyData(root, r, null)));
                }
                defaults.put(fieldName, lines);
            } else if (Constants.MANY2MANY.equals(field.getType())) {
                List<String> ids = Arrays.asList(((Records) rec.get(fieldName)).getIds());
                defaults.put(fieldName, Collections.singletonList(Arrays.asList(6, 0, ids)));
            } else {
                defaults.put(fieldName, field.convertToWrite(rec.get(field), rec));
            }
        }
        MetaModel model = rec.getMeta();
        for (UniqueConstraint u : model.getUniques()) {
            for (String field : u.getFields()) {
                boolean isDefaultValue = defaultValues != null && defaultValues.containsKey(field);
                if (!isDefaultValue && defaults.containsKey(field) && model.getField(field) instanceof StringField) {
                    String v = rec.l10n("%s (副本)", defaults.get(field));
                    defaults.put(field, v);
                }
            }
        }
        return defaults;
    }

    /**
     * 调用ir.import模型导入数据
     */
    @Model.ServiceMethod(label = "导入", doc = "根据唯一索引更新或者插入数据", ids = false)
    @Doc("返回值")
    public Object importData(Records record, @Doc("参考模型字段") List<Map<String, Object>> values) {
        return record.getEnv().get("ir.import").call("importValues", record.getMeta(), values);
    }

    /**
     * 创建或者更新，用于数据导入，根据唯一约束找到记录则更新，找不到则插入
     */
    public Map<String, Integer> createOrUpdate(Records record, List<Map<String, Object>> values) {
        int created = 0;
        int updated = 0;
        List<String> uniqueFields = null;
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> value : values) {
            if (uniqueFields == null) {
                uniqueFields = getUniqueFields(value.keySet(), record.getMeta());
            }
            try {
                if (!uniqueFields.isEmpty()) {
                    Criteria criteria = new Criteria();
                    for (String field : uniqueFields) {
                        Object v = value.get(field);
                        if ("company_id".equals(field) && v == null) {
                            v = record.getEnv().getCompany().getId();
                        }
                        criteria.and(Criteria.equal(field, v));
                    }
                    record = record.find(criteria);
                    if (record.any()) {
                        record.update(value);
                        updated++;
                    } else {
                        record.create(value);
                        created++;
                    }
                } else {
                    record.create(value);
                    created++;
                }
            } catch (Exception e) {
                Throwable t = ThrowableUtils.getCause(e);
                if (t instanceof ValidationException) {
                    errors.add(t.getMessage());
                } else {
                    throw e;
                }
            }
        }
        if (errors.size() > 0) {
            throw new ValidationException(errors.stream().collect(Collectors.joining("\r\n")));
        }
        HashMap<String, Integer> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        return result;
    }

    public static List<String> getUniqueFields(Set<String> fields, MetaModel meta) {
        UniqueConstraint unique = null;
        Collection<UniqueConstraint> uniques = meta.getUniques();
        Optional<UniqueConstraint> opt = uniques.stream().filter(uc -> uc.isUpdate()).findFirst();
        if (opt.isPresent()) {
            unique = opt.get();
        } else {
            for (UniqueConstraint uc : meta.getUniques()) {
                if (uc.getFields().length == 1 && fields.contains(uc.getFields()[0])) {
                    unique = uc;
                    break;
                }
                if (unique == null) {
                    if (fields.containsAll(Arrays.asList(uc.getFields()))) {
                        unique = uc;
                    } else {
                        Set<String> s = SetUtils.hashSet(uc.getFields());
                        if (s.remove("company_id") && fields.containsAll(s)) {
                            unique = uc;
                        }
                    }
                }
            }
        }
        if (unique != null) {
            return Arrays.asList(unique.getFields());
        }
        return Collections.emptyList();
    }

    /**
     * 检查字段访问权限
     *
     * @param rec       数据集
     * @param operation 操作
     * @param fields    字段集合
     * @return 有权限的字段集合
     */
    @SuppressWarnings("unchecked")
    public Collection<String> checkFieldAccessRights(Records rec, String operation, Collection<String> fields) {
        MetaModel m = rec.getMeta();
        Set<String> deny = (Set<String>) rec.getEnv().get("rbac.permission").call("loadModelDenyFields", m.getName());
        for (String field : fields) {
            if (deny.contains(field)) {
                throw new AccessException(rec.l10n("没有访问模型[%s(%s)]的字段[%s(%s)]的权限", m.getLabel(), m.getName(), m.getField(field).getLabel(), field));
            }
        }
        return fields;
    }

    public void checkAccessRule(Records rec, String action) {

    }

    public void initColumn(Records rec, String col) {
        MetaField field = rec.getMeta().getField(col);
        Object value = field.getDefault(rec);
        if (value != null) {
            value = field.convertToWrite(value, rec);
            value = field.convertToColumn(value, rec, true);
            Cursor cr = rec.getEnv().getCursor();
            String sql = String.format("UPDATE %s SET %s=%%s WHERE %s IS NULL", cr.quote(rec.getMeta().getTable()), cr.quote(field.getName()), cr.quote(field.getName()));
            cr.execute(sql, Collections.singletonList(value));
        }
    }

    /**
     * 调用父方法，类似java的super关键字
     *
     * @param records
     * @param args
     * @return
     */
    protected Object callSuper(Records records, Object... args) {
        StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
        Set<String> ignores = SetUtils.hashSet("<generated>", "MethodProxy.java", "ModelInterceptor.java");
        StackTraceElement element = null;
        for (int i = 2; i < stacks.length; i++) {
            element = stacks[i];
            if (!ignores.contains(element.getFileName())) {
                break;
            }
        }
        try {
            Class clazz = Class.forName(element.getClassName());
            return records.callSuper(clazz, element.getMethodName(), args);
        } catch (Exception ex) {
            throw new ModelException("callSuper失败", ex);
        }
    }

    private ModelRefactor refactor;

    public ModelRefactor getRefactor() {
        if (refactor == null) {
            refactor = new ModelRefactor();
            refactor.model = this;
        }
        return refactor;
    }

    public static class ModelRefactor {

        BaseModel model;

        /**
         * 从注解获取名称
         */
        public String getName() {
            Class<?> clazz = model.getClass();
            Model.Meta a = clazz.getAnnotation(Model.Meta.class);
            if (a != null) {
                String name = a.name();
                if (StringUtils.hasUpperCase(name)) {
                    throw new ModelException(String.format("模型名称[%s]不能包含大写字母", name));
                }
                return name;
            }
            return clazz.getName().toLowerCase();
        }

        /**
         * 从注解获取继承
         */
        public List<String> getInherit() {
            Class<?> clazz = model.getClass();
            Model.Meta a = clazz.getAnnotation(Model.Meta.class);
            if (a != null) {
                return new ArrayList<>(Arrays.asList(a.inherit()));
            }
            return new ArrayList<>();
        }

        /**
         * 从注解获取继承
         */
        public List<String> getInheritIf() {
            Class<?> clazz = model.getClass();
            Model.Meta a = clazz.getAnnotation(Model.Meta.class);
            if (a != null) {
                return new ArrayList<>(Arrays.asList(a.inheritIf()));
            }
            return new ArrayList<>();
        }

        /**
         * 从注解获取参数
         */
        public Map<String, Object> getArgs() {
            Map<String, Object> map = new HashMap<>(16);
            Class<?> clazz = model.getClass();
            Model.Meta a = clazz.getAnnotation(Model.Meta.class);
            if (a != null) {
                String label = a.label();
                if (StringUtils.isNotBlank(label)) {
                    map.put(Constants.LABEL, label);
                }
                String authModel = a.authModel();
                if (StringUtils.isNotBlank(authModel)) {
                    map.put(Constants.AUTH_MODEL, authModel);
                }
                String desc = a.description();
                if (StringUtils.isNotBlank(desc)) {
                    map.put(Constants.DESCRIPTION, desc);
                }
                String order = a.order();
                if (StringUtils.isNotBlank(order)) {
                    map.put(Constants.ORDER, order);
                }
                String[] present = a.present();
                if (present.length > 0) {
                    map.put(Constants.PRESENT, present);
                }
                String presentFormat = a.presentFormat();
                if (StringUtils.isNoneBlank(presentFormat)) {
                    map.put(Constants.PRESENT_FORMAT, presentFormat);
                }
                String table = a.table();
                if (StringUtils.isNotBlank(table)) {
                    map.put(Constants.TABLE, table);
                }
                BoolState log = a.logAccess();
                if (log == BoolState.False) {
                    map.put(Constants.LOG_ACCESS, false);
                } else if (log == BoolState.True) {
                    map.put(Constants.LOG_ACCESS, true);
                }
            }
            return map;
        }
    }
}
