package org.jmaa.base.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.https.RequestHandler;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.TenantResolver;
import org.jmaa.sdk.util.ServerDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * HTTP控制器
 *
 * @author Eric Liang
 */
@org.springframework.stereotype.Controller
public class HttpController extends Controller {
    @Autowired
    TenantResolver tenantResolver;

    @RequestMapping(value = "/*/login", method = RequestMethod.GET)
    public void login(HttpServletRequest request, HttpServletResponse response) {
        getEnv().get("ir.http").call("login", request, response);
    }

    @RequestMapping(value = "/*", method = RequestMethod.GET)
    public void web(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            redirectToLogin();
        } else {
            env.get("ir.http").call("web", "base.web_app", request, response);
        }
    }

    @RequestMapping(value = "/*/pad", method = RequestMethod.GET)
    public void pad(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            redirectToLogin();
        } else {
            env.get("ir.http").call("web", "base.pad_app", request, response);
        }
    }

    @RequestMapping(value = "/*/m", method = RequestMethod.GET)
    public void mobile(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            redirectToLogin();
        } else {
            env.get("ir.http").call("web", "base.mobile_app", request, response);
        }
    }

    @RequestMapping(value = "/*/view", method = RequestMethod.GET)
    public void view(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            redirectToLogin();
        } else {
            env.get("ir.http").call("view", "base.web_view", request, response);
        }
    }

    @RequestMapping(value = "/*/m/view", method = RequestMethod.GET)
    public void mobileView(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            redirectToLogin();
        } else {
            env.get("ir.http").call("view", "base.mobile_view", request, response);
        }
    }

    @RequestMapping(value = "/*/home", method = RequestMethod.GET)
    public void home(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            redirectToLogin();
        } else {
            env.get("ir.http").call("view", "base.web_home", request, response);
        }
    }

    @RequestMapping(value = "/*/test", method = RequestMethod.GET)
    public void test(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        if (Utils.isEmpty(env.getUserId())) {
            redirectToLogin();
        } else {
            env.get("ir.http").call("test", request, response);
        }
    }

    @RequestMapping(value = "/*/server/time", method = RequestMethod.GET)
    public String time() {
        return Utils.format(new ServerDate(), "yyyy-MM-dd HH:mm:ss");
    }

    @RequestMapping(value = "/*/rpc/loginChangePassword", method = RequestMethod.POST)
    @RequestHandler(auth = RequestHandler.AuthType.NONE, type = RequestHandler.HandlerType.JSON)
    public Object changePassword(String login, String oldPassword, String newPassword) {
        return getEnv().get("rbac.user").call("loginChangePassword", oldPassword, newPassword, login);
    }
}
