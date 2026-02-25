package org.jmaa.sdk.exceptions;

/**
 * @author Eric Liang
 */
public class SqlConstraintException extends RuntimeException {
    String constraint;

    public String getConstraint() {
        return constraint;
    }

    public SqlConstraintException(String constraint) {
        super("违反约束:" + constraint);
        this.constraint = constraint;
    }
}
