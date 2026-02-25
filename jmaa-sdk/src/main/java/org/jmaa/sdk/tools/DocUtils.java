package org.jmaa.sdk.tools;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

import org.jmaa.sdk.fields.*;
import org.jmaa.sdk.core.MetaField;

/**
 * 工具杂项
 *
 * @author Eric Liang
 */
public class DocUtils {

    public static Object getExampleValue(MetaField field, boolean usePresent) {
        if (field instanceof BooleanField) {
            return true;
        } else if (field instanceof DateField) {
            return "2022-01-01";
        } else if (field instanceof DateTimeField) {
            return "2022-01-01 12:00:00";
        } else if (field instanceof FloatField) {
            return 0.1;
        } else if (field instanceof IdField) {
            return "01no6a1ocg9hc";
        } else if (field instanceof BinaryBaseField) {
            return "binary".getBytes();
        } else if (field instanceof IntegerField) {
            return 1;
        } else if (field instanceof Many2manyField || field instanceof One2manyField) {
            return Arrays.asList("id1", "id2");
        } else if (field instanceof Many2oneField) {
            if (usePresent) {
                return Arrays.asList("id", "name");
            }
            return "id";
        } else if (field instanceof SelectionField) {
            return "selection";
        } else if (field instanceof StringField) {
            return "str";
        }
        return "";
    }

    public static Object getExampleValue(Class<?> clazz) {
        if (clazz == String.class) {
            return "str";
        } else if (clazz == Boolean.class) {
            return true;
        } else if (clazz == Integer.class) {
            return 1;
        } else if (clazz == Float.class || clazz == Double.class) {
            return 0.1;
        } else if (clazz == Timestamp.class) {
            return "2022-01-01 12:00:00";
        } else if (clazz == Date.class) {
            return "2022-01-01";
        } else if (Collection.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz)
            || Set.class.isAssignableFrom(clazz) || clazz.isArray()) {
            return Collections.emptyList();
        } else if (Map.class.isAssignableFrom(clazz)) {
            return Collections.emptyMap();
        }
        return "";
    }

    public static String getJsonType(Class<?> clazz) {
        if (clazz == String.class) {
            return "字符串";
        } else if (clazz == Boolean.class) {
            return "布尔";
        } else if (clazz == Integer.class || clazz == Long.class || clazz == Float.class || clazz == Double.class) {
            return "数字";
        } else if (clazz == Timestamp.class) {
            return "日期时间";
        } else if (clazz == Date.class) {
            return "日期";
        } else if (Collection.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz)
            || Set.class.isAssignableFrom(clazz) || clazz.isArray()) {
            return "数组";
        }
        return "对象";
    }
}
