package org.jmaa.sdk.exceptions;

/**
 * 缺失异常
 *
 * @author Eric Liang
 */
public class MissingException extends PlatformException {

    public MissingException(Throwable cause) {
        super(cause);
    }

    public MissingException(String message) {
        super(message);
    }

    public MissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingException(int errorCode) {
        super(errorCode);
    }

    public MissingException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }

    public MissingException(String message, int errorCode) {
        super(message, errorCode);
    }

    public MissingException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }
}
