package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.services.CountService;
import org.jmaa.sdk.services.ReadService;
import org.jmaa.sdk.services.SearchService;

/**
 * 上传进度
 *
 * @author eric
 */
@Model.Meta(name = "res.upload", label = "上传进度", logAccess = BoolState.False, order = "upload_time desc")

@Model.Service(remove = "@all")
@Model.Service(name = "search", label = "查询", auth = Constants.ANONYMOUS, description = "搜索并读取记录集指定字段的值", type = SearchService.class)
@Model.Service(name = "count", label = "计数", auth = Constants.ANONYMOUS, description = "统计匹配条件的记录数", type = CountService.class)
@Model.Service(name = "read", label = "查看",auth = Constants.ANONYMOUS, description = "读取记录集指定字段的值", type = ReadService.class)
public class ResUpload extends Model {
    /**
     * 执行中
     */
    public static final String RUNNING = "r";
    /**
     * 已停止执行
     */
    public static final String STOP = "s";
    /**
     * 通知用户后结束
     */
    public static final String FINISH = "f";
    static Field title = Field.Char().label("标题").length(1000);
    static Field message = Field.Char().label("信息").length(4000);
    static Field model = Field.Char().label("模型");
    static Field options = Field.Char().label("参数").length(2000);
    static Field user_id = Field.Many2one("rbac.user").label("上传者");
    static Field upload_time = Field.DateTime().label("上传时间");
    static Field stop_time = Field.DateTime().label("结束时间");
    static Field progress = Field.Integer().label("进度").defaultValue(0);
    static Field success = Field.Boolean().label("是否成功");
    static Field file = Field.Binary().label("文件").attachment(true);
    static Field state = Field.Selection(new Options() {{
        put(RUNNING, "进行中");
        put(STOP, "已停止");
        put(FINISH, "完成");
    }}).label("状态").defaultValue("r");
}
