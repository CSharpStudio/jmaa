package org.jmaa.sdk.fields;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;

/**
 * 大文本
 *
 * @author Eric Liang
 */
public class TextField extends StringField<TextField> {
    public TextField() {
        type = Constants.TEXT;
        columnType = ColumnType.Text;
    }

    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
