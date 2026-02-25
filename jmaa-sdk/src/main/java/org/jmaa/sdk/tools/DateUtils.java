package org.jmaa.sdk.tools;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.jmaa.sdk.exceptions.ValueException;
import org.springframework.lang.Nullable;

/**
 * @author Eric Liang
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    public static java.sql.Date atZone(@Nullable java.sql.Date date, TimeZone zone) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dt = format.format(date);
        format.setTimeZone(zone);
        try {
            return new java.sql.Date(format.parse(dt).getTime());
        } catch (Exception exc) {
            throw new ValueException("时区转换失败", exc);
        }
    }

    public static Timestamp atZone(Timestamp date, TimeZone zone) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dt = format.format(date);
        format.setTimeZone(zone);
        try {
            return new Timestamp(format.parse(dt).getTime());
        } catch (Exception exc) {
            throw new ValueException("时区转换失败", exc);
        }
    }

    public static java.util.Date atZone(@Nullable java.util.Date date, TimeZone zone) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dt = format.format(date);
        format.setTimeZone(zone);
        try {
            return format.parse(dt);
        } catch (Exception exc) {
            throw new ValueException("时区转换失败", exc);
        }
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public static java.sql.Date toUTC(@Nullable java.sql.Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dt = format.format(date);
        format.setTimeZone(TimeZone.getDefault());
        try {
            return new java.sql.Date(format.parse(dt).getTime());
        } catch (Exception exc) {
            throw new ValueException("时区转换失败", exc);
        }
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public static Timestamp toUTC(@Nullable Timestamp date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dt = format.format(date);
        format.setTimeZone(TimeZone.getDefault());
        try {
            return new Timestamp(format.parse(dt).getTime());
        } catch (Exception exc) {
            throw new ValueException("时区转换失败", exc);
        }
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public static java.util.Date toUTC(@Nullable java.util.Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dt = format.format(date);
        format.setTimeZone(TimeZone.getDefault());
        try {
            return format.parse(dt);
        } catch (Exception exc) {
            throw new ValueException("时区转换失败", exc);
        }
    }

    /**
     * 使用SimpleDateFormat格式化日期
     * <pre>
     *     format(null, "") -> null
     *     format(new Date(), null) -> null
     *     format(new Date(), ""yyyy-MM-dd HH:mm:ss"") -> 2024-12-12 12:00:00
     * </pre>
     *
     * @param date
     * @param format
     * @return
     */
    public static String format(@Nullable java.util.Date date, @Nullable String format) {
        if (date == null || format == null) {
            return "";
        }
        SimpleDateFormat f = new SimpleDateFormat(format);
        return f.format(date);
    }

    /**
     * 创建指定年、月、日的Date对象
     *
     * @param year  年份（如：2023）
     * @param month 月份（1-12）
     * @param day   日期（1-31，需符合当月天数）
     * @return 对应的Date对象
     */
    public static Date createDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 创建指定年、月、日的Date对象
     *
     * @param year  年份（如：2023）
     * @param month 月份（1-12）
     * @param day   日期（1-31，需符合当月天数）
     * @return 对应的Date对象
     */
    public static Date createDate(int year, int month, int day, int hours, int minutes, int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
