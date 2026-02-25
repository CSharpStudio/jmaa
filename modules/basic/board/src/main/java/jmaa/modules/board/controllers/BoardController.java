package jmaa.modules.board.controllers;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.core.BaseService;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.RequestHandler;
import org.jmaa.sdk.https.jsonrpc.JsonRpcError;
import org.jmaa.sdk.https.jsonrpc.JsonRpcParameter;
import org.jmaa.sdk.https.jsonrpc.JsonRpcRequest;
import org.jmaa.sdk.https.jsonrpc.JsonRpcResponse;
import org.jmaa.sdk.tools.HttpUtils;
import org.jmaa.sdk.tools.IoUtils;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;
import org.jmaa.sdk.util.SecurityCode;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
public class BoardController extends Controller {
    @RequestMapping(value = "/*/board", method = RequestMethod.GET)
    public void handler(HttpServletResponse response) {
        InputStream input = IoUtils.getResourceAsStream("/jmaa/modules/board/statics/board/index.html");
        HttpUtils.writeData(response, IoUtils.toByteArray(input), "text/html;charset=UTF-8");
    }

    @RequestMapping(value = "/*/rpc/board/{id}", method = RequestMethod.GET)
    public void read(@PathVariable String id, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            KvMap data = new KvMap().set("error", new KvMap().set("code", SecurityCode.UNAUTHORIZED).set("message", "身份验证失败"));
            HttpUtils.writeData(response, Utils.toJsonString(data).getBytes(StandardCharsets.UTF_8), "application/json;charset=UTF-8");
            return;
        }
        // 权限
        if (!env.isAdmin() && !"mom-board".equals(env.getUser().get("login"))) {
            Records security = env.get("rbac.security");
            if (!(boolean) security.call("hasPermission", env.getUserId(), "board.designer", "read")) {
                KvMap data = new KvMap().set("error", new KvMap().set("code", SecurityCode.NO_PERMISSION).set("message", "没有权限，请联系管理员分配权限"));
                HttpUtils.writeData(response, Utils.toJsonString(data).getBytes(StandardCharsets.UTF_8), "application/json;charset=UTF-8");
                return;
            }
        }
        Object data = id.startsWith("$") ? env.get("board.template", id.substring(1)).read(Utils.asList("content")).get(0) :
            env.get("board.designer", id).read(Utils.asList("content")).get(0);
        String json = Utils.toJsonString(data);
        HttpUtils.writeData(response, json.getBytes(StandardCharsets.UTF_8), "application/json;charset=UTF-8");
    }

    /**
     * 对mom-board用户不需要分配权限
     */
    @RequestMapping(value = "/*/rpc/board/**", method = RequestMethod.POST)
    @RequestHandler(auth = RequestHandler.AuthType.USER, type = RequestHandler.HandlerType.JSON)
    public Object service(JsonRpcRequest request) {
        Environment env = getEnv();
        JsonRpcParameter params = request.getParams();
        Map<String, Object> paramMap = params.getMap();
        ParamIn in = new ParamIn(env, (String) paramMap.get("model"), request.getMethod(), (Map<String, Object>) paramMap.get("args"));
        MetaModel meta = env.getRegistry().get(in.getModel());
        if (!meta.hasBase("board.service")) {
            throw new IllegalArgumentException(String.format("模型[%s]未继承[board.service]", in.getModel()));
        }
        BaseService svc = meta.findService(in.getService());
        if (svc == null) {
            throw new IllegalArgumentException(String.format("模型[%s]未定义服务[%s]", in.getModel(), in.getService()));
        }
        // 权限
        if (!Constants.ANONYMOUS.equals(meta.getAuthModel()) && !Constants.ANONYMOUS.equals(svc.getAuth())
            && !env.isAdmin() && !"mom-board".equals(env.getUser().get("login"))) {
            Records security = env.get("rbac.security");
            if (!(boolean) security.call("hasPermission", env.getUserId(), in.getModel(), svc.getAuth())) {
                throw new AccessException("没有权限，请联系管理员分配权限", SecurityCode.NO_PERMISSION);
            }
        }
        ParamOut out = new ParamOut();
        svc.executeService(in, out);
        if (!out.getErrors().isEmpty()) {
            return new JsonRpcResponse(request.getId(), new JsonRpcError(out.getErrorCode(), org.jmaa.sdk.tools.StringUtils.join(out.getErrors())));
        }
        if (!out.getResult().isEmpty()) {
            String token = (String) env.getContext().get("token");
            if (Utils.isNotEmpty(token)) {
                out.putContext("token", token);
            }
            return new JsonRpcResponse(request.getId(), out.getResult());
        }
        return new JsonRpcResponse(request.getId());
    }

    /**
     * 对mom-board用户不需要分配权限
     */
    @RequestMapping(value = "/*/rpc/board/{model}/{method}", method = RequestMethod.GET)
    public void service(@PathVariable String model, @PathVariable String method, HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            KvMap data = new KvMap().set("error", new KvMap().set("code", SecurityCode.UNAUTHORIZED).set("message", "身份验证失败"));
            HttpUtils.writeData(response, Utils.toJsonString(data).getBytes(StandardCharsets.UTF_8), "application/json;charset=UTF-8");
            return;
        }
        Map<String, Object> paramMap = new KvMap();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            paramMap.put(paramName, paramValue);
        }
        MetaModel meta = env.getRegistry().get(model);
        if (!meta.hasBase("board.service")) {
            throw new IllegalArgumentException(String.format("模型[%s]未继承[board.service]", model));
        }
        BaseService svc = meta.findService(method);
        if (svc == null) {
            throw new IllegalArgumentException(String.format("模型[%s]未定义服务[%s]", model, method));
        }
        // 权限
        if (!Constants.ANONYMOUS.equals(meta.getAuthModel()) && !Constants.ANONYMOUS.equals(svc.getAuth())
            && !env.isAdmin() && !"mom-board".equals(env.getUser().get("login"))) {
            Records security = env.get("rbac.security");
            if (!(boolean) security.call("hasPermission", env.getUserId(), model, svc.getAuth())) {
                KvMap data = new KvMap().set("error", new KvMap().set("code", SecurityCode.NO_PERMISSION).set("message", "没有权限，请联系管理员分配权限"));
                HttpUtils.writeData(response, Utils.toJsonString(data).getBytes(StandardCharsets.UTF_8), "application/json;charset=UTF-8");
                return;
            }
        }
        ParamIn in = new ParamIn(env, model, method, paramMap);
        ParamOut out = new ParamOut();
        svc.executeService(in, out);
        if (!out.getErrors().isEmpty()) {
            String errors = Utils.join(out.getErrors());
            KvMap data = new KvMap().set("error", new KvMap().set("code", 1000).set("message", errors));
            HttpUtils.writeData(response, Utils.toJsonString(data).getBytes(StandardCharsets.UTF_8), "application/json;charset=UTF-8");
        } else {
            String token = (String) env.getContext().get("token");
            if (Utils.isNotEmpty(token)) {
                out.putContext("token", token);
            }
            Map<String, Object> result = out.getResult();
            String json = Utils.toJsonString(result);
            HttpUtils.writeData(response, json.getBytes(StandardCharsets.UTF_8), "application/json;charset=UTF-8");
        }
    }
}
