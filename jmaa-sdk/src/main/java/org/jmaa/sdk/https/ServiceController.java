package org.jmaa.sdk.https;

import org.jmaa.sdk.https.jsonrpc.JsonRpcRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 服务控制器
 *
 * @author Eric Liang
 */
@org.springframework.stereotype.Controller
public class ServiceController extends Controller {

    @RequestMapping(value = "/*/rpc/service/**", method = RequestMethod.POST)
    @RequestHandler(auth = RequestHandler.AuthType.USER, type = RequestHandler.HandlerType.JSON)
    public Object execute(JsonRpcRequest request) {
        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/*/rpc/login/**", method = RequestMethod.POST)
    @RequestHandler(auth = RequestHandler.AuthType.NONE, type = RequestHandler.HandlerType.JSON)
    public Object login(String login, String password, boolean remember, boolean force) {
        throw new UnsupportedOperationException();
    }
}
