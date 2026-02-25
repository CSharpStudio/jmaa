package org.jmaa.sdk.fields;

import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.tools.StringUtils;

/**
 * 小数
 *
 * @author Eric Liang
 */
public class FloatField extends BaseField<FloatField> {

    @Related
    String digits;
    @Related
    Double max;
    @Related
    Double min;
    @Related
    Double gt;
    @Related
    Double lt;

    @Related
    int scale = 7;

    /**
     * 最大值(小于等于)
     *
     * @param max
     * @return
     */
    public FloatField max(Double max) {
        args.put("max", max);
        return this;
    }

    /**
     * 最小值(大于等于)
     *
     * @param min
     * @return
     */
    public FloatField min(Double min) {
        args.put("min", min);
        return this;
    }

    /**
     * 精度
     *
     * @param scale
     * @return
     */
    public FloatField scale(int scale) {
        args.put("scale", scale);
        return this;
    }

    /**
     * 大于
     *
     * @param gt
     * @return
     */
    public FloatField greaterThen(Double gt) {
        args.put("gt", gt);
        return this;
    }

    /**
     * 小于
     *
     * @param lt
     * @return
     */
    public FloatField lessThen(Double lt) {
        args.put("lt", lt);
        return this;
    }

    public Double getMax() {
        return max;
    }

    public Double getMin() {
        return min;
    }

    public Double getGt() {
        return gt;
    }

    public Double getLt() {
        return lt;
    }

    public FloatField() {
        type = Constants.FLOAT;
        columnType = ColumnType.Float;
    }

    /**
     * 数据库小数位
     *
     * @param digits
     * @return
     */
    public FloatField digits(String digits) {
        args.put("digits", digits);
        return this;
    }

    @Override
    public String getDbColumnType(SqlDialect sqlDialect) {
        if (StringUtils.isNotBlank(digits)) {
            // TODO
        }
        return super.getDbColumnType(sqlDialect);
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
        Double num = ObjectUtils.toDouble(value);
        num = org.apache.commons.math3.util.Precision.round(num, scale);
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
            return isRequired() ? 0d : null;
        }
        return ObjectUtils.toDouble(value);
    }
}
