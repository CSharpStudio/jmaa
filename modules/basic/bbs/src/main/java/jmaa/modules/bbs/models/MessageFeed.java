package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.util.KvMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "bbs.message_feed", label = "消息流", logAccess = BoolState.False)
public class MessageFeed extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field notification_ids = Field.One2many("bbs.notification", "feed_id").label("通知");
    static Field last_message_id = Field.Many2one("bbs.message").label("最新的消息");
    static Field type = Field.Selection(new Options() {{
        put("follow", "关注");
    }}).label("名称").defaultValue("follow");
    static Field view = Field.Char().label("界面视图").defaultValue("form");
    static Field menu_id = Field.Many2one("ir.ui.menu").label("菜单").ondelete(DeleteMode.SetNull);

    @ServiceMethod(label = "读取通知推送", auth = Constants.ANONYMOUS)
    public Object loadFeed(Records records) {
        Environment env = records.getEnv();
        List<Map<String, Object>> result = new ArrayList<>();
        Cursor cr = env.getCursor();
        cr.execute("select count(id),feed_id from bbs_notification where feed_id is not null and user_id=%s and is_read=%s group by feed_id",
            Utils.asList(env.getUserId(), false));
        List<Object[]> rows = cr.fetchAll();
        Map<String, Object> map = rows.stream().collect(Collectors.toMap(r -> (String) r[1], r -> r[0]));
        Records feeds = records.browse(map.keySet());
        for (Records feed : feeds) {
            Records message = feed.getRec("last_message_id");
            if (message.any()) {
                Records menu = feed.getRec("menu_id");
                KvMap kv = new KvMap()
                    .set("id", feed.getId())
                    .set("name", feed.getString("name"))
                    .set("description", message.get("body"))
                    .set("model", message.getString("model"))
                    .set("res_id", message.getRec("res_id").getId())
                    .set("type", feed.getString("type"))
                    .set("view", feed.getString("view"))
                    .set("views", menu.getString("view"))
                    .set("menu", menu.l10n(menu.getString("name")))
                    .set("count", map.get(feed.getId()))
                    .set("last_dt", Utils.format(message.getDateTime("create_date"), "yyyy-MM-dd HH:mm:ss"));
                result.add(kv);
            }
        }
        result.sort((x, y) -> {
            String dt1 = (String) x.get("last_dt");
            String dt2 = (String) y.get("last_dt");
            if (Utils.isEmpty(dt1)) return 1;
            if (Utils.isEmpty(dt2)) return -1;
            return dt2.compareTo(dt1);
        });
        Records notification = env.get("bbs.notification");
        Criteria criteria = Criteria.equal("user_id", env.getUserId())
            .and("is_read", "=", false).and("feed_id", "=", null);
        long count = notification.count(criteria);
        if (count > 0) {
            notification = notification.find(criteria, 0, 1, "message_id desc");
            Records message = notification.getRec("message_id");
            KvMap kv = new KvMap()
                .set("name", env.l10n("消息通知"))
                .set("description", message.get("body"))
                .set("model", "bbs.message")
                .set("type", "notification")
                .set("menu", env.l10n("消息"))
                .set("view", "custom")
                .set("views", "custom")
                .set("count", count)
                .set("last_dt", Utils.format(message.getDateTime("create_date"), "yyyy-MM-dd HH:mm:ss"));
            result.add(kv);
        }
        List<Map<String, Object>> channels = (List<Map<String, Object>>) env.get("bbs.channel").call("fetchFeed");
        result.addAll(channels);
        return result;
    }

    @ServiceMethod(label = "标记为已读", auth = Constants.ANONYMOUS, doc = "按通知推送标记消息为已读")
    public Object markRead(Records records) {
        Records notification = records.getEnv().get("bbs.notification").find(Criteria.equal("user_id", records.getEnv().getUserId())
            .and("is_read", "=", false).and("feed_id", "in", records.getIds()));
        notification.set("is_read", true);
        return Action.success();
    }
}
