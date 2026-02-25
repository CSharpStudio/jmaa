package org.jmaa.base.controllers;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import org.jmaa.sdk.Criteria;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.jsonrpc.JsonRpcResponse;
import org.jmaa.sdk.tools.HttpUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 附件控制器
 *
 * @author Eric Liang
 */
@org.springframework.stereotype.Controller
@RestController
public class AttachmentController extends Controller {
    @RequestMapping("/*/image/{model}/{field}/{id}")
    public void image(@PathVariable String model, @PathVariable String field, @PathVariable String id, HttpServletResponse response) {
        byte[] image = (byte[]) getEnv().get("ir.attachment").call("getFileDataByRes", model, field, id);
        if (Utils.isNotEmpty(image)) {
            HttpUtils.writeData(response, image, "image/png");
        } else {
            response.setStatus(404);
        }
    }

    @RequestMapping("/*/attachment/{id}")
    public void attachment(@PathVariable String id, HttpServletResponse response) throws UnsupportedEncodingException {
        Records attachment = getEnv().get("ir.attachment", id).exists();
        if (attachment.any()) {
            writeFile(attachment, response);
        } else {
            response.setStatus(404);
        }
    }

    @RequestMapping("/*/attachment/upload")
    public Object upload(@RequestParam("file") MultipartFile multipartFile,
                         @RequestParam(value = "res_model", required = false) String resModel,
                         @RequestParam(value = "res_field", required = false) String resField,
                         @RequestParam(value = "res_id", required = false) String resId) throws IOException {
        Object data = getEnv().get("ir.attachment").call("upload", multipartFile.getOriginalFilename(), multipartFile.getBytes(), resModel, resField, resId);
        JsonRpcResponse response = new JsonRpcResponse();
        response.setResult(data);
        return response;
    }

    @RequestMapping(value = "/*/assets/{code}", method = RequestMethod.GET)
    public void assets(@PathVariable String code, HttpServletResponse response) throws UnsupportedEncodingException {
        Records asset = getEnv().get("ir.assets").find(Criteria.equal("code", code).and("active", "=", true));
        if (asset.any()) {
            Map<String, Object> data = (Map<String, Object>) asset.call("getAsset");
            response.setCharacterEncoding("UTF-8");
            String fileName = java.net.URLEncoder.encode((String) data.get("fileName"), "UTF-8");
            response.setHeader("Content-Disposition", "filename=" + fileName);
            HttpUtils.writeData(response, (byte[]) data.get("content"), (String) data.get("contentType"));
        } else {
            response.setStatus(404);
        }
    }

    void writeFile(Records attachment, HttpServletResponse response) throws UnsupportedEncodingException {
        byte[] data = (byte[]) attachment.call("getFileData");
        String fileName = java.net.URLEncoder.encode(attachment.getString("file_name"), "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        final String contentType = ObjectUtil.defaultIfNull(FileUtil.getMimeType(attachment.get("file_name").toString()), "application/octet-stream");
        HttpUtils.writeData(response, data, contentType);
    }

    @RequestMapping(value = "/*/download/{code}", method = RequestMethod.GET)
    public void download(@PathVariable String code, HttpServletResponse response) throws UnsupportedEncodingException {
        Records download = getEnv().get("res.download").find(Criteria.equal("code", code).and("active", "=", true));
        if (download.any()) {
            Cursor cr = getEnv().getCursor();
            cr.execute("update res_download set count=count+1 where id=%s", Utils.asList(download.getId()));
            cr.commit();
            Records attachment = getEnv().get("ir.attachment").find(Criteria.equal("res_id", download.getId())
                .and("res_model", "=", "res.download").and("res_field", "=", "file"));
            if (attachment.any()) {
                writeFile(attachment, response);
                return;
            }
        }
        response.setStatus(404);
    }

    @RequestMapping(value = "/*/download/info/{code}", method = RequestMethod.GET)
    public void downloadVersion(@PathVariable String code, HttpServletResponse response) throws UnsupportedEncodingException {
        Records download = getEnv().get("res.download").find(Criteria.equal("code", code).and("active", "=", true));
        if (download.any()) {
            Map<String, Object> info = download.read(Utils.asList("code", "name", "version", "file")).get(0);
            info.remove("id");
            List<Map<String, Object>> files = (List<Map<String, Object>>) info.get("file");
            if (Utils.isNotEmpty(files)) {
                files.get(0).remove("id");
            }
            String json = Utils.toJsonString(info);
            HttpUtils.writeData(response, json.getBytes(StandardCharsets.UTF_8), "application/json");
            return;
        }
        response.setStatus(404);
    }
}
