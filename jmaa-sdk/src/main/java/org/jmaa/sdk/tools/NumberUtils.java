package org.jmaa.sdk.tools;

import org.apache.commons.math3.util.Precision;
import org.springframework.lang.Nullable;

public class NumberUtils extends org.apache.commons.lang3.math.NumberUtils {

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
        if (d1 == d2) {
            return true;
        }
        if (d1 == null || d2 == null) {
            return false;
        }
        return Precision.equals(d1, d2, 0.000001d);
    }

    /**
     * 保留6位小数
     *
     * @param d
     * @return
     */
    public static double round(@Nullable final double d) {
        return Precision.round(d, 6);
    }

    /**
     * 保存指定小数位
     *
     * @param d
     * @param scale
     * @return
     */
    public static double round(@Nullable final double d, int scale) {
        return Precision.round(d, scale);
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
        return Precision.compareTo(d1, d2, 0.000001d) < 0;
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
        return Precision.compareTo(d1, d2, 0.000001d) <= 0;
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
        return Precision.compareTo(d1, d2, 0.000001d) > 0;
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
        return Precision.compareTo(d1, d2, 0.000001d) >= 0;
    }
}
