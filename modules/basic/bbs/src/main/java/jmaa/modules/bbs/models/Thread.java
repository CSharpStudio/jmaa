package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.SecurityCode;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author eric
 */
@Model.Meta(name = "bbs.thread", label = "消息")
public class Thread extends AbstractModel {
    static Field message_ids = Field.One2many("bbs.message", "res_id").label("消息");
    static Field message_follower_ids = Field.One2many("bbs.follower", "res_id").label("关注者");

    public Records messagePost(Records rec, Map<String, Object> values) {
        rec.ensureOne();
        Map<String, Object> message = new HashMap<>();
        if (values != null) {
            message.putAll(values);
        }
        message.put("model", rec.getMeta().getName());
        message.put("res_id", rec.getId());
        message.put("author_id", rec.getEnv().getUserId());
        return rec.getEnv().get("bbs.message").create(message);
    }

    public Records messageNotify(Records rec, String subject, String body, List<String> userIds, Map<String, Object> values) {
        Map<String, Object> toCreate = new HashMap<>();
        if (values != null) {
            toCreate.putAll(values);
        }
        toCreate.put("subject", subject);
        toCreate.put("body", body);
        toCreate.put("message_type", "user_notification");
        if (rec.any()) {
            toCreate.put("model", rec.getMeta().getName());
            toCreate.put("res_id", rec.getId());
        }
        toCreate.put("author_id", rec.getEnv().getUserId());
        List<List<Object>> recipient_ids = userIds.stream().map(uid -> Utils.asList((Object) 4, uid)).collect(Collectors.toList());
        toCreate.put("recipient_ids", recipient_ids);
        Records message = rec.getEnv().get("bbs.message").create(toCreate);
        List<Map<String, Object>> notification = new ArrayList<>();
        for (String uid : userIds) {
            notification.add(new KvMap().set("message_id", message.getId()).set("user_id", uid));
        }
        rec.getEnv().get("bbs.notification").createBatch(notification);
        return message;
    }

    public void trackMessage(Records records, String message) {
        trackPrepare(records);
        String key = String.format("bbs.tracking.message-%s", records.getMeta().getName());
        Cursor cr = records.getEnv().getCursor();
        KvMap bodies = (KvMap) cr.getPreCommit().getData().getOrSupply(key, () -> new KvMap());
        for (String id : records.getIds()) {
            List<String> messages = (List<String>) bodies.get(id);
            if (messages == null) {
                messages = new ArrayList<>();
                bodies.put(id, messages);
            }
            messages.add(message);
        }
    }

    @Override
    public void update(Records records, Map<String, Object> values) {
        Map context = records.getEnv().getContext();
        if (!Utils.toBoolean(context.get("tracking_disable"))) {
            trackPrepare(records);
        }
        records.callSuper(Thread.class, "update", values);
    }

    /**
     * 准备追踪记录，记录初始值
     *
     * @param records
     */
    public void trackPrepare(Records records) {
        MetaModel meta = records.getMeta();
        Cursor cr = records.getEnv().getCursor();
        String key = String.format("bbs.tracking-%s", meta.getName());
        KvMap data = cr.getPreCommit().getData();
        KvMap initialValues = (KvMap) data.get(key);
        if (initialValues == null) {
            cr.getPreCommit().add(() -> {
                trackFinalize(records);
            });
            initialValues = new KvMap();
            data.put(key, initialValues);
            List<String> fieldList = meta.getFields().values().stream().filter(f -> f.isTracking())
                .map(f -> f.getName()).collect(Collectors.toList());
            if (fieldList.isEmpty()) {
                return;
            }
            for (Records record : records) {
                KvMap values = (KvMap) initialValues.getOrSupply(record.getId(), () -> new KvMap());
                if (values != null) {
                    for (String field : fieldList) {
                        values.putIfAbsent(field, record.get(field));
                    }
                }
            }
        }
    }

    /**
     * 生成记录集的追踪信息
     *
     * @param records
     */
    public void trackFinalize(Records records) {
        MetaModel meta = records.getMeta();
        Cursor cr = records.getEnv().getCursor();
        String key = String.format("bbs.tracking-%s", meta.getName());
        KvMap initialValues = (KvMap) cr.getPreCommit().getData().remove(key);
        if (initialValues != null) {
            messageTrack(records, initialValues);
            records.flush();
        }
    }

    public void messageTrack(Records records, KvMap initialValues) {
        MetaModel meta = records.getMeta();
        List<MetaField> fields = meta.getFields().values().stream().filter(f -> f.isTracking()).collect(Collectors.toList());
        fields.sort(Comparator.comparingInt(MetaField::getSequence));
        KvMap tracking = new KvMap();
        for (Records record : records) {
            tracking.put(record.getId(), bbsTrack(record, fields, (KvMap) initialValues.get(record.getId())));
        }
        Cursor cr = records.getEnv().getCursor();
        String key = String.format("bbs.tracking.message-%s", meta.getName());
        KvMap bodies = (KvMap) cr.getPreCommit().getData().getOrSupply(key, () -> new KvMap());
        for (Records record : records) {
            List<Object> trackingValueIds = (List<Object>) tracking.get(record.getId());
            if (trackingValueIds.isEmpty() && bodies.isEmpty()) {
                continue;
            }
            KvMap message = new KvMap();
            List<String> messages = (List<String>) bodies.get(record.getId());
            if (Utils.isNotEmpty(messages)) {
                String body = "";
                for (String msg : messages) {
                    body += String.format("<div>%s</div>", msg);
                }
                message.put("body", body);
            }
            message.put("model", meta.getName());
            message.put("res_id", record.getId());
            message.put("author_id", record.getEnv().getUserId());
            message.put("tracking_values_ids", trackingValueIds);
            record.getEnv().get("bbs.message").create(message);
        }
    }

    public List<Object> bbsTrack(Records record, List<MetaField> fields, KvMap values) {
        List<Object> trackingValueIds = new ArrayList<>();
        Records trackingValue = record.getEnv().get("bbs.tracking_value");
        for (MetaField field : fields) {
            String fName = field.getName();
            if (!values.containsKey(fName)) {
                continue;
            }
            Object initialValue = values.get(fName);
            Object newValue = record.get(fName);
            if (Utils.equals(initialValue, newValue)) {
                continue;
            }
            trackingValueIds.add(Arrays.asList(0, 0, trackingValue.call("createTrackingValues", initialValue, newValue, field)));
        }
        return trackingValueIds;
    }

    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        Records records = (Records) rec.callSuper(Thread.class, "createBatch", valuesList);
        for (Records record : records) {
            trackingCreate(record);
        }
        return records;
    }

    public void trackingCreate(Records record) {
        String body = record.l10n("创建");
        messagePost(record, new KvMap().set("body", body));
    }

    @Model.ServiceMethod(auth = "read", label = "取消关注")
    public void messageUnsubscribe(Records record, List<String> userIds) {
        if (Utils.isEmpty(userIds)) {
            userIds = Utils.asList(record.getEnv().getUserId());
        } else {
            Records security = record.getEnv().get("rbac.security");
            if (!(boolean) security.call("hasPermission", record.getEnv().getUserId(), record.getMeta().getName(), "update")) {
                throw new AccessException("没有权限,请重新登录或联系管理员分配权限", SecurityCode.NO_PERMISSION);
            }
        }
        record.getEnv().get("bbs.follower").find(Criteria.equal("res_model", record.getMeta().getName())
            .and("res_id", "=", record.getId()).and("user_id", "in", userIds)).delete();
    }

    @Model.ServiceMethod(auth = "read", label = "添加关注")
    public void messageSubscribe(Records record, List<String> userIds) {
        if (Utils.isEmpty(userIds)) {
            userIds = Utils.asList(record.getEnv().getUserId());
        } else {
            Records security = record.getEnv().get("rbac.security");
            if (!(boolean) security.call("hasPermission", record.getEnv().getUserId(), record.getMeta().getName(), "update")) {
                throw new AccessException("没有权限,请重新登录或联系管理员分配权限", SecurityCode.NO_PERMISSION);
            }
        }
        Records followers = record.getEnv().get("bbs.follower").find(Criteria.equal("res_model", record.getMeta().getName())
            .and("res_id", "=", record.getId()).and("user_id", "in", userIds));
        List<String> exists = followers.stream().map(r -> r.getRec("user_id").getId()).collect(Collectors.toList());
        List<Map<String, Object>> toCreate = new ArrayList<>();
        for (String uid : userIds) {
            if (!exists.contains(uid)) {
                toCreate.add(new KvMap()
                    .set("res_model", record.getMeta().getName())
                    .set("res_id", record.getId())
                    .set("user_id", uid));
            }
        }
        record.getEnv().get("bbs.follower").createBatch(toCreate);
    }

    @Model.ServiceMethod(auth = "read", label = "添加关注")
    public Object followerInvite(Records record, List<String> userIds, boolean notify, String message) {
        messageSubscribe(record, userIds);
        if (notify) {
            messageNotify(record, "", message, userIds, Collections.emptyMap());
        }
        return Action.success();
    }

    @Model.ServiceMethod(auth = "read", label = "添加消息")
    public Object addMessage(Records record, String subject, String body, boolean notify) {
        Records message = messagePost(record, new KvMap().set("body", body).set("subject", subject));
        if (notify) {
            Records followers = record.getEnv().get("bbs.follower").find(Criteria.equal("res_model", record.getMeta().getName())
                .and("res_id", "=", record.getId()));
            List<String> userIds = followers.stream().map(r -> r.getRec("user_id").getId()).collect(Collectors.toList());
            List<Map<String, Object>> notification = new ArrayList<>();
            Records feed = record.getEnv().get("bbs.message_feed").find(Criteria.equal("last_message_id.model", record.getMeta().getName())
                .and("last_message_id.res_id", "=", record.getId()), 0, 1, null);
            if (!feed.any()) {
                Records menu = record.getEnv().get("ir.ui.menu").find(Criteria.equal("model", message.get("model"))
                    .and("active", "=", true), 0, 1, null);
                String name = (menu.any() ? record.l10n(menu.getString("name")) : record.l10n(record.getMeta().getLabel())) + "：" + record.get("present");
                feed = feed.create(new KvMap().set("last_message_id", message.getId()).set("name", name).set("type", "follow").set("menu_id", menu.getId()));
            } else {
                feed.set("last_message_id", message.getId());
            }
            for (String uid : userIds) {
                notification.add(new KvMap().set("message_id", message.getId())
                    .set("user_id", uid).set("feed_id", feed.getId()));
            }
            record.getEnv().get("bbs.notification").createBatch(notification);
        }
        return Action.success();
    }
}
