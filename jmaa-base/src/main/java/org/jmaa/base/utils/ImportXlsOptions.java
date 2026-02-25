package org.jmaa.base.utils;

import com.alibaba.fastjson.annotation.JSONField;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.util.ImportOptions;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 导入选项
 *
 * @authorEric Liang
 */
public class ImportXlsOptions extends ImportOptions {
    String model;
    Map<String, Integer> fields;
    Integer sheetIndex;
    String importer;
    String parentField;
    String parentId;
    @JSONField(serialize = false, deserialize = false)
    MultipartFile file;
    Map<String, Object> args;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Map<String, Integer> getFields() {
        return fields;
    }

    public void setFields(Map<String, Integer> fields) {
        this.fields = fields;
    }

    public Integer getSheetIndex() {
        return sheetIndex;
    }

    public void setSheetIndex(Integer sheetIndex) {
        this.sheetIndex = sheetIndex;
    }

    public String getImporter() {
        return importer;
    }

    public void setImporter(String importer) {
        this.importer = importer;
    }

    public String getParentField() {
        return parentField;
    }

    public void setParentField(String parentField) {
        this.parentField = parentField;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    @Override
    public String getColumnName(Field field) {
        int index = fields.get(field.getName());
        char[] chars = new char[3];
        int pos = chars.length - 1;
        do {
            chars[pos--] = (char) ('A' + (index % 26));
            index = index / 26 - 1;
        } while (index >= 0);
        return new String(chars, pos + 1, chars.length - pos - 1);
    }
}
