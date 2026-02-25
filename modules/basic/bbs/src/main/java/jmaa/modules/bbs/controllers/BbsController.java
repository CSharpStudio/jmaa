package jmaa.modules.bbs.controllers;

import com.alibaba.fastjson.JSONObject;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.RequestHandler;
import org.jmaa.sdk.tools.HttpUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 通知
 *
 * @author eric
 */
@org.springframework.stereotype.Controller
@RestController
public class BbsController extends Controller {
    @RequestMapping(value = "/*/bbs/polling", method = RequestMethod.GET)
    @RequestHandler(auth = RequestHandler.AuthType.USER, type = RequestHandler.HandlerType.HTTP)
    public void polling(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        try {
            Object result = env.get("bbs.notification").call("poll");
            HttpUtils.writeData(response, JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8), "application/json");
        } catch (Exception e) {
            HttpUtils.writeData(response, "".getBytes(), "application/json");
        }
    }
}
