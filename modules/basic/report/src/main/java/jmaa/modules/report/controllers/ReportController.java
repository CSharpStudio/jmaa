package jmaa.modules.report.controllers;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.RequestHandler;
import org.jmaa.sdk.tools.HttpUtils;
import org.jmaa.sdk.tools.IoUtils;
import org.jmaa.sdk.util.ParamOut;
import org.jmaa.sdk.util.SecurityCode;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Map;

public class ReportController extends Controller {
    @RequestMapping(value = "/*/report", method = RequestMethod.GET)
    public void report(HttpServletResponse response) {
        InputStream input = IoUtils.getResourceAsStream("/jmaa/modules/report/statics/report.html");
        HttpUtils.writeData(response, IoUtils.toByteArray(input), "text/html;charset=UTF-8");
    }

    @RequestMapping(value = "/*/report/query", method = RequestMethod.POST)
    @RequestHandler(auth = RequestHandler.AuthType.NONE, type = RequestHandler.HandlerType.JSON)
    public Object query(String id, String dataset, Map<String, Object> params, Integer limit, Integer offset) {
        boolean isAdmin = getEnv().isAdmin();
        Records report = getEnv().get("rpt.report", id);
        if (!isAdmin && !(boolean) report.call("hasPermission")) {
            throw new AccessException("没有权限，请联系管理员分配权限", SecurityCode.NO_PERMISSION);
        }
        ParamOut out = new ParamOut();
        Object data = report.call("searchData", dataset, params, limit, offset);
        out.putData(data);
        String token = (String) getEnv().getContext().get("token");
        if (org.jmaa.sdk.tools.StringUtils.isNotEmpty(token)) {
            out.putContext("token", token);
        }
        return out.getResult();
    }
}
