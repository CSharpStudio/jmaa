package org.jmaa.sdk;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Eric Liang
 * @Date 2023/3/28 14:01
 * @Description
 */
public class SelectionValue {
    String value;
    String name;
    LinkedHashMap<String, String> map = new LinkedHashMap<>();

    /**
     * 选项值
     *
     * @param value 值
     * @param name  显示的名称
     */
    public SelectionValue(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public Map<String, String> getMap() {
        return map;
    }

    /**
     * 获取值
     *
     * @return
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取显示的名称
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 根据值获取显示的名称
     *
     * @param value
     * @return
     */
    public String getName(String value) {
        return map.get(value);
    }

    protected SelectionValue() {
        Class clazz = getClass();
        List<SelectionValue> values = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (SelectionValue.class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) {
                try {
                    f.setAccessible(true);
                    SelectionValue value = (SelectionValue) f.get(null);
                    values.add(value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        values.stream().forEach(v -> map.put(v.value, v.name));
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        return Objects.equals(value, other.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
