package jmaa.modules.print.controllers;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.util.FileInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Octopus
 * @date 2023/3/21
 * @apiNode
 */
@RestController
public class PrintTemplateController extends Controller {
    @RequestMapping(value = "/*/print/{id}", method = RequestMethod.GET)
    public void download(@PathVariable String id, HttpServletResponse response) {
        Records tpl = getEnv().get("print.template", id);
        List<Map> files = (List<Map>) tpl.get("file");
        if (!files.isEmpty()) {
            byte[] data = (byte[]) files.get(0).get("data");
            String fileName = tpl.getString("file_name");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            HttpUtils.writeData(response, data, "application/octet-stream");
        }
    }

    @RequestMapping(value = "/*/print/upload", method = RequestMethod.POST)
    public void uploadFile(@RequestParam("file") MultipartFile multipartFile,
                           @RequestParam("id") String templateId) throws IOException {
        Records tpl = getEnv().get("print.template", templateId);
        byte[] data = multipartFile.getBytes();
        String checkSum = IoUtils.computeChecksum(data);
        String rawSum = tpl.getString("check_sum");
        if (checkSum.equals(rawSum)) {
            return;
        }
        tpl.set("file_name", multipartFile.getOriginalFilename());
        tpl.set("file", new FileInfo() {{
            setName(multipartFile.getOriginalFilename())
                .setData(data);
        }});
        tpl.set("check_sum", checkSum);
        tpl.set("upload_status", true);
    }

    @RequestMapping(value = "/*/print/save/{id}", method = RequestMethod.POST)
    public void saveTemplate(@RequestParam("template") String template,
                             @RequestParam("fileName") String fileName,
                             @PathVariable("id") String id) {
        Records tpl = getEnv().get("print.template", id);
        byte[] data = template.getBytes(StandardCharsets.UTF_8);
        String checkSum = IoUtils.computeChecksum(data);
        String rawSum = tpl.getString("check_sum");
        if (checkSum.equals(rawSum)) {
            return;
        }
        tpl.set("file_name", fileName);
        tpl.set("file", new FileInfo() {{
            setName(fileName).setData(data);
        }});
        tpl.set("check_sum", checkSum);
        tpl.set("upload_status", true);
    }
}
