package org.jmaa.sdk.fields;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;

import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.tools.*;

/**
 * @author Eric Liang
 */
public class Many2oneReferenceField extends CharField {
    @Related
    String modelField;

    /**
     * 构建{@link Many2oneReferenceField}实例
     */
    public Many2oneReferenceField() {
        type = Constants.MANY2ONE_REFERENCE;
        columnType = ColumnType.VarChar;
        sortable = false;
    }

    public Many2oneReferenceField(String modelField) {
        this();
        args.put("modelField", modelField);
    }

    public String getModelField() {
        return modelField;
    }

    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (value instanceof Records) {
            Records r = (Records) value;
            String[] ids = r.getIds();
            if (ids != null && ids.length > 0) {
                value = ids[0];
            } else {
                value = null;
            }
        }
        return super.convertToCache(value, rec, validate);
    }

    @Override
    public Object convertToRead(Object value, Records rec, boolean usePresent) {
        if (value == null) {
            return null;
        }
        Records r = (Records) value;
        if (usePresent && r.any()) {
            return r.getPresent();
        }
        return r.getId();
    }

    /**
     * 转成数据集
     *
     * @return
     */
    @Override
    public Object convertToRecord(Object value, Records rec) {
        if (rec.any()) {
            String model = rec.getString(modelField);
            if (StringUtils.isNotEmpty(model)) {
                String[] ids = StringUtils.isEmpty((String) value) ? ArrayUtils.EMPTY_STRING_ARRAY : new String[]{(String) value};
                return rec.getEnv().getRegistry().get(model).browse(rec.getEnv(), ids, null);
            }
        }
        return null;
    }
}
