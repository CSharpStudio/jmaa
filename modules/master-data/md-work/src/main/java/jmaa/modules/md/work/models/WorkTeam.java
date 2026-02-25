package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.work_team", label = "班组")
public class WorkTeam extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field member_ids = Field.One2many("md.staff", "work_team_id").label("成员");
}
