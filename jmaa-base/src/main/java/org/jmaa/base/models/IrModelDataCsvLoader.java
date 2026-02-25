package org.jmaa.base.models;

import cn.hutool.core.text.csv.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmaa.base.utils.ImportErrorHandle;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.exceptions.ValueException;
import org.jmaa.sdk.fields.Many2manyField;
import org.jmaa.sdk.fields.Many2oneField;
import org.jmaa.sdk.fields.One2manyField;
import org.jmaa.sdk.fields.RelationalField;
import org.jmaa.sdk.util.KvMap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Model.Meta(name = "ir.model.data.csv.loader", label = "模型数据CSV加载器")
public class IrModelDataCsvLoader extends ValueModel {
    private static final Pattern refPattern = Pattern.compile("ref\\((?<ref>\\S+?)\\)");

    public void loadData(Records record, InputStream input, String module, String modelName, ImportErrorHandle onError) {
        Environment env = record.getEnv();
        List<Map<String, String>> rows = readRows(input, onError);
        Records storeRecords = record.getEnv().get(modelName);
        int rowIdx = 0;
        for (int i = 0; i < rows.size(); i++) {
            String column = null;
            String columnValue = null;
            try {
                rowIdx++;
                Map<String, String> map = rows.get(i);
                KvMap values = new KvMap(map.size());
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    column = entry.getKey();
                    if (!"id".equals(column)) {
                        columnValue = entry.getValue();
                        //对关联id字段的非空处理,获取字段对应的关联类型
                        MetaField metaField = storeRecords.getMeta().getField(column);
                        Object value = getFieldValue(record, metaField, columnValue, module);
                        values.put(entry.getKey(), value);
                    }
                }
                column = null;
                // csv文件插入数据库后不更新，@开头的id表示更新一次
                String id = map.get("id");
                boolean modify = id.startsWith("@");
                if (modify) {
                    Map<String, Object> data = (Map<String, Object>) env.get("ir.model.data").call("findData", module + "." + id);
                    if (data.isEmpty()) {
                        data = (Map<String, Object>) env.get("ir.model.data").call("findData", id.substring(1));
                        if (data.isEmpty()) {
                            throw new ValueException(String.format("id[%s]指向的数据不存在", id));
                        }
                        if (!data.get("model").equals(modelName)) {
                            throw new ValueException(String.format("id[%s]原模型[%s]与当前模型[%s]不一致", id, data.get("model"), modelName));
                        }
                        String resId = (String) data.get("res_id");
                        env.get(modelName, resId).update(values);
                        env.get("ir.model.data").create(new KvMap()
                            .set("name", id)
                            .set("module", module)
                            .set("model", modelName)
                            .set("res_id", resId)
                            .set("no_update", true));
                    }
                } else {
                    String key = id;
                    if (!key.contains(".")) {
                        key = module + "." + id;
                    }
                    Map<String, Object> data = (Map<String, Object>) env.get("ir.model.data").call("findData", key);
                    if (data.isEmpty()) {
                        Records rec = IrImport.findByValue(env, env.getRegistry().get(modelName), values);
                        if (!rec.any()) {
                            rec = rec.create(values);
                        }
                        env.get("ir.model.data").create(new KvMap()
                            .set("name", id)
                            .set("module", module)
                            .set("model", modelName)
                            .set("res_id", rec.getId())
                            .set("no_update", true));
                    }
                }
            } catch (Exception e) {
                String errMsg = Utils.isEmpty(column) ? String.format("第[%s]行发生错误", rowIdx)
                    : String.format("第[%s]行[%s]列的值[%s]发生错误", rowIdx, column, columnValue);
                onError.handle(errMsg, e);
            }
        }
    }

    List<Map<String, String>> readRows(InputStream input, ImportErrorHandle onError) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            CsvReadConfig csvReadConfig = new CsvReadConfig();
            // 跳过空白行
            csvReadConfig.setSkipEmptyRows(true);
            // 设置首行为标题行
            csvReadConfig.setContainsHeader(true);
            InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
            CsvReader csvReader = CsvUtil.getReader(csvReadConfig);
            CsvData csvRows = csvReader.read(isr);
            List<String> csvRowsHeader = csvRows.getHeader();
            List<CsvRow> rows = csvRows.getRows();

            int rowIdx = 0;
            for (CsvRow csvRow : rows) {
                String column = null;
                String value = null;
                try {
                    rowIdx++;
                    Map<String, String> map = new HashMap<>();
                    for (int j = 0; j < csvRowsHeader.size(); j++) {
                        column = csvRowsHeader.get(j);
                        value = csvRow.get(j);
                        if (null != csvRow.get(j)) {
                            map.put(csvRowsHeader.get(j), csvRow.get(j));
                        }
                    }
                    result.add(map);
                } catch (Exception e) {
                    String error = String.format("第[%s]行[%s]列的值[%s]发生错误", rowIdx, column, value);
                    onError.handle(error, e);
                }
            }
        } catch (Exception e) {
            onError.handle("读取csv文件失败", e);
        }
        return result;
    }

    private Object getFieldValue(Records record, MetaField field, String value, String module) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (!(field instanceof RelationalField)) {
            return value;
        }
        if (field instanceof Many2oneField) {
            //多对一 :demo.factory_id4
            if (!value.contains(".")) {
                value = module + "." + value;
            }
            Records refRec = record.getEnv().getRef(value);
            return refRec.getId();
        } else if (field instanceof Many2manyField || field instanceof One2manyField) {
            //多对多 :"[[4,ref(demo.m2m1)],[4,ref(demo.m2m2)],[4,ref(demo.m2m3)]]"
            String ref;
            Records refRec;
            for (Matcher m = refPattern.matcher(value); m.find(); value = value.replace(ref, "\"" + refRec.getId() + "\"")) {
                ref = m.group();
                String refVal = m.group("ref");
                if (!refVal.contains(".")) {
                    refVal = module + "." + refVal;
                }
                refRec = record.getEnv().getRef(refVal);
            }
            //单个值 base.company_default
            if (!value.contains("[") && !value.contains("ref")) {
                if (!value.contains(".")) {
                    value = module + "." + value;
                }
                Records oneRec = record.getEnv().getRef(value);
                return oneRec.getId();
            }
        }
        //一对多，直接处理执行
        return evalValue(value);
    }

    Object evalValue(String eval) {
        ObjectMapper map = new ObjectMapper();
        try {
            JsonNode node = map.readTree(eval);
            if (node.isBoolean()) {
                return node.asBoolean();
            } else if (node.isDouble()) {
                return node.asDouble();
            } else if (node.isInt()) {
                return node.asInt();
            } else if (node.isNull()) {
                return null;
            } else if (node.isTextual()) {
                return node.asText();
            } else {
                return node.isArray() ? map.readValue(eval, List.class) : map.readValue(eval, Map.class);
            }
        } catch (Exception e) {
            throw new ValueException(String.format("eval的参数[%s]解析失败", eval), e);
        }
    }
}
