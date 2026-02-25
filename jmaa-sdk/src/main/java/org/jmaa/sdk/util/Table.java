package org.jmaa.sdk.util;

import org.jmaa.sdk.Utils;

import java.util.*;

public class Table implements Iterable<List<Object>> {

    List<List<Object>> data = new ArrayList<>();

    public void add(Object value, Object... values) {
        List<Object> row = new ArrayList<>(values.length + 1);
        row.add(value);
        for (Object v : values) {
            row.add(v);
        }
        data.add(row);
    }

    public List<Object> getColumn(int column) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            List<Object> row = data.get(i);
            if (row.size() > column) {
                values.add(data.get(i).get(column));
            } else {
                values.add(null);
            }
        }
        return values;
    }

    public List<Object> getRow(int row) {
        return data.get(row);
    }

    public String getString(int column, int row) {
        return Utils.toString(data.get(row).get(column));
    }

    public int getInt(int column, int row) {
        return Utils.toInt(data.get(row).get(column));
    }

    public long getLong(int column, int row) {
        return Utils.toLong(data.get(row).get(column));
    }

    public double getDouble(int column, int row) {
        return Utils.toDouble(data.get(row).get(column));
    }

    public Object get(int column, int row) {
        return data.get(row).get(column);
    }

    @Override
    public Iterator<List<Object>> iterator() {
        return new Iterator<List<Object>>() {
            int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < data.size();
            }

            @Override
            public List<Object> next() {
                return data.get(cursor++);
            }
        };
    }
}
