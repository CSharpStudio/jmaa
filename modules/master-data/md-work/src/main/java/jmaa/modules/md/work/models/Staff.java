package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.staff")
public class Staff extends Model {
    static Field work_team_id = Field.Many2one("md.work_team").label("班组");
    static Field duty = Field.Selection(new Options() {{
        put("member", "成员");
        put("team-leader", "班组长");
    }}).label("职能").defaultValue("member");
}
