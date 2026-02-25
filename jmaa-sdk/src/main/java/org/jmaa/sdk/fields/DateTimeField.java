package org.jmaa.sdk.fields;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.util.ServerDate;

/**
 * 日期时间
 *
 * @author Eric Liang
 */
public class DateTimeField extends BaseField<DateTimeField> {
    public DateTimeField() {
        type = Constants.DATETIME;
        columnType = ColumnType.DateTime;
    }

    @Related
    String format = "yyyy-MM-dd HH:mm:ss";

    public DateTimeField format(String format) {
        args.put("format", format);
        return this;
    }

    @Override
    public Object convertToRecord(Object value, Records rec) {
        return toTimestamp(value, rec);
    }

    /**
     * 转为用户的时区
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
        Timestamp ts = value instanceof ServerDate ? new Timestamp(System.currentTimeMillis())
                : toTimestamp(value, rec);
        SimpleDateFormat format = new SimpleDateFormat(this.format);
        format.setTimeZone(getZone(rec));
        return format.format(ts);
    }

    /**
     * 转成UTC时间存储
     *
     * @param value
     * @param rec
     * @param validate
     * @return
     */
    @Override
    public Object convertToColumn(Object value, Records rec, boolean validate) {
        if (value == null) {
            if (validate && isRequired()) {
                throw new ValidationException(rec.l10n("%s 不能为空", getLabel()));
            }
            return null;
        }
        if (value instanceof ServerDate) {
            return value;
        }
        Timestamp ts = toTimestamp(value, rec);
        SqlDialect sd = rec.getEnv().getCursor().getSqlDialect();
        return sd.toDbTimestamp(ts);
    }

    /**
     * 转成服务器时区的本地日期时间
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
        return toTimestamp(value, rec);
    }

    /**
     * 转成服务器时区的本地日期时间
     *
     * @param value
     * @param rec
     * @return
     */
    @Override
    public Object convertToWrite(Object value, Records rec) {
        return convertToCache(value, rec, true);
    }

    Timestamp toTimestamp(Object value, Records rec) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        try {
            if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
                SimpleDateFormat format = new SimpleDateFormat(this.format);
                format.setTimeZone(getZone(rec));
                return new Timestamp(format.parse((String) value).getTime());
            }
            if (value instanceof Date) {
                return new Timestamp(((Date) value).getTime());
            }
        } catch (Exception exc) {
            throw new ValidationException(rec.l10n("%s不是有效的日期时间，应为2023-01-01 12:00:00格式", value), exc);
        }
        throw new ValidationException(rec.l10n("%s不是有效的日期时间，应为2023-01-01 12:00:00格式", value));
    }

    TimeZone getZone(Records rec) {
        String tz = (String) rec.getEnv().getTimezone();
        return TimeZone.getTimeZone(tz);
    }
}
