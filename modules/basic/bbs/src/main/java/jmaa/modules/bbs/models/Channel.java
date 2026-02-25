package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.util.KvMap;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "bbs.channel", label = "频道")
public class Channel extends Model {
    static Field name = Field.Char().label("名称");
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field channel_type = Field.Selection(new Options() {{
        put("chat", "聊天");
        put("channel", "频道");
        put("group", "群聊");
    }}).label("类型").defaultValue("channel");
    static Field channel_user_ids = Field.One2many("bbs.channel_user", "channel_id").label("成员");

    @ServiceMethod(label = "获取频道信息", auth = Constants.ANONYMOUS)
    public Object getChannels(Records records) {
        Environment env = records.getEnv();
        Records channelUsers = env.get("bbs.channel_user").find(Criteria.equal("channel_id.active", true)
            .and("user_id", "=", env.getUserId()).and("is_pinned", "=", true));
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> chatChannelIds = new ArrayList<>();
        for (Records userChannel : channelUsers) {
            Records channel = userChannel.getRec("channel_id");
            String name = userChannel.getString("custom_channel_name");
            if (Utils.isEmpty(name)) {
                name = channel.getString("name");
            }
            String type = channel.getString("channel_type");
            KvMap kv = new KvMap()
                .set("id", channel.getId())
                .set("name", name)
                .set("channel_type", type);
            result.add(kv);
            if ("chat".equals(type)) {
                chatChannelIds.add(channel.getId());
            }
        }
        if (!chatChannelIds.isEmpty()) {
            updateChatAvatar(records, chatChannelIds, result);
        }
        return result;
    }

    public void updateChatAvatar(Records records, List<String> channelIds, List<Map<String, Object>> list) {
        Map<String, Object> map = new KvMap();
        Records chatUser = records.getEnv().get("bbs.channel_user").find(Criteria.in("channel_id", channelIds)
            .and("user_id", "!=", records.getEnv().getUserId()));
        for (Records row : chatUser) {
            List<Map<String, Object>> images = (List<Map<String, Object>>) row.getRec("user_id").get("image");
            if (Utils.isNotEmpty(images)) {
                map.put(row.getRec("channel_id").getId(), images.get(0).get("id"));
            }
        }
        for (Map<String, Object> row : list) {
            Object image = map.get(row.get("id"));
            if (Utils.isNotEmpty(image)) {
                row.put("avatar", image);
            }
        }
    }

    @ServiceMethod(label = "获取频道推送", auth = Constants.ANONYMOUS)
    public Object fetchFeed(Records records) {
        Environment env = records.getEnv();
        List<Map<String, Object>> result = new ArrayList<>();
        Cursor cr = env.getCursor();
        String sql = "SELECT u.id,u.channel_id,u.is_pinned,max(m.id) FROM bbs_channel_user u " +
            "left join bbs_message m on m.res_id=u.channel_id and m.id > u.seen_message_id " +
            "where u.user_id=%s group by u.id,u.channel_id,u.is_pinned having u.is_pinned=%s or max(m.id) is not null;";
        cr.execute(sql, Utils.asList(env.getUserId(), true));
        List<Object[]> rows = cr.fetchAll();
        Map<String, Object> mapCount = new KvMap();
        for (Object[] row : rows) {
            boolean hasNew = Utils.isNotEmpty(row[3]);
            if (hasNew && !Utils.toBoolean(row[2])) {
                env.get("bbs.channel_user", (String) row[0]).set("is_pinned", true);
            }
            mapCount.put((String) row[1], hasNew);
        }
        Records channels = records.browse(mapCount.keySet());
        sql = "SELECT max(id) FROM bbs_message where res_id in %s group by res_id";
        cr.execute(sql, Utils.asList(mapCount.keySet()));
        List<String> messageIds = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
        Records messages = env.get("bbs.message", messageIds);
        for (Records channel : channels) {
            Records channelUsers = channel.getRec("channel_user_ids");
            Records message = messages.filter(r -> Utils.equals(r.getRec("res_id").getId(), channel.getId()));
            Records myChannel = channelUsers.filter(u -> Utils.equals(env.getUserId(), u.getRec("user_id").getId()));
            String name = myChannel.getString("custom_channel_name");
            if (Utils.isEmpty(name)) {
                name = channel.getString("name");
            }
            String type = channel.getString("channel_type");
            KvMap kv = new KvMap()
                .set("id", channel.getId())
                .set("name", name)
                .set("description", message.get("body"))
                .set("model", "bbs.message")
                .set("res_id", channel.getId())
                .set("type", type)
                .set("last_dt", Utils.format(message.getDateTime("create_date"), "yyyy-MM-dd HH:mm:ss"))
                .set("menu", channel.l10n("消息"))
                .set("view", "custom")
                .set("views", "custom")
                .set("unread", mapCount.get(channel.getId()));
            result.add(kv);
            if ("chat".equals(type)) {
                Records userChannel = channelUsers.filter(u -> !Utils.equals(env.getUserId(), u.getRec("user_id").getId()));
                List<Map<String, Object>> images = (List<Map<String, Object>>) userChannel.getRec("user_id").get("image");
                if (Utils.isNotEmpty(images)) {
                    kv.put("avatar", images.get(0).get("id"));
                }
            }
        }
        result.sort((x, y) -> {
            String dt1 = (String) x.get("last_dt");
            String dt2 = (String) y.get("last_dt");
            if (Utils.isEmpty(dt1)) return 1;
            if (Utils.isEmpty(dt2)) return -1;
            return dt2.compareTo(dt1);
        });
        return result;
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "获取私信频道", doc = "获取或创建私信频道", ids = false)
    public Object getChannel(Records rec, String userId) {
        Environment env = rec.getEnv();
        Cursor cr = env.getCursor();
        String sql = "select c.id from bbs_channel c join bbs_channel_user u on c.id=u.channel_id " +
            "where c.channel_type='chat' and u.user_id in %s and not exists (select * from bbs_channel_user u2 where u2.channel_id=c.id and u2.user_id not in %s)";
        List<String> userIds = Arrays.asList(userId, env.getUserId());
        cr.execute(sql, Arrays.asList(userIds, userIds));
        Object[] row = cr.fetchOne();
        Records channel = env.get("bbs.channel");
        Records user = env.get("rbac.user", userId);
        Map<String, Object> result = new KvMap();
        if (Utils.isNotEmpty(row)) {
            String id = (String) row[0];
            channel = channel.browse(id);
            Records channelUser = env.get("bbs.channel_user").find(Criteria.equal("channel_id", id).and("user_id", "=", env.getUserId()));
            channelUser.set("is_pinned", true);
            result.put("name", channelUser.get("custom_channel_name"));
        } else {
            channel = channel.create(new KvMap().set("channel_type", "chat"));
            env.get("bbs.channel_user").createBatch(Arrays.asList(
                new KvMap().set("user_id", user.getId()).set("channel_id", channel.getId()).set("custom_channel_name", env.getUser().get("present")),
                new KvMap().set("user_id", env.getUserId()).set("channel_id", channel.getId()).set("custom_channel_name", user.get("present"))));
            result.put("name", user.get("present"));
        }
        result.put("id", channel.getId());
        result.put("channel_type", channel.get("channel_type"));
        List<Map<String, Object>> images = (List<Map<String, Object>>) user.get("image");
        if (Utils.isNotEmpty(images)) {
            result.put("avatar", images.get(0).get("id"));
        }
        return result;
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "加入频道")
    public Object joinChannel(Records record) {
        String type = record.getString("channel_type");
        if ("chat".equals(type)) {
            //
        } else {
            record.getEnv().get("bbs.channel_user").create(new KvMap()
                .set("user_id", record.getEnv().getUserId()).set("channel_id", record.getId()));
        }
        return null;
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "离开频道")
    public Object leaveChannel(Records records) {
        for (Records record : records) {
            String type = record.getString("channel_type");
            Records channelUser = record.getEnv().get("bbs.channel_user").find(Criteria.equal("channel_id", record.getId())
                .and("user_id", "=", record.getEnv().getUserId()));
            if ("chat".equals(type)) {
                channelUser.set("is_pinned", false);
            } else {
                channelUser.delete();
            }
        }
        return Action.success();
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "读取消息")
    public Map<String, Object> readChannel(Records rec, List<String> fields) {
        Map<String, Object> result = new KvMap();
        Environment env = rec.getEnv();
        Records channelUser = env.get("bbs.channel_user").find(Criteria.equal("channel_id", rec.getId()).and("user_id", "=", env.getUserId()));
        String name = channelUser.getString("custom_channel_name");
        if (Utils.isEmpty(name)) {
            name = rec.getString("name");
        }
        result.put("name", name);
        result.put("type", rec.get("channel_type"));
        result.put("messages", readMessage(rec, null, fields));
        return result;
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "读取消息")
    public List<Map<String, Object>> readMessage(Records rec, String lastId, List<String> fields) {
        Criteria criteria = Criteria.equal("res_id", rec.getId()).and("model", "=", "bbs.channel");
        if (Utils.isNotEmpty(lastId)) {
            criteria.and("id", ">", lastId);
        }
        Records messages = rec.getEnv().get("bbs.message").find(criteria, 0, 1000, "id asc");
        if (messages.any()) {
            String[] ids = messages.getIds();
            rec.getEnv().get("bbs.channel_user").find(Criteria.equal("channel_id", rec.getId())
                .and("user_id", "=", rec.getEnv().getUserId())).set("seen_message_id", ids[ids.length - 1]);
        }
        return messages.read(fields);
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "发送消息")
    public Map<String, Object> postMessage(Records rec, String body, List<String> fields) {
        Records message = rec.getEnv().get("bbs.message").create(new KvMap()
            .set("model", "bbs.channel")
            .set("res_id", rec.getId())
            .set("author_id", rec.getEnv().getUserId())
            .set("body", body));
        rec.getEnv().get("bbs.channel_user").find(Criteria.equal("channel_id", rec.getId())
            .and("user_id", "=", rec.getEnv().getUserId())).set("seen_message_id", message.getId());
        return message.read(fields).get(0);
    }
}
