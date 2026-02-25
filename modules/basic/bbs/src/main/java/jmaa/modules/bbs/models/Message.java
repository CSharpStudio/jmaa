package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.Cursor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author eric
 */

@Model.Meta(name = "bbs.message", label = "消息")
@Model.Service(remove = "@all")
public class Message extends Model {
    static Field subject = Field.Char().label("标题");
    static Field body = Field.Html().label("内容");
    static Field description = Field.Char().label("摘要").help("消息摘要，主题或者内容的开头").compute("computeDescription");
    static Field model = Field.Char().label("模型");
    static Field res_id = Field.Many2oneReference("model").label("模型");
    static Field message_type = Field.Selection(new Options() {{
        put("comment", "评论");
        put("notification", "系统通知");
        put("user_notification", "用户特定通知");
    }}).label("类型").required().defaultValue("comment");
    static Field author_id = Field.Many2one("rbac.user").label("发件人").index().ondelete(DeleteMode.SetNull);
    static Field recipient_ids = Field.Many2many("rbac.user", "bbs_message_recipient", "message_id", "user_id").label("收件人");
    static Field tracking_values_ids = Field.One2many("bbs.tracking_value", "message_id").label("跟踪值");
    static Field tracking_values = Field.Object().compute("computeTrackingValues").label("跟踪值");
    static Field notification_ids = Field.One2many("bbs.notification", "message_id").label("通知");
    static Field author_image = Field.Image().related("author_id.image");

    public Object computeTrackingValues(Records record) {
        return record.getRec("tracking_values_ids").call("trackingValueFormat");
    }

    public String computeDescription(Records record) {
        String subject = record.getString("subject");
        if (Utils.isNotEmpty(subject)) {
            return subject;
        }
        String body = record.getString("body");
        if (Utils.isNotEmpty(body) && body.length() > 30) {
            return body.substring(0, 30) + "...";
        }
        return body;
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "标记为已读")
    public void markRead(Records records) {
        Cursor cr = records.getEnv().getCursor();
        cr.execute("update bbs_notification set is_read=%s where message_id in %s and user_id=%s",
            Utils.asList(true, records.getIds(), records.getEnv().getUserId()));
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "读取消息", ids = false)
    public Map<String, Object> searchMessage(Records rec, Criteria criteria, List<String> fields, int limit, int offset, String order) {
        criteria.and(Criteria.equal("user_id", rec.getEnv().getUserId()));
        Integer size = limit + 1;
        Records notification = rec.getEnv().get("bbs.notification").find(criteria, offset, size, order);
        List<String> messageIds = notification.stream().map(r -> r.getRec("message_id").getId()).collect(Collectors.toList());
        Records messages = rec.browse(messageIds);
        List<Map<String, Object>> data = messages.read(fields);
        Map<String, Object> result = new HashMap<>(2);
        if (data.size() > limit) {
            data.remove(data.size() - 1);
            result.put("hasNext", true);
        } else {
            result.put("hasNext", false);
        }
        result.put("values", data);
        for (Map<String, Object> row : data) {
            String messageId = (String) row.get("id");
            Records n = notification.filter(r -> Utils.equals(messageId, r.getRec("message_id").getId())).firstOrDefault();
            Records feed = n.getRec("feed_id");
            if (feed.any()) {
                Records message = rec.browse((String) row.get("id"));
                Records menu = feed.getRec("menu_id");
                row.put("model", message.get("model"));
                row.put("res_id", message.getRec("res_id").getId());
                row.put("feed", feed.get("name"));
                row.put("view", feed.get("view"));
                row.put("views", menu.getString("view"));
                row.put("menu", menu.l10n(menu.getString("name")));
            }
            row.put("is_read", n.getBoolean("is_read"));
        }
        return result;
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "统计消息数量", ids = false)
    public long countMessage(Records rec, Criteria criteria) {
        criteria.and(Criteria.equal("user_id", rec.getEnv().getUserId()));
        return rec.getEnv().get("bbs.notification").count(criteria);
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "读取消息", ids = false)
    public List<Map<String, Object>> searchUser(Records rec, String keyword) {
        Records users = rec.getEnv().get("rbac.user").find(Criteria.equal("active", true)
            .and(Criteria.like("login", keyword).or("name", "like", keyword))
            .and("id", "!=", rec.getEnv().getUserId()), 0, 10, "name asc");
        return users.read(Arrays.asList("present"));
    }
}
