package org.jmaa.sdk.https;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValueException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 控制器
 *
 * @author Eric Liang
 */
public class Controller {
    @Autowired
    protected HttpServletRequest httpServletRequest;
    @Autowired
    protected HttpServletResponse httpServletResponse;
    @Autowired
    TenantResolver tenantResolver;

    public Environment getEnv() {
        Environment result = Environment.envs.get();
        if (result == null) {
            throw new ValueException("env未初始化");
        }
        return result;
    }

    protected void redirectToLogin() {
        try {
            String path = tenantResolver.resolveTenantKey(httpServletRequest);
            String url = httpServletRequest.getRequestURI();
            String query = httpServletRequest.getQueryString();
            if (StringUtils.isNotEmpty(query)) {
                url += "?" + query;
            }
            httpServletResponse.sendRedirect("/" + path + "/login?url=" + url);
        } catch (Exception e) {
            // do nothing
        }
    }
}
