package jmaa.modules.print.controllers;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.tools.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class PrintClientController {
    /**
     * 打印客户端升级下载
     */
    @RequestMapping(value = "/print/client/**", method = RequestMethod.GET)
    public void client(HttpServletRequest request, HttpServletResponse response) {
        String path = (String) SpringUtils.getProperty("print.client");
        if (Utils.isEmpty(path)) {
            path = PathUtils.combine(System.getProperty("user.dir"), "print");
        }
        String filePath = request.getServletPath();
        filePath = filePath.startsWith("/") ? filePath.substring(14) : path.substring(13);
        File file = new File(PathUtils.combine(path, filePath));
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                byte[] data = IoUtils.toByteArray(in);
                String fileName = PathUtils.getFileName(filePath);
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
                HttpUtils.writeData(response, data, "application/octet-stream");
            } catch (Exception e) {
                response.setStatus(500);
                HttpUtils.writeHtml(response, ThrowableUtils.getDebug(e));
            }
        } else {
            response.setStatus(404);
        }
    }
}
