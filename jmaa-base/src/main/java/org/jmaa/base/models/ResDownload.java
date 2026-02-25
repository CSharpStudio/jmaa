package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;

import java.util.List;
import java.util.Map;

@Model.Meta(name = "res.download", label = "下载")
public class ResDownload extends Model {
    static Field code = Field.Char().label("编码").unique().required();
    static Field name = Field.Char().label("名称").required();
    static Field version = Field.Char().label("版本").required().defaultValue("1.0");
    static Field image = Field.Image().label("图标").help("上传文件保存后，根据访问地址生成下载二维码");
    static Field file = Field.Binary().label("文件").limit(1);
    static Field count = Field.Integer().label("下载次数").defaultValue(0);
    static Field active = Field.Boolean().label("是否生效");
    static Field path = Field.Char().label("访问地址").compute("computePath");

    public String computePath(Records record) {
        return "/" + record.getEnv().getRegistry().getTenant().getKey() + "/download/" + record.get("code");
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "读取下载信息")
    public List<Map<String, Object>> readDownload(Records records, List<String> fields) {
        return records.find(Criteria.equal("active", true)).read(fields);
    }
}
