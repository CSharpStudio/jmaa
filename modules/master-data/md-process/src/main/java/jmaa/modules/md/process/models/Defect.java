package jmaa.modules.md.process.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.defect")
public class Defect extends Model {
    static Field process_ids = Field.Many2many("md.work_process", "md_work_process_defect", "defect_id", "process_id").label("工序");
}
