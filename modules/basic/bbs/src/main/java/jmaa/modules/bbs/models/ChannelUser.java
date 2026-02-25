package jmaa.modules.bbs.models;

import org.jmaa.sdk.DeleteMode;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "bbs.channel_user", label = "频道成员")
@Model.UniqueConstraint(name = "channel_user_unique", fields = {"user_id", "channel_id"})
public class ChannelUser extends Model {
    static Field user_id = Field.Many2one("rbac.user").label("用户").ondelete(DeleteMode.Cascade).index();
    static Field channel_id = Field.Many2one("bbs.channel").label("频道");
    static Field custom_channel_name = Field.Char().label("自定义频道名称");
    static Field seen_message_id = Field.Many2one("bbs.message").label("最后看见的消息").defaultValue("!!!empty");
    static Field is_pinned = Field.Boolean().defaultValue(true);
}
