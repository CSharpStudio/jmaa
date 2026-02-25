package org.jmaa.sdk.fields;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.exceptions.ValueException;
import org.jmaa.sdk.util.ServerDate;

/**
 * 日期
 *
 * @author Eric Liang
 */
public class DateField extends BaseField<DateField> {
    public DateField() {
        type = Constants.DATE;
        columnType = ColumnType.Date;
    }

    @Related
    String format = "yyyy-MM-dd";

    public DateField format(String format) {
        args.put("format", format);
        return this;
    }

    @Override
    public Object convertToRecord(Object value, Records rec) {
        return toDate(value, rec);
    }

    /**
     * 转成本地日期
     *
     * @param value
     * @param rec
     * @param usePresent
     * @return
     */
    @Override
    public Object convertToRead(Object value, Records rec, boolean usePresent) {
        if (value == null) {
            return "";
        }
        Date dt = value instanceof ServerDate ? new Date(System.currentTimeMillis())
                : toDate(value, rec);
        SimpleDateFormat format = new SimpleDateFormat(this.format);
        return format.format(dt);
    }

    /**
     * 转成本地日期
     *
     * @param value
     * @param rec
     * @param validate
     * @return
     */
    @Override
    public Object convertToColumn(Object value, Records rec, boolean validate) {
        return convertToCache(value, rec, validate);
    }

    /**
     * 转成本地日期
     *
     * @param value
     * @param rec
     * @param validate
     * @return
     */
    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (value == null) {
            if (validate && isRequired()) {
                throw new ValidationException(rec.l10n("%s 不能为空", getLabel()));
            }
            return null;
        }
        if (value instanceof ServerDate) {
            return value;
        }
        return toDate(value, rec);
    }

    /**
     * 转成本地日期
     *
     * @param value
     * @param rec
     * @return
     */
    @Override
    public Object convertToWrite(Object value, Records rec) {
        return convertToCache(value, rec, true);
    }

    Date toDate(Object value, Records rec) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        try {
            if (value instanceof String) {
                SimpleDateFormat format = new SimpleDateFormat(this.format);
                return new Date(format.parse((String) value).getTime());
            }
            if (value instanceof java.util.Date) {
                return new Date(((java.util.Date) value).getTime());
            }
        } catch (Exception exc) {
            throw new ValueException(String.format("%s不是有效的日期，应为2023-01-01格式", value), exc);
        }
        throw new ValueException(String.format("%s不是有效的日期，应为2023-01-01格式", value));
    }
}
