package org.jmaa.sdk.https.jsonrpc.exceptions;

/**
 * json rpc问题
 *
 * @author Eric Liang
 */
public class JsonRpcException extends RuntimeException {

    public JsonRpcException() {
    }

    public JsonRpcException(Throwable cause) {
        super(cause);
    }

    public JsonRpcException(String message) {
        super(message);
    }

    public JsonRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
