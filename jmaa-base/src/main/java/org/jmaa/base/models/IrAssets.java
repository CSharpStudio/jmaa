package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.util.KvMap;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "ir.assets", label = "资源文件")
public class IrAssets extends Model {
    static Field code = Field.Char().label("编码").unique().required();
    static Field name = Field.Char().label("名称").required();
    static Field type = Field.Selection(new Options() {{
        put("js", "Javascript");
        put("css", "CSS");
        put("json", "Json");
        put("svg", "SVG");
        put("image", "Image");
        put("mp4", "MP4");
        put("mp3", "MP3");
        put("file", "文件");
    }}).label("类型").required();
    static Field file = Field.Binary().label("文件").limit(1);
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field content = Field.Text().label("文件内容");
    static Field path = Field.Char().label("访问地址").compute("computePath");

    public String computePath(Records record) {
        return "/" + record.getEnv().getRegistry().getTenant().getKey() + "/assets/" + record.get("code");
    }

    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        for (Map<String, Object> row : valuesList) {
            String type = (String) row.get("type");
            if ("js".equals(type) || "css".equals(type) || "json".equals(type) || "svg".equals(type)) {
                row.remove("file");
            } else {
                row.remove("content");
            }
        }
        return (Records) callSuper(rec, valuesList);
    }

    public Map<String, Object> getAsset(Records record) {
        KvMap result = new KvMap();
        String type = record.getString("type");
        boolean isAttachment = false;
        if ("js".equals(type)) {
            result.put("contentType", "application/javascript");
            result.put("fileName", record.get("code") + ".js");
        } else if ("css".equals(type)) {
            result.put("contentType", "text/css");
            result.put("fileName", record.get("code") + ".css");
        } else if ("json".equals(type)) {
            result.put("contentType", "application/json");
            result.put("fileName", record.get("code") + ".json");
        } else if ("svg".equals(type)) {
            result.put("contentType", "image/svg+xml");
            result.put("fileName", record.get("code") + ".svg");
        } else if ("image".equals(type)) {
            result.put("contentType", "image/png");
            isAttachment = true;
        } else if ("mp4".equals(type)) {
            result.put("contentType", "video/mp4");
            isAttachment = true;
        } else if ("mp3".equals(type)) {
            result.put("contentType", "audio/mpeg");
            isAttachment = true;
        } else {
            result.put("contentType", "application/octet-stream");
            isAttachment = true;
        }
        if (isAttachment) {
            Records attachment = record.getEnv().get("ir.attachment").find(Criteria.equal("res_id", record.getId())
                .and("res_model", "=", "ir.assets").and("res_field", "=", "file"));
            byte[] data = (byte[]) attachment.call("getFileData");
            result.put("fileName", attachment.getString("file_name"));
            result.put("content", data);
        } else {
            result.put("content", record.getString("content").getBytes(StandardCharsets.UTF_8));
        }
        return result;
    }
}
