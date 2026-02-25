package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.work_resource")
public class WorkResource extends Model {
    static Field calendar_id = Field.Many2one("md.work_calendar").label("工作日历").defaultValue(Default.method("defaultCalendar"));

    public Object defaultCalendar(Records record) {
        return record.getEnv().get("md.work_calendar").find(Criteria.equal("is_default", "true")).firstOrDefault().getId();
    }
}
