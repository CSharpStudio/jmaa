package org.jmaa.sdk.exceptions;

/**
 * 访问异常
 *
 * @author Eric Liang
 */
public class AccessException extends UserException {

    public AccessException(Throwable cause) {
        super(cause);
    }

    public AccessException(String message) {
        super(message);
    }

    public AccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccessException(int errorCode) {
        super(errorCode);
    }

    public AccessException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }

    public AccessException(String message, int errorCode) {
        super(message, errorCode);
    }

    public AccessException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }
}
