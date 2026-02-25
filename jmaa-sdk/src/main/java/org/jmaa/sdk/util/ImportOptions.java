package org.jmaa.sdk.util;

import org.jmaa.sdk.Field;

public class ImportOptions {
    int headRow;

    public int getHeadRow() {
        return headRow;
    }

    public void setHeadRow(Integer head) {
        this.headRow = head;
    }

    public String getColumnName(Field field) {
        return field.getName();
    }
}
