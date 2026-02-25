package org.jmaa.sdk.https.jsonrpc.exceptions;

/**
 * 解析问题
 *
 * @author Eric Liang
 */
public class JsonRpcParseException extends JsonRpcException {

    public JsonRpcParseException() {
    }

    public JsonRpcParseException(Throwable cause) {
        super(cause);
    }

    public JsonRpcParseException(String message) {
        super(message);
    }

    public JsonRpcParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
