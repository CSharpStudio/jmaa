package org.jmaa.sdk;

import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.tools.*;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.lang.Nullable;

import java.text.ParseException;
import java.util.*;
import java.util.function.Supplier;

public class Utils {
//    public static void main(String[] args) {
//        System.out.println(StringUtils.join("a", "b"));
//        System.out.println(StringUtils.join(new Object[]{"a", "b"}));
//        System.out.println(StringUtils.join(Arrays.asList("a", "b")));
//        System.out.println(Arrays.asList("a", "b").stream().collect(Collectors.joining()));
//        System.out.println(format("a,%s,c,%s", "b", "d"));
//        System.out.println(format("a,{},c,{}", "b", "d"));
//        System.out.println(format("a,{0},c,{1}", "b", "d"));
//        System.out.println(format("{0,number,#.##}", 1.266));
//        System.out.println(format("{0},{},%s", "a", "b", "c"));
//        System.out.println(format("{0},{}", "a", "b"));
//    }

    /**
     * ObjectUtils
     */
    public static class Objects extends ObjectUtils {
    }

    /**
     * StringUtils
     */
    public static class Strings extends StringUtils {
    }

    /**
     * ArrayUtils
     */
    public static class Arrays extends ArrayUtils {
    }

    /**
     * DateUtils
     */
    public static class Dates extends DateUtils {
    }

    /**
     * NumberUtils
     */
    public static class Numbers extends NumberUtils {
    }


    public static Date addYears(Date date, int amount) {
        return DateUtils.addYears(date, amount);
    }

    public static Date addMonths(Date date, int amount) {
        return DateUtils.addMonths(date, amount);
    }

    public static Date addWeeks(Date date, int amount) {
        return DateUtils.addWeeks(date, amount);
    }

    public static Date addDays(Date date, int amount) {
        return DateUtils.addDays(date, amount);
    }

    public static Date addHours(Date date, int amount) {
        return DateUtils.addHours(date, amount);
    }

    public static Date addMinutes(Date date, int amount) {
        return DateUtils.addMinutes(date, amount);
    }

    public static Date addSeconds(Date date, int amount) {
        return DateUtils.addSeconds(date, amount);
    }

    public static Date addMilliseconds(Date date, int amount) {
        return DateUtils.addMilliseconds(date, amount);
    }

    public static Date parseDate(String str, String pattern) {
        try {
            return DateUtils.parseDate(str, (Locale) null, pattern);
        } catch (ParseException e) {
            throw new PlatformException(String.format("使用格式[%s]解析日期[%s]失败", pattern, str));
        }
    }

    /**
     * 返回由指定数组支持的列表。
     *
     * <pre>
     *     List&lt;String&gt; list = Utils.asList("a", "b", "c");
     * </pre>
     *
     * @param elements
     * @param <T>
     * @return
     */
    @SafeVarargs
    public static <T> List<T> asList(T... elements) {
        return ArrayUtils.asList(elements);
    }

    /**
     * 返回由指定数组支持的{@link HashSet}。
     * <pre>
     *     HashSet&lt;String&gt; set = Utils.hashSet("a", "b", "c");
     * </pre>
     *
     * @param items
     * @param <E>
     * @return
     */
    @SafeVarargs
    public static <E> HashSet<E> hashSet(E... items) {
        return items == null ? null : new HashSet<>(java.util.Arrays.asList(items));
    }

    /**
     * 检查指定的对象引用是否不为 null，如果为 null，则引发自定义的 {@link NullPointerException}。
     * 此方法主要用于在具有多个参数的方法和构造函数中进行参数验证，如下所示：
     * <pre>
     *   public Foo(Bar bar, Baz baz) {
     *       this.bar = Objects.requireNonNull(bar, "bar must not be null");
     *       this.baz = Objects.requireNonNull(baz, "baz must not be null");
     *   }
     * </pre>
     *
     * @param obj
     * @param message
     * @param <T>
     * @return
     */
    public static <T> T requireNotNull(T obj, String message) {
        return java.util.Objects.requireNonNull(obj, message);
    }

    /**
     * 检查指定的对象引用是否不为 null，如果为 null，则引发自定义的 {@link NullPointerException}。
     * 与方法 {@link Utils#requireNotNull(T, String)} 不同，此方法允许将消息的创建推迟到进行 null 检查之后。
     * 虽然这可能会在非 null 情况下带来性能优势，但在决定调用此方法时，应注意创建消息提供者的成本低于直接创建字符串消息的成本。
     *
     * @param obj
     * @param messageSupplier
     * @param <T>
     * @return
     */
    public static <T> T requireNotNull(T obj, Supplier<String> messageSupplier) {
        return java.util.Objects.requireNonNull(obj, messageSupplier);
    }

    /**
     * 使用正则表达式替换所有字符
     * <pre>
     *     replaceAll(null, "a", "b") -> null
     *     replaceAll("ab", null, "b") -> null
     *     replaceAll("ab", "a", null) -> null
     * </pre>
     *
     * @param str
     * @param regex
     * @param replacement
     * @return
     */
    public static String replaceAll(@Nullable String str, String regex, String replacement) {
        return RegExUtils.replaceAll(str, regex, replacement);
    }

    /**
     * 格式化字符串。支持%s、{}、{0}三种占位符，但不支持同时使用。
     * <pre>
     *     format(null) -> ""
     *     format("a,%s,c,%s", "b","d") -> a,b,c,d
     *     format("a,{},c,{}", "b","d") -> a,b,c,d
     *     format("a,{0},c,{1}", "b","d") -> a,b,c,d
     *     format("{0,number,#.##}", 1.266) -> 1.27
     *     format("{0},{},%s", "a","b","c") -> {0},{},a
     *     format("{0},{}", "a","b") -> {0},a
     * </pre>
     *
     * @param message
     * @param arguments
     * @return
     */
    public static String format(@Nullable String message, @Nullable Object... arguments) {
        return StringUtils.format(message, arguments);
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
    public static String format(Date date, String format) {
        return DateUtils.format(date, format);
    }

    /**
     * 拼接字符串
     * <pre>
     *     join(null) -> null
     *     join(Arrays.asList("a", "b")) -> a,b
     * </pre>
     *
     * @param items
     * @return
     */
    public static String join(Collection<String> items) {
        return StringUtils.join(items, ",");
    }

    /**
     * 拼接字符串
     * <pre>
     *     join(null) -> null
     *     join(new Object[]{"a", "b"}) -> a,b
     * </pre>
     *
     * @param arrays
     * @return
     */
    public static String join(Object[] arrays) {
        return StringUtils.join(arrays, ",");
    }

    /**
     * 拼接字符串
     * <pre>
     *     join(null, ",") -> null
     *     join(Arrays.asList("a", "b"), "|") -> a|b
     * </pre>
     *
     * @param strings
     * @param separator
     * @return
     */
    public static String join(Iterable<String> strings, String separator) {
        return StringUtils.join(strings, separator);
    }

    /**
     * 拼接字符串
     * <pre>
     *     join(null, ",") -> null
     *     join(new Object[]{"a", "b"}, "|") -> a|b
     * </pre>
     *
     * @param arrays
     * @param separator
     * @return
     */
    public static String join(Object[] arrays, String separator) {
        return StringUtils.join(arrays, ",");
    }

    /**
     * 比较两个对象是否相等。<br>
     * <pre>
     *      equals(null, null) true
     *      equals(new String[]{"1","2"}, new String[]{"1","2"}) true
     *      equals(new int[]{1,2}, new int[]{1,2}) true
     *      equals(Arrays.asList(1,2), Arrays.asList(1,2)) true
     * </pre>
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否相等
     */
    public static boolean equals(@Nullable Object obj1, @Nullable Object obj2) {
        return org.springframework.util.ObjectUtils.nullSafeEquals(obj1, obj2);
    }

    /**
     * 比较两个double是否相等, 精度0.000001d
     * <pre>
     *     equals(null, null) -> true
     *     equals(null, 0.1) -> false
     *     equals(1.000001, 1.0) -> true
     *     equals(1.000002, 1.0) -> false
     * </pre>
     *
     * @param d1
     * @param d2
     * @return
     */
    public static boolean equals(@Nullable Double d1, @Nullable Double d2) {
        return NumberUtils.equals(d1, d2);
    }

    /**
     * 保留6位小数
     *
     * @param d
     * @return
     */
    public static double round(@Nullable final double d) {
        return NumberUtils.round(d);
    }

    /**
     * 保存指定小数位
     *
     * @param d
     * @param scale
     * @return
     */
    public static double round(@Nullable final double d, int scale) {
        return NumberUtils.round(d, scale);
    }

    /**
     * 比较两个double大小, 精度0.000001d
     * <pre>
     *     less(1.0, 1.000001) -> false
     *     less(1.0, 1.000002) -> true
     * </pre>
     *
     * @param d1
     * @param d2
     * @return
     */
    public static boolean less(double d1, double d2) {
        return NumberUtils.less(d1, d2);
    }

    /**
     * 比较两个double大小, 精度0.000001d
     * <pre>
     *     lessOrEqual(1.0, 0.999998) -> false
     *     lessOrEqual(1.0, 0.999999) -> true
     *     lessOrEqual(1.0, 1.000001) -> true
     * </pre>
     *
     * @param d1
     * @param d2
     * @return
     */
    public static boolean lessOrEqual(double d1, double d2) {
        return NumberUtils.lessOrEqual(d1, d2);
    }

    /**
     * 比较两个double大小, 精度0.000001d
     * <pre>
     *     large(1.000001, 1.0) -> false
     *     large(1.000002, 1.0) -> true
     * </pre>
     *
     * @param d1
     * @param d2
     * @return
     */
    public static boolean large(double d1, double d2) {
        return NumberUtils.large(d1, d2);
    }

    /**
     * 比较两个double大小, 精度0.000001d
     * <pre>
     *     largeOrEqual(0.999998, 1.0) -> false
     *     largeOrEqual(0.999999, 1.0) -> true
     *     largeOrEqual(1.000001, 1.0) -> true
     * </pre>
     *
     * @param d1
     * @param d2
     * @return
     */
    public static boolean largeOrEqual(double d1, double d2) {
        return NumberUtils.largeOrEqual(d1, d2);
    }

    /**
     * 将Object转换为布尔值，
     * 如果转换失败，则返回false。
     * 如果对象为null，则返回false。
     *
     * <pre>
     *   toBoolean(null) -> false
     *   toBoolean("") -> false
     *   toBoolean("1") -> true
     *   toBoolean(1.1) -> true
     *   toBoolean("True") -> true
     *   toBoolean("Y") -> true
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>false</code> if
     * conversion fails
     */
    public static boolean toBoolean(Object o) {
        return ObjectUtils.toBoolean(o);
    }

    /**
     * 将Object转换为布尔值，
     * 如果转换失败，则返回默认值。
     * 如果对象为null，则返回默认值。
     *
     * <pre>
     *   toBoolean(null) -> false
     *   toBoolean("") -> false
     *   toBoolean("1") -> true
     *   toBoolean(1.1) -> true
     *   toBoolean("True") -> true
     *   toBoolean("Y") -> true
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static boolean toBoolean(@Nullable final Object o, boolean defaultValue) {
        return ObjectUtils.toBoolean(o, defaultValue);
    }

    /**
     * 将Object转换为长整型，
     * 如果转换失败，则返回0L。
     * 如果对象为null，则返回0L。
     *
     * <pre>
     *   toLong(null) -> 0
     *   toLong("") -> 0
     *   toLong("1") -> 1
     *   toLong(1.1) -> 1
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>zero</code> if
     * conversion fails
     */
    public static long toLong(@Nullable final Object o) {
        return ObjectUtils.toLong(o);
    }

    /**
     * 将Object转换为长整型，
     * 如果转换失败，则返回默认值。
     * 如果对象为null，则返回默认值。
     *
     * <pre>
     *   toLong(null, 1) -> 1
     *   toLong("", 1) -> 1
     *   toLong("1", 0) -> 1
     *   toLong(1.1, 0) -> 1
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static long toLong(@Nullable final Object o, final long defaultValue) {
        return ObjectUtils.toLong(o, defaultValue);
    }

    /**
     * 将Object转换为整型，
     * 如果转换失败，则返回0。
     * 如果对象为null，则返回0。
     *
     * <pre>
     *   toInt(null) -> 0
     *   toInt("") -> 0
     *   toInt("1") -> 1
     *   toInt(1.1) -> 1
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>zero</code> if
     * conversion fails
     */
    public static int toInt(@Nullable final Object o) {
        return ObjectUtils.toInt(o);
    }

    /**
     * 将Object转换为整型，
     * 如果转换失败，则返回默认值。
     * 如果对象为null，则返回默认值。
     *
     * <pre>
     *   toInt(null, 1) -> 1
     *   toInt("", 1) -> 1
     *   toInt("1", 0) -> 1
     *   toInt(1.1, 0) -> 1
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static int toInt(@Nullable final Object o, final int defaultValue) {
        return ObjectUtils.toInt(o, defaultValue);
    }

    /**
     * 将Object转换为双精度，
     * 如果转换失败，则返回0D。
     * 如果对象为null，则返回0D。
     *
     * <pre>
     *   toDouble(null) -> 0d
     *   toDouble("") -> 0d
     *   toDouble("1") -> 1d
     *   toDouble(1.1) -> 1.1
     * </pre>
     *
     * @param o the object to convert, may be null
     * @return the int represented by the object, or <code>zero</code> if
     * conversion fails
     */
    public static double toDouble(@Nullable final Object o) {
        return ObjectUtils.toDouble(o);
    }

    /**
     * 将Object转换为双精度，
     * 如果转换失败，则返回默认值。
     * 如果对象为null，则返回默认值。
     *
     * <pre>
     *   toDouble(null, 1d) -> 1d
     *   toDouble("", 1d) -> 1d
     *   toDouble("1", 0d) -> 1d
     *   toDouble(1.1, 0d) -> 1.1
     * </pre>
     *
     * @param o            the object to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the object, or the default if conversion fails
     */
    public static double toDouble(@Nullable final Object o, double defaultValue) {
        return ObjectUtils.toDouble(o, defaultValue);
    }

    /**
     * 序列化为JSON
     * <pre>
     *     toJsonString(null) -> ""
     * </pre>
     *
     * @param obj
     * @return
     */
    public static String toJsonString(@Nullable Object obj) {
        return ObjectUtils.toJsonString(obj);
    }

    /**
     * 转为字符串
     * <pre>
     *     toString(null) -> ""
     * </pre>
     *
     * @param obj
     * @return
     */
    public static String toString(@Nullable Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /**
     * 判断空对象 object、map、list、set、字符串、数组
     * <pre>
     *     isEmpty(null) -> true
     *     isEmpty("") -> true
     *     isEmpty(new int[]) -> true
     *     isEmpty(new ArrayList()) -> true
     *     isEmpty(new HashMap()) -> true
     *     isEmpty(new HashSet()) -> true
     * </pre>
     *
     * @param obj the object to check
     * @return 数组是否为空
     */
    public static boolean isEmpty(@Nullable Object obj) {
        return ObjectUtils.isEmpty(obj);
    }

    /**
     * 对象不为空 object、map、list、set、字符串、数组
     * <pre>
     *     isNotEmpty(null) -> false
     *     isNotEmpty("") -> false
     *     isNotEmpty(new int[]) -> false
     *     isNotEmpty(new ArrayList()) -> false
     *     isNotEmpty(new HashMap()) -> false
     *     isNotEmpty(new HashSet()) -> false
     * </pre>
     *
     * @param obj the object to check
     * @return 是否不为空
     */
    public static boolean isNotEmpty(@Nullable Object obj) {
        return ObjectUtils.isNotEmpty(obj);
    }

    public static Object ifEmpty(@Nullable Object obj, Object match, Object notMatch) {
        return ObjectUtils.isEmpty(obj) ? match : notMatch;
    }

    public static Object ifNotEmpty(@Nullable Object obj, Object match, Object notMatch) {
        return ObjectUtils.isNotEmpty(obj) ? match : notMatch;
    }

    /**
     * 判断是否为空字符串
     * <pre>
     *     isBlank(null) -> true
     *     isBlank("") -> true
     *     isBlank(" ") -> true
     *     isBlank("12345") -> false
     *     isBlank(" 12345 ") -> false
     * </pre>
     *
     * @param cs the {@code CharSequence} to check (may be {@code null})
     * @return {@code true} if the {@code CharSequence} is not {@code null},
     * its length is greater than 0, and it does not contain whitespace only
     * @see Character#isWhitespace
     */
    public static boolean isBlank(@Nullable final CharSequence cs) {
        return StringUtils.isBlank(cs);
    }

    /**
     * 判断不为空字符串
     * <pre>
     *      isNotBlank(null) -> false
     *      isNotBlank("") -> false
     *      isNotBlank(" ") -> false
     *      isNotBlank("bob") -> true
     *      isNotBlank("  bob  ") -> true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace
     * @see Character#isWhitespace
     */
    public static boolean isNotBlank(@Nullable final CharSequence cs) {
        return StringUtils.isNotBlank(cs);
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
        return ObjectUtils.getOrDefault(value, defaultValue);
    }
}
