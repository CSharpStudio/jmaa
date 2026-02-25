package org.jmaa.base.models;

import cn.hutool.poi.excel.ExcelReader;
import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import org.jmaa.base.utils.ImportXlsOptions;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.fields.*;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.util.*;
import org.apache.commons.collections4.SetUtils;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Model.Meta(name = "ir.import", label = "导入数据")
public class IrImport extends AbstractModel {
    static final int batchSize = 100;
    static Pattern presentPattern = Pattern.compile("\\((?<present>\\S+?)\\)");

    public String importXls(Records record, ExcelReader reader, ImportXlsOptions options) {
        MetaModel model = record.getEnv().getRegistry().get(options.getModel());
        List<Map<String, Object>> values = readValueList(record, reader, options);
        convertValues(record, model, options.getFields().keySet(), values, options);
        Cursor cr = record.getEnv().getCursor();
        String uploadId = saveUpload(record, options);
        if (values.size() < 500) {
            try {
                Map<String, Integer> result = (Map<String, Integer>) model.browse(record.getEnv()).call("createOrUpdate", values);
                String message = record.l10n("导入成功，新增%s条，更新%s条", result.get("created"), result.get("updated"));
                cr.execute("update res_upload set message=%s,progress=100,state=%s,success=%s,stop_time=%s where id=%s",
                    Arrays.asList(message, ResUpload.FINISH, true, new ServerDate(), uploadId));
                return message;
            } catch (Exception e) {
                String error = ThrowableUtils.getCause(e).getMessage();
                cr.execute("update res_upload set message=%s,state=%s,success=%s,stop_time=%s where id=%s",
                    Arrays.asList(error, ResUpload.FINISH, false, new ServerDate(), uploadId));
                throw e;
            }
        }
        saveValueAsync(record, uploadId, model, values);
        return "async";
    }

    public List<Map<String, Object>> readValueList(Records record, ExcelReader reader, ImportXlsOptions options) {
        List<Map<String, Object>> values = new ArrayList<>();
        Map<String, Integer> fields = options.getFields();
        String parentField = options.getParentField();
        String parentId = options.getParentId();
        boolean setParent = Utils.isNotEmpty(parentField) && Utils.isNotEmpty(parentId) && !fields.containsKey((parentField));
        int rowCount = reader.getRowCount();
        for (int idx = options.getHeadRow(); idx < rowCount; idx++) {
            List<Object> rowData = reader.readRow(idx);
            if (rowData.isEmpty()) {
                break;
            }
            Map<String, Object> rowValue = new HashMap<>();
            values.add(rowValue);
            for (Map.Entry<String, Integer> entry : fields.entrySet()) {
                int columnIdx = entry.getValue();
                Object value = columnIdx < rowData.size() ? rowData.get(columnIdx) : null;
                rowValue.put(entry.getKey(), value);
            }
            if (setParent) {
                rowValue.put(parentField, parentId);
            }
        }
        return values;
    }

    public Object importValues(Records record, MetaModel model, List<Map<String, Object>> values) {
        if (values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> row = values.get(0);
        convertValues(record, model, row.keySet(), values, new ImportOptions());
        Map<String, Integer> result = (Map<String, Integer>) record.getEnv().get(model.getName()).call("createOrUpdate", values);
        return record.l10n("导入成功，新增%s条，更新%s条", result.get("created"), result.get("updated"));
    }

    public void saveValueAsync(Records record, String uploadId, MetaModel model, List<Map<String, Object>> values) {
        Environment env = record.getEnv();
        Logger logger = record.getLogger();
        CompletableFuture.runAsync(() -> {
            try (Cursor cur = env.getDatabase().openCursor()) {
                try {
                    Environment e = new Environment(env.getRegistry(), cur, env.getUserId(), env.getContext());
                    int i = 0;
                    int created = 0;
                    int updated = 0;
                    while (i < values.size()) {
                        int to = i + batchSize;
                        if (to > values.size()) {
                            to = values.size();
                        }
                        List<Map<String, Object>> batch = values.subList(i, to);
                        Map<String, Integer> result = (Map<String, Integer>) model.browse(e).call("createOrUpdate", batch);
                        created += Utils.toInt(result.get("created"));
                        updated += Utils.toInt(result.get("updated"));
                        i += batchSize;
                        int progress = i * 100 / values.size();
                        cur.execute("update res_upload set progress=%s where id=%s", Arrays.asList(progress, uploadId));
                        e.get("base").flush();
                        cur.commit();
                    }
                    e.get("base").flush();
                    String message = e.l10n("导入成功，新增%s条，更新%s条", created, updated);
                    cur.execute("update res_upload set message=%s,progress=100,state=%s,success=%s,stop_time=%s where id=%s",
                        Arrays.asList(message, ResUpload.STOP, true, new ServerDate(), uploadId));
                    cur.commit();
                } catch (Exception e) {
                    String error = ThrowableUtils.getCause(e).getMessage();
                    if (StringUtils.isEmpty(error)) {
                        error = e.toString();
                    }
                    if (error.length() > 2000) {
                        error = error.substring(0, 2000);
                    }
                    cur.rollback();
                    cur.execute("update res_upload set message=%s,state=%s,success=%s,stop_time=%s where id=%s",
                        Arrays.asList(error, ResUpload.STOP, false, new ServerDate(), uploadId));
                    cur.commit();
                }
            } catch (Exception exc) {
                logger.error("导入失败", exc);
            }
        });
    }

    public String saveUpload(Records record, ImportXlsOptions options) {
        Supplier<Map<String, Object>> getFile = () -> {
            try {
                String fileName = options.getFile().getOriginalFilename();
                String type = fileName.substring(fileName.lastIndexOf(".") + 1);
                return new FileInfo() {{
                    setType(type);
                    setName(fileName);
                    setData(IoUtils.toByteArray(options.getFile().getInputStream()));
                }};
            } catch (Exception e) {
                throw new PlatformException("读取文件失败", e);
            }
        };
        Map<String, Object> values = new HashMap<>();
        String model = options.getModel();
        String label = record.getEnv().getRegistry().get(model).getLabel();
        values.put("title", record.l10n("[%s]: %s", label, options.getFile().getOriginalFilename()));
        values.put("model", model);
        values.put("options", JSONObject.toJSONString(options));
        values.put("user_id", record.getEnv().getUserId());
        values.put("upload_time", new ServerDate());
        values.put("file", getFile.get());
        return record.getEnv().get("res.upload").create(values).getId();
    }

    public void convertValues(Records record, MetaModel model, Collection<String> fields, List<Map<String, Object>> values, ImportOptions importOptions) {
        List<String> errors = new ArrayList<>();
        List<Many2oneReferenceField> refFields = new ArrayList<>();
        List<One2manyField> o2mFields = new ArrayList<>();
        for (String key : fields) {
            MetaField field = model.getField(key);
            if (field instanceof Many2oneReferenceField) {
                refFields.add((Many2oneReferenceField) field);
            }
            if (field instanceof One2manyField) {
                o2mFields.add((One2manyField) field);
            }
            convertValue(record, field, values, importOptions, errors);
        }
        for (Many2oneReferenceField field : refFields) {
            convertToMany2oneReference(record, field, values, importOptions, errors);
        }
        for (One2manyField field : o2mFields) {
            convertToOne2many(record, model, field, values, importOptions, errors);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors.stream().collect(Collectors.joining("\r\n")));
        }
    }

    public void convertValue(Records record, MetaField field, List<Map<String, Object>> values, ImportOptions importOptions, List<String> errors) {
        for (int idx = 0; idx < values.size(); idx++) {
            Object value = values.get(idx);
            if (Utils.isEmpty(value) && field.isRequired()) {
                errors.add(record.l10n("列[%s]行[%s]:不能为空。", importOptions.getColumnName(field), idx + importOptions.getHeadRow()));
            }
        }
        if (field instanceof SelectionField) {
            convertToSelection(record, (SelectionField) field, values, importOptions, errors);
        } else if (field instanceof BooleanField) {
            convertToBoolean(record, field, values, importOptions, errors);
        } else if (field instanceof Many2oneField) {
            convertToMany2One(record, (Many2oneField) field, values, importOptions, errors);
        } else if (field instanceof Many2manyField) {
            convertToMany2Many(record, (Many2manyField) field, values, importOptions, errors);
        } else {
            Function<MetaField, String> getTypeName = f -> {
                if (f instanceof IntegerField) {
                    return "整数";
                } else if (f instanceof FloatField) {
                    return "小数";
                } else if (f instanceof StringField) {
                    return "文本";
                } else if (f instanceof DateTimeField || f instanceof DateField) {
                    return "日期";
                }
                return f.getType();
            };
            for (int idx = 0; idx < values.size(); idx++) {
                Map<String, Object> map = values.get(idx);
                Object value = map.get(field.getName());
                try {
                    if (!Utils.isEmpty(value)) {
                        field.convertToWrite(value, record);
                    }
                } catch (Exception e) {
                    errors.add(record.l10n("列[%s]行[%s]:值[%s]转换为%s失败:%s。",
                        importOptions.getColumnName(field), idx + importOptions.getHeadRow() + 1, value, getTypeName.apply(field), e.getMessage()));
                }
            }
        }
    }

    public void convertToMany2Many(Records record, Many2manyField field, List<Map<String, Object>> values, ImportOptions importOptions, List<String> errors) {
        Set<String> valuesSet = new HashSet<>();
        for (Map<String, Object> map : values) {
            Object value = map.get(field.getName());
            if (Utils.isNotEmpty(value)) {
                for (String item : value.toString().split(",")) {
                    valuesSet.add(item);
                    Matcher m = presentPattern.matcher(item);
                    while (m.find()) {
                        String text = m.group("present");
                        valuesSet.add(text);
                    }
                }
            }
        }
        Map<Object, String> valueIdMap = new HashMap<>();
        if (Utils.isNotEmpty(valuesSet)) {
            Records comodel = record.getEnv().get(field.getComodel());
            String[] presentFields = comodel.getMeta().getPresent();
            Criteria criteria = new Criteria();
            if (comodel.getMeta().hasMethod("presentToId")) {
                valueIdMap = (Map<Object, String>) comodel.call("presentToId", values);
            } else {
                for (String p : presentFields) {
                    criteria.or(Criteria.in(p, valuesSet));
                }
                comodel = comodel.find(criteria);
                for (Records row : comodel) {
                    for (String f : presentFields) {
                        Object value = row.get(f);
                        if (Utils.isNotEmpty(value)) {
                            valueIdMap.put(value, row.getId());
                        }
                    }
                }
            }
        }
        for (int idx = 0; idx < values.size(); idx++) {
            List<Object> commands = new ArrayList<>();
            List<Object> newIds = new ArrayList<>();
            Map<String, Object> map = values.get(idx);
            Object value = map.get(field.getName());
            if (Utils.isEmpty(value)) {
                commands.add(Arrays.asList(5, 0, 0));
            } else {
                List<String> valueSplitList = Arrays.asList(value.toString().split(","));
                for (String s : valueSplitList) {
                    String rid = valueIdMap.get(s);
                    if (rid == null) {
                        errors.add(record.l10n("列[%s]行[%s]:值[%s]无效。",
                            importOptions.getColumnName(field), idx + importOptions.getHeadRow() + 1, s));
                        break;
                    } else {
                        newIds.add(rid);
                    }
                }
                commands.add(Arrays.asList(6, 0, newIds));
            }
            map.put(field.getName(), commands);
        }
    }

    public void convertToMany2oneReference(Records record, Many2oneReferenceField field, List<Map<String, Object>> values, ImportOptions importOptions, List<String> errors) {
        String modelField = field.getModelField();
        Map<String, Set<Object>> modelValue = new HashMap<>();
        for (Map<String, Object> value : values) {
            String key = (String) value.get(modelField);
            Set<Object> data = modelValue.get(key);
            if (data == null) {
                data = new HashSet<>();
                modelValue.put(key, data);
            }
            data.add(value.get(field.getName()));
        }
        Environment env = record.getEnv();
        for (Map.Entry<String, Set<Object>> entry : modelValue.entrySet()) {
            String key = entry.getKey();
            Set<Object> distinctValues = new HashSet<>();
            Records rec = env.get(key);
            String[] presentFields = rec.getMeta().getPresent();
            Criteria criteria = new Criteria();
            for (Object value : entry.getValue()) {
                distinctValues.add(value);
                if (value instanceof String) {
                    String text = (String) value;
                    Matcher m = presentPattern.matcher(text);
                    while (m.find()) {
                        String txt = m.group("present");
                        distinctValues.add(txt);
                    }
                }
            }
            for (String p : presentFields) {
                criteria.or(Criteria.in(p, distinctValues));
            }
            rec = rec.find(criteria);
            Map<Object, String> valueIdMap = new HashMap<>(rec.size() * presentFields.length);
            for (Records r : rec) {
                valueIdMap.put(r.get("present"), r.getId());
                for (String f : presentFields) {
                    Object value = r.get(f);
                    if (Utils.isNotEmpty(value)) {
                        valueIdMap.put(value, r.getId());
                    }
                }
            }
            for (int idx = 0; idx < values.size(); idx++) {
                Map<String, Object> map = values.get(idx);
                Object model = map.get(modelField);
                if (Utils.equals(model, key)) {
                    Object value = map.get(field.getName());
                    if (Utils.isNotEmpty(value)) {
                        String rid = valueIdMap.get(value.toString());
                        map.put(field.getName(), rid);
                        if (rid == null) {
                            errors.add(record.l10n("列[%s]行[%s]:值[%s]无效。",
                                importOptions.getColumnName(field), idx + importOptions.getHeadRow() + 1, value));
                        }
                    }
                }
            }
        }
    }

    public void convertToMany2One(Records record, Many2oneField field, List<Map<String, Object>> values, ImportOptions importOptions, List<String> errors) {
        Records comodel = record.getEnv().get(field.getComodel());
        String[] presentFields = comodel.getMeta().getPresent();
        Criteria criteria = new Criteria();
        Map<Object, String> valueIdMap = new HashMap<>();
        if (comodel.getMeta().hasMethod("presentToId")) {
            Set<Object> distinctValues = new HashSet<>();
            for (Map<String, Object> map : values) {
                Object value = map.get(field.getName());
                distinctValues.add(value);
            }
            valueIdMap = (Map<Object, String>) comodel.call("presentToId", distinctValues);
        } else {
            Set<Object> distinctValues = new HashSet<>();
            for (Map<String, Object> map : values) {
                Object value = map.get(field.getName());
                distinctValues.add(value);
                if (value instanceof String) {
                    String text = (String) value;
                    Matcher m = presentPattern.matcher(text);
                    while (m.find()) {
                        String txt = m.group("present");
                        distinctValues.add(txt);
                    }
                }
            }
            for (String pf : presentFields) {
                Field coField = comodel.getMeta().getField(pf);
                if (coField instanceof Many2oneField) {
                    Many2oneField m2o = (Many2oneField) coField;
                    Records co2 = record.getEnv().get(m2o.getComodel());
                    String[] pFields = co2.getMeta().getPresent();
                    for (String p2 : pFields) {
                        criteria.or(Criteria.in(pf + "." + p2, distinctValues));
                    }
                } else {
                    criteria.or(Criteria.in(pf, distinctValues));
                }
            }
            comodel = comodel.find(criteria);
            for (Records row : comodel) {
                valueIdMap.put(row.get("present"), row.getId());
                for (String f : presentFields) {
                    Object value = row.get(f);
                    if (Utils.isNotEmpty(value)) {
                        valueIdMap.put(value, row.getId());
                    }
                }
            }
        }
        for (int idx = 0; idx < values.size(); idx++) {
            Map<String, Object> map = values.get(idx);
            Object value = map.get(field.getName());
            if (Utils.isNotEmpty(value)) {
                String rid = valueIdMap.get(value.toString());
                map.put(field.getName(), rid);
                if (rid == null) {
                    errors.add(record.l10n("列[%s]行[%s]:值[%s]无效。",
                        importOptions.getColumnName(field), idx + importOptions.getHeadRow() + 1, value));
                }
            } else {
                //处理空白字符，避免外键约束验证不通过
                map.put(field.getName(), null);
            }
        }
    }

    public void convertToBoolean(Records record, MetaField field, List<Map<String, Object>> values, ImportOptions importOptions, List<String> errors) {
        for (int idx = 0; idx < values.size(); idx++) {
            Map<String, Object> map = values.get(idx);
            Object value = map.get(field.getName());
            if (Utils.isNotEmpty(value)) {
                if ("TRUE".equalsIgnoreCase(value.toString()) || "是".equals(value) || "Y".equals(value) || Boolean.TRUE.equals(value)) {
                    map.put(field.getName(), true);
                } else if ("FALSE".equalsIgnoreCase(value.toString()) || "否".equals(value) || "N".equals(value) || Boolean.FALSE.equals(value)) {
                    map.put(field.getName(), false);
                } else {
                    errors.add(record.l10n("列[%s]行[%s]:值[%s]无效，有效值应为[TRUE,FALSE]。",
                        importOptions.getColumnName(field), idx + importOptions.getHeadRow() + 1, value));
                }
            }
        }
    }

    public void convertToSelection(Records record, SelectionField field, List<Map<String, Object>> values, ImportOptions importOptions, List<String> errors) {
        Map<String, String> options = field.getOptions(record);
        String validValues = options.values().stream().collect(Collectors.joining(","));
        BiFunction<Map<String, String>, Object, String> findOption = (map, value) -> {
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (value.equals(e.getValue()) || value.equals(e.getKey())) {
                    return e.getKey();
                }
            }
            return null;
        };
        for (int idx = 0; idx < values.size(); idx++) {
            Map<String, Object> map = values.get(idx);
            Object value = map.get(field.getName());
            if (Utils.isNotEmpty(value)) {
                String opt = findOption.apply(options, value);
                map.put(field.getName(), opt);
                if (opt == null) {
                    errors.add(record.l10n("列[%s]行[%s]:值[%s]无效，有效值应为[%s]。",
                        importOptions.getColumnName(field), idx + importOptions.getHeadRow() + 1, value, validValues));
                }
            }
        }
    }

    public void convertToOne2many(Records record, MetaModel model, One2manyField field, List<Map<String, Object>> values, ImportOptions importOptions, List<String> errors) {
        Records comodel = record.getEnv().get(field.getComodel());
        for (Map<String, Object> map : values) {
            List<Object> commands = new ArrayList<>();
            List<Map<String, Object>> list = (List<Map<String, Object>>) map.get(field.getName());
            Records parent = findByValue(record.getEnv(), model, map);
            if (parent.any()) {
                if (Utils.isEmpty(list)) {
                    commands.add(Arrays.asList(5, 0, 0));
                } else {
                    Map<String, Object> data = list.get(0);
                    convertValues(record, comodel.getMeta(), data.keySet(), list, importOptions);
                    Set<String> existIds = new HashSet<>();
                    for (Map<String, Object> value : list) {
                        value.put(field.getInverseName(), parent.getId());
                        Records exist = findByValue(record.getEnv(), comodel.getMeta(), value);
                        if (exist.any()) {
                            existIds.add(exist.getId());
                            commands.add(Arrays.asList(1, exist.getId(), value));
                        } else {
                            commands.add(Arrays.asList(0, 0, value));
                        }
                    }
                    Records children = parent.getRec(field.getName());
                    for (String cid : children.getIds()) {
                        if (!existIds.contains(cid)) {
                            commands.add(Arrays.asList(2, cid));
                        }
                    }
                }
            } else if (Utils.isNotEmpty(list)) {
                Map<String, Object> data = list.get(0);
                convertValues(record, comodel.getMeta(), data.keySet(), list, importOptions);
                for (Map<String, Object> value : list) {
                    commands.add(Arrays.asList(0, 0, value));
                }
            }
            map.put(field.getName(), commands);
        }
    }

    static Records findByValue(Environment env, MetaModel meta, Map<String, Object> values) {
        List<String> uniqueFields = getUniqueFields(values.keySet(), meta);
        if (!uniqueFields.isEmpty()) {
            Criteria criteria = new Criteria();
            for (String field : uniqueFields) {
                Object v = values.get(field);
                if ("company_id".equals(field) && v == null) {
                    v = env.getCompany().getId();
                }
                criteria.and(Criteria.equal(field, v));
            }
            return env.get(meta.getName()).find(criteria);
        }
        return env.get(meta.getName());
    }
}
