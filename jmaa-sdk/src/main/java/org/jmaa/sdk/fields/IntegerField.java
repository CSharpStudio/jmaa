package org.jmaa.sdk.fields;

import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.tools.ObjectUtils;

/**
 * 整型
 *
 * @author Eric Liang
 */
public class IntegerField extends BaseField<IntegerField> {
    public IntegerField() {
        type = Constants.INTEGER;
        columnType = ColumnType.Integer;
    }

    @Related
    Integer max;
    @Related
    Integer min;
    @Related
    Integer gt;
    @Related
    Integer lt;

    public IntegerField max(Integer max) {
        args.put("max", max);
        return this;
    }

    public IntegerField min(Integer min) {
        args.put("min", min);
        return this;
    }

    /**
     * 大于
     *
     * @param gt
     * @return
     */
    public IntegerField greaterThen(Integer gt) {
        args.put("gt", gt);
        return this;
    }

    /**
     * 小于
     *
     * @param lt
     * @return
     */
    public IntegerField lessThen(Integer lt) {
        args.put("lt", lt);
        return this;
    }

    public Integer getMax() {
        return max;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getGt() {
        return gt;
    }

    public Integer getLt() {
        return lt;
    }

    @Override
    public Object convertToColumn(Object value, Records record, boolean validate) {
        return convertToCache(value, record, validate);
    }

    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (value == null || "".equals(value)) {
            if (validate && isRequired()) {
                throw new ValidationException(rec.l10n("%s 不能为空", getLabel()));
            }
            return null;
        }
        Integer num = ObjectUtils.toInt(value);
        if (max != null && num > max) {
            if (validate) {
                throw new ValidationException(rec.l10n("%s 不能大于最大值 %s", getLabel(), max));
            } else {
                num = max;
            }
        }
        if (min != null && num < min) {
            if (validate) {
                throw new ValidationException(rec.l10n("%s 不能小于最小值 %s", getLabel(), min));
            } else {
                num = min;
            }
        }
        return num;
    }

    @Override
    public Object convertToRecord(Object value, Records rec) {
        if (value == null || "".equals(value)) {
            return isRequired() ? 0 : null;
        }
        return ObjectUtils.toInt(value);
    }
}
