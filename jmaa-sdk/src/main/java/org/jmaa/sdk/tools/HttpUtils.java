package org.jmaa.sdk.tools;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.https.ServerInfo;

import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Eric Liang
 */
public class HttpUtils {
    /**
     * 把 html 内容写入 response
     *
     * @param response
     * @param html
     */
    public static void writeHtml(HttpServletResponse response, String html) {
        response.setHeader("Content-type", "text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter pw = response.getWriter()) {
            pw.write(html);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("响应失败", e);
        }
    }

    /**
     * 把 data 数据写入 response
     *
     * @param response
     * @param data
     * @param contentType
     */
    public static void writeData(HttpServletResponse response, byte[] data, String contentType) {
        response.setHeader("Content-type", contentType);
        response.setHeader("Content-length", String.valueOf(data.length));
        response.setCharacterEncoding("UTF-8");
        try (ServletOutputStream outStream = response.getOutputStream()) {
            outStream.write(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("响应失败", e);
        }
    }

    /**
     * 获取请求的信息，用于日志分析
     *
     * @param request
     * @return
     */
    public static String getRequestInfo(HttpServletRequest request) {
        ServerInfo server = SpringUtils.getBean(ServerInfo.class);
        String info = Utils.format("server:%s:%s\nclient:%s\nurl:%s", server.getServerAddress(), server.getServerPort(), HttpUtils.getIpAddress(request), request.getRequestURL());
        String query = request.getQueryString();
        if (Utils.isNotEmpty(query)) {
            info += "?" + query;
        }
        Object json = request.getAttribute("JsonRpcRequest");
        if (Utils.isNotEmpty(json)) {
            info += "\nbody:" + json;
        }
        return info;
    }

    /**
     * 获取 cookie 值，处理 url 编码
     *
     * @param cookie
     * @return
     */
    public static String getCookieValue(Cookie cookie) {
        try {
            return java.net.URLDecoder.decode(cookie.getValue(), "UTF-8");
        } catch (Exception e) {
            return cookie.getValue();
        }
    }

    /**
     * 获取请求的 ip 地址，尝试多种方式解析
     *
     * @param request
     * @return
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 获取到多个ip时取第一个作为客户端真实ip
        if (StringUtils.isNotEmpty(ip) && ip.contains(",")) {
            String[] ipArray = ip.split(",");
            if (ArrayUtils.isNotEmpty(ipArray)) {
                ip = ipArray[0];
            }
        }
        return ip;
    }

    /**
     * 获取本机 ip
     *
     * @return
     */
    public static List<String> getIpAddress() {
        List<String> list = new ArrayList<>();
        try {
            Enumeration enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface network = (NetworkInterface) enumeration.nextElement();
                Enumeration addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = (InetAddress) addresses.nextElement();
                    if (address != null && address instanceof Inet4Address) {
                        String ip = address.getHostAddress();
                        if (!"127.0.0.1".equals(ip)) {
                            list.add(ip);
                        }
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return list;
    }
}
