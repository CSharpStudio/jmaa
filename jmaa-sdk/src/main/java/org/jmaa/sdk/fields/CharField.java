package org.jmaa.sdk.fields;

import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.DbColumn;
import org.jmaa.sdk.data.SqlDialect;

/**
 * 文本
 *
 * @author Eric Liang
 */
public class CharField extends StringField<CharField> {
    @Related
    int length = 240;
    @Related
    Boolean trim = true;

    public CharField() {
        type = Constants.CHAR;
        columnType = ColumnType.VarChar;
    }

    public int getLength() {
        return length;
    }

    /**
     * 是否去除空格，在web前端处理
     *
     * @return
     */
    public boolean getTrim() {
        if (trim == null) {
            trim = false;
        }
        return trim;
    }

    public CharField length(Integer length) {
        args.put("length", length);
        return this;
    }

    public CharField trim(Boolean trim) {
        args.put("trim", trim);
        return this;
    }

    @Override
    public String getDbColumnType(SqlDialect sqlDialect) {
        return sqlDialect.getColumnType(columnType, length, null);
    }

    @Override
    protected void updateDbColumn(Records model, DbColumn column) {
        // TODO the column's varchar length does not match self.length; convert it
        super.updateDbColumn(model, column);
    }

    @Override
    public Object convertToColumn(Object value, Records rec, boolean validate) {
        return convertToCache(value, rec, validate);
    }

    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (value == null || "".equals(value)) {
            if (validate && isRequired()) {
                throw new ValidationException(rec.l10n("%s 不能为空", getLabel()));
            }
            return null;
        }
        return toValidateString(value, rec, validate);
    }

    String toValidateString(Object value, Records rec, boolean validate) {
        String str = value.toString();
        if (str.length() > length) {
            if (validate) {
                String label = getLabel();
                if (StringUtils.isEmpty(label)) {
                    label = getName();
                }
                throw new ValidationException(rec.l10n("%s 长度%s超过最大长度%s", label, str.length(), length));
            }
            return str.substring(0, length);
        }
        return str;
    }
}
