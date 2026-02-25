package jmaa.modules.md.process.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.work_process_step", label = "工序过站步骤", authModel = "md.work_process", order = "seq")
public class WorkProcessStep extends Model {
    static Field process_id = Field.Many2one("md.work_process").ondelete(DeleteMode.Cascade).label("工序");
    static Field seq = Field.Integer().label("采集顺序");
    static Field code_type = Field.Selection(new Options() {{
        put("product", "产品条码");
        put("module", "组件条码");
        put("block", "拼板条码");
        put("container", "容器条码");
        put("batch", "批次条码");
        put("move", "过站条码");
    }}).required().label("条码类型");
    static Field is_move_code = Field.Boolean().label("过站条码").help("有多个步骤时，可指定过站条码，用于验证工序过站。如果没有指定，默认使用第一个条码过站");
    static Field bind_mode = Field.Selection(new Options() {{
        put("bind", "绑定");
        put("unbind", "解绑");
    }}).label("绑定/解绑").help("第一次使用时绑定，最后一次使用时解绑");
    static Field print = Field.Boolean().label("在线打印");
}
