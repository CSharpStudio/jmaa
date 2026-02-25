package org.jmaa.sdk.tools;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.exceptions.ValueException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 对象工具
 *
 * @author Eric Liang
 */
public class ObjectUtils extends org.apache.commons.lang3.ObjectUtils {

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj, Class<T> clazz) {
        return (T) obj;
    }

    @SuppressWarnings("unchecked")
    public static <T> T as(Object o, Class<T> tClass) {
        return tClass.isInstance(o) ? (T) o : null;
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>boolean</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, <code>false</code> is returned.</p>
     *
     * <pre>
     *   toBoolean(null) = false
     *   toBoolean("")   = false
     *   toBoolean("1")  = true
     *   toBoolean(1.1)  = true
     *   toBoolean("True")  = true
     *   toBoolean("Y")  = true
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>false</code> if
     * conversion fails
     */
    public static boolean toBoolean(Object o) {
        return toBoolean(o, false);
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>boolean</code>, returning a
     * default value if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, the default value is returned.</p>
     *
     * <pre>
     *   toBoolean(null) = false
     *   toBoolean("")   = false
     *   toBoolean("1")  = true
     *   toBoolean(1.1)  = true
     *   toBoolean("True")  = true
     *   toBoolean("Y")  = true
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static boolean toBoolean(@Nullable final Object o, boolean defaultValue) {
        if (o == null || "".equals(o)) {
            return defaultValue;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof Integer) {
            return 0 != (Integer) o;
        }
        if (o instanceof Long) {
            return 0L != (Long) o;
        }
        if (o instanceof Double) {
            return 0d != (Double) o;
        }
        if (o instanceof BigDecimal) {
            return !BigDecimal.ZERO.equals(o);
        }
        if (o instanceof String) {
            String str = (String) o;
            if ("1".equals(str) || "Y".equals(str)) {
                return true;
            }
            return Boolean.parseBoolean((String) o);
        }
        return false;
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>long</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, <code>zero</code> is returned.</p>
     *
     * <pre>
     *   toLong(null) = 0
     *   toLong("")   = 0
     *   toLong("1")  = 1
     *   toLong(1.1)  = 1
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>zero</code> if
     * conversion fails
     */
    public static long toLong(@Nullable final Object o) {
        return toLong(o, 0L);
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>long</code>, returning a
     * default value if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, the default value is returned.</p>
     *
     * <pre>
     *   toLong(null, 1) = 1
     *   toLong("", 1)   = 1
     *   toLong("1", 0)  = 1
     *   toLong(1.1, 0)  = 1
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static long toLong(@Nullable final Object o, final long defaultValue) {
        if (o == null || "".equals(o)) {
            return defaultValue;
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        if (o instanceof BigDecimal) {
            return ((BigDecimal) o).longValue();
        }
        try {
            return Long.valueOf(o.toString());
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>int</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, <code>zero</code> is returned.</p>
     *
     * <pre>
     *   toInt(null) = 0
     *   toInt("")   = 0
     *   toInt("1")  = 1
     *   toInt(1.1)  = 1
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>zero</code> if
     * conversion fails
     */
    public static int toInt(@Nullable final Object o) {
        return toInt(o, 0);
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>int</code>, returning a
     * default value if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, the default value is returned.</p>
     *
     * <pre>
     *   toInt(null, 1) = 1
     *   toInt("", 1)   = 1
     *   toInt("1", 0)  = 1
     *   toInt(1.1, 0)  = 1
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static int toInt(@Nullable final Object o, final int defaultValue) {
        if (o == null || "".equals(o)) {
            return defaultValue;
        }
        if (o instanceof Integer) {
            return (Integer) o;
        }
        if (o instanceof Long) {
            return ((Long) o).intValue();
        }
        if (o instanceof Double) {
            return ((Double) o).intValue();
        }
        if (o instanceof Float) {
            return ((Float) o).intValue();
        }
        if (o instanceof Short) {
            return ((Short) o).intValue();
        }
        if (o instanceof Byte) {
            return ((Byte) o).intValue();
        }
        if (o instanceof BigDecimal) {
            return ((BigDecimal) o).intValue();
        }
        try {
            return Integer.valueOf(o.toString());
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>double</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, <code>zero</code> is returned.</p>
     *
     * <pre>
     *   toDouble(null) = 0d
     *   toDouble("")   = 0d
     *   toDouble("1")  = 1d
     *   toDouble(1.1)  = 1.1
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>zero</code> if
     * conversion fails
     */
    public static double toDouble(@Nullable final Object o) {
        return toDouble(o, 0d);
    }

    /**
     * <p>Convert an <code>Object</code> to an <code>double</code>, returning a
     * default value if the conversion fails.</p>
     *
     * <p>If the object is <code>null</code>, the default value is returned.</p>
     *
     * <pre>
     *   toDouble(null, 1d) = 1d
     *   toDouble("", 1d)   = 1d
     *   toDouble("1", 0d)  = 1d
     *   toDouble(1.1, 0d)  = 1.1
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static double toDouble(@Nullable final Object o, double defaultValue) {
        if (o == null || "".equals(o)) {
            return defaultValue;
        }
        if (o instanceof Double) {
            return (double) o;
        }
        if (o instanceof Float) {
            return ((Float) o).doubleValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).doubleValue();
        }
        if (o instanceof Long) {
            return ((Long) o).doubleValue();
        }
        if (o instanceof Short) {
            return ((Short) o).doubleValue();
        }
        if (o instanceof Byte) {
            return ((Byte) o).doubleValue();
        }
        if (o instanceof BigDecimal) {
            return ((BigDecimal) o).doubleValue();
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * 字节数组转整型
     *
     * @param byt
     * @return
     */
    public static int toInt(byte[] byt) {
        return (int) (byt[0] & 0xff | (byt[1] & 0xff) << 8 | (byt[2] & 0xff) << 16 | (byt[3] & 0xff) << 24);
    }

    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "";
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new ValueException("转JSON字符串失败", e);
        }
    }

    public static Map<String, Object> fromJsonString(String json) {
        if (Utils.isEmpty(json)) {
            return Collections.emptyMap();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new ValueException("读取JSON字符串失败", e);
        }
    }

    /**
     * 整型转字节数组
     *
     * @param number
     * @return
     */
    public static byte[] toByteArray(int number) {
        byte[] byt = new byte[4];
        byt[0] = (byte) (number & 0xff);
        byt[1] = (byte) (number >> 8 & 0xff);
        byt[2] = (byte) (number >> 16 & 0xff);
        byt[3] = (byte) (number >> 24 & 0xff);
        return byt;
    }

    /**
     * 获取第一个不为空的值
     * <pre>
     *     firstNotEmpty("", null, "a") -> a
     * </pre>
     *
     * @param values
     * @param <T>
     * @return
     */
    public static <T> T firstNotEmpty(T... values) {
        for (T v : values) {
            if (isNotEmpty(v)) {
                return v;
            }
        }
        return null;
    }

    /**
     * 如果为空则返回默认值
     * <pre>
     *     getOrDefault("", "a") -> a
     *     getOrDefault(null, "a") -> a
     * </pre>
     *
     * @param value
     * @param defaultValue
     * @param <T>
     * @return
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return isNotEmpty(value) ? value : defaultValue;
    }
}
