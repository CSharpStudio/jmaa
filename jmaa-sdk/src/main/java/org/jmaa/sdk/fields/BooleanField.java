package org.jmaa.sdk.fields;

import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.tools.ObjectUtils;

/**
 * 布尔
 *
 * @author Eric Liang
 */
public class BooleanField extends BaseField<BooleanField> {
    public BooleanField() {
        columnType = ColumnType.Boolean;
        type = Constants.BOOLEAN;
    }

    @Override
    public Object convertToRecord(Object value, Records rec) {
        if (value == null) {
            return isRequired() ? false : null;
        }
        return ObjectUtils.toBoolean(value);
    }

    @Override
    public Object convertToColumn(Object value, Records record, boolean validate) {
        return convertToCache(value, record, validate);
    }

    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (value == null) {
            if (validate && isRequired()) {
                throw new ValidationException(rec.l10n("%s 不能为空", getLabel()));
            }
            return false;
        }
        return ObjectUtils.toBoolean(value);
    }

    @Override
    public Object convertToExport(Object value, Records rec) {
        return value;
    }
}
