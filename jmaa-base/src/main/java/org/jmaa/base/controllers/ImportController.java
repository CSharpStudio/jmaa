package org.jmaa.base.controllers;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSONObject;
import org.jmaa.base.models.ResUpload;
import org.jmaa.base.utils.ImportXlsOptions;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.exceptions.UserException;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.RequestHandler;
import org.jmaa.sdk.https.jsonrpc.JsonRpcError;
import org.jmaa.sdk.tools.HttpUtils;
import org.jmaa.sdk.tools.ThrowableUtils;
import org.jmaa.sdk.util.SecurityCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : eric
 **/
@org.springframework.stereotype.Controller
public class ImportController extends Controller {
    /**
     * 查询未完成的导入任务
     */
    @RequestMapping(value = "/*/upload/polling", method = RequestMethod.GET)
    @RequestHandler(auth = RequestHandler.AuthType.USER, type = RequestHandler.HandlerType.HTTP)
    public void polling(HttpServletRequest request, HttpServletResponse response) {
        Environment env = getEnv();
        String uid = env.getUserId();
        Cursor cr = env.getCursor();
        try {
            Map<String, Object> result = new HashMap<>();
            cr.execute("select count(1) from res_upload where state=%s and user_id=%s and upload_time>%s", Arrays.asList(ResUpload.RUNNING, uid, Utils.addHours(new Date(), -1)));
            result.put("running", Utils.toInt(cr.fetchOne()[0]));
            cr.execute("select id,title,message,success from res_upload where state=%s and user_id=%s", Arrays.asList(ResUpload.STOP, uid));
            List<Map<String, Object>> stop = cr.fetchMapAll();
            result.put("stop", stop);
            if (stop.size() > 0) {
                List<String> ids = stop.stream().map(r -> (String) r.get("id")).collect(Collectors.toList());
                cr.execute("update res_upload set state=%s where id in %s", Arrays.asList(ResUpload.FINISH, ids));
            }
            HttpUtils.writeData(response, JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8), "application/json");
        } catch (Exception e) {
            HttpUtils.writeHtml(response, "-1");
        }
    }

    /**
     * 导入Excel数据
     */
    @RequestMapping(value = "/*/importXls", method = RequestMethod.POST)
    @RequestHandler(auth = RequestHandler.AuthType.USER, type = RequestHandler.HandlerType.HTTP)
    @ResponseBody
    public Object importXls(@RequestParam("file") MultipartFile file, String options) {
        Environment env = getEnv();
        try (InputStream inputStream = file.getInputStream()) {
            ImportXlsOptions opt = JSONObject.parseObject(options, ImportXlsOptions.class);
            Records security = env.get("rbac.security");
            if (!env.isAdmin() && !(boolean) security.call("hasPermission", env.getUserId(), opt.getModel(), "importData")) {
                throw new AccessException("没有权限，请联系管理员分配权限", SecurityCode.NO_PERMISSION);
            }
            String importer = opt.getImporter();
            if (Utils.isEmpty(importer)) {
                importer = "ir.import";
            }
            opt.setFile(file);
            ExcelReader reader = ExcelUtil.getReader(inputStream, opt.getSheetIndex());
            String message = (String) env.get(importer).call("importXls", reader, opt);
            return new HashMap<String, Object>() {{
                put("success", true);
                put("message", message);
            }};
        } catch (Exception e) {
            env.getCursor().rollback();
            env.getToUpdate().clear();
            Throwable cause = ThrowableUtils.getCause(e);
            if (cause instanceof UserException) {
                UserException userError = (UserException) cause;
                return new JsonRpcError(userError.getErrorCode(), userError.getMessage(), e);
            }
            return JsonRpcError.createInternalError(e);
        }
    }
}



