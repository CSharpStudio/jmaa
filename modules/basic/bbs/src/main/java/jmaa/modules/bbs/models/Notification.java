package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;

@Model.Meta(name = "bbs.notification", label = "消息通知", logAccess = BoolState.False)
public class Notification extends Model {
    static Field message_id = Field.Many2one("bbs.message").label("消息").index();
    static Field user_id = Field.Many2one("rbac.user").label("收件人").index().required().ondelete(DeleteMode.Cascade);
    static Field is_read = Field.Boolean().label("已读").defaultValue(false);
    static Field read_date = Field.DateTime().label("已读时间");
    static Field feed_id = Field.Many2one("bbs.message_feed").label("通知推送").index();

    public Object poll(Records records) {
        Cursor cr = records.getEnv().getCursor();
        cr.execute("select count(1),max(message_id) from bbs_notification where is_read=%s and user_id=%s", Utils.asList(false, records.getEnv().getUserId()));
        Object[] row = cr.fetchOne();
        long count = Utils.toLong(row[0]);
        String id1 = (String) row[1];
        String sql = "select count(distinct u.channel_id),max(m.id) from bbs_channel_user u " +
            "join bbs_message m on m.res_id=u.channel_id and m.id > u.seen_message_id " +
            "where u.user_id=%s";
        cr.execute(sql, Utils.asList(records.getEnv().getUserId()));
        row = cr.fetchOne();
        count += Utils.toLong(row[0]);
        String id2 = (String) row[1];
        if (Utils.isEmpty(id1) || Utils.isNotEmpty(id2) && id2.compareTo(id1) > 0) {
            id1 = id2;
        }
        return Utils.asList(count, id1);
    }
}
