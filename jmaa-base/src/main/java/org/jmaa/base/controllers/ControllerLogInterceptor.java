package org.jmaa.base.controllers;

import org.jmaa.base.utils.Jwt;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Eric Liang
 */
public class ControllerLogInterceptor implements AsyncHandlerInterceptor {
    private static Logger logger = LoggerFactory.getLogger(ControllerLogInterceptor.class);

    boolean isTrace(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if ("ctx_trace".equals(name)) {
                    try {
                        String value = HttpUtils.getCookieValue(cookie);
                        Jwt.validate(value);
                        return true;
                    } catch (Exception e) {
                        Cookie remove = new Cookie("ctx_trace", "");
                        remove.setMaxAge(0);
                        response.addCookie(remove);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            Object bean = method.getBean();
            if (bean instanceof Controller) {
                MDC.put("traceId", String.valueOf(SnowFlake.getSnowflakeId()));
                request.setAttribute("ticks", System.currentTimeMillis());
                Profiler.setTracing(isTrace(request, response));
            }
        }
        return AsyncHandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            Object bean = method.getBean();
            if (bean instanceof Controller) {
                long elapsed = System.currentTimeMillis() - Utils.toLong(request.getAttribute("ticks"));
                if (elapsed > 1000) {
                    logger.warn("request slow:{}ms\n{}", elapsed, HttpUtils.getRequestInfo(request));
                }
                if (ex != null) {
                    Throwable cause = ThrowableUtils.getCause(ex);
                    if (!(cause instanceof ValidationException)) {
                        logger.error("request error\n{}\nerror:{}", HttpUtils.getRequestInfo(request), ThrowableUtils.getDebug(ex));
                    }
                }
            }
        }
        MDC.clear();
        Profiler.clear();
        AsyncHandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
