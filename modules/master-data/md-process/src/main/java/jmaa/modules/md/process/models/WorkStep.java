package jmaa.modules.md.process.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.work_step", label = "工步", authModel = "md.work_process")
public class WorkStep extends Model {
    static Field process_id = Field.Many2one("md.work_process").ondelete(DeleteMode.Cascade).label("工序");
    static Field seq = Field.Integer().label("顺序");
    static Field name = Field.Char().label("工步").required();
    static Field description = Field.Char().label("描述");
}
