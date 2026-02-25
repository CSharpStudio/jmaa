package org.jmaa.sdk.https.jsonrpc.exceptions;

/**
 * 参数问题
 *
 * @author Eric Liang
 */
public class JsonRpcInvalidParamsException extends JsonRpcException {

    public JsonRpcInvalidParamsException() {
    }

    public JsonRpcInvalidParamsException(Throwable cause) {
        super(cause);
    }

    public JsonRpcInvalidParamsException(String message) {
        super(message);
    }

    public JsonRpcInvalidParamsException(String message, Throwable cause) {
        super(message, cause);
    }

}
