package org.jmaa.web.controllers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.tools.IoUtils;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.tools.StringUtils;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 静态资源
 *
 * @author Eric Liang
 */
@org.springframework.stereotype.Controller
public class WebController {
    @RequestMapping(value = "/web/**", method = RequestMethod.GET)
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        String etag = request.getParameter("etag");
        String path = request.getServletPath();
        path = path.startsWith("/") ? path.substring(5) : path.substring(4);
        if (Utils.isNotEmpty(etag)) {
            response.setHeader("Etag", etag);
            String tag = request.getHeader("If-None-Match");
            if (Utils.equals(tag, etag)) {
                response.setStatus(304);
                return;
            }
        } else if (isCache(path) && !isDebug(request)) {
            response.setHeader("Etag", getEtag());
            String tag = request.getHeader("If-None-Match");
            if (getEtag().equals(tag)) {
                response.setStatus(304);
                return;
            }
        }
        Optional<MediaType> o = MediaTypeFactory.getMediaType(path);
        if (o.isPresent()) {
            response.setContentType(o.get().toString());
        }
        response.setCharacterEncoding("UTF-8");
        InputStream input = IoUtils.getResourceAsStream(path);
        if (input != null) {
            try (OutputStream output = response.getOutputStream()) {
                IoUtils.stream(input, output);
                input.close();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("响应失败", e);
            }
        } else {
            response.setStatus(404);
        }
    }

    static String eTag;

    public static String getEtag() {
        if (eTag == null) {
            try {
                Properties properties = PropertiesLoaderUtils.loadAllProperties("maven.properties");
                eTag = properties.getProperty("maven.buildTime");
            } catch (Throwable e) {
            }
            if (StringUtils.isEmpty(eTag)) {
                eTag = UUID.randomUUID().toString();
            }
        }
        return eTag;
    }

    public static boolean isDebug(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ctx_debug".equals(cookie.getName())) {
                    return ObjectUtils.toBoolean(cookie.getValue());
                }
            }
        }
        return false;
    }

    public static boolean isCache(String path) {
        return true;
    }
}
