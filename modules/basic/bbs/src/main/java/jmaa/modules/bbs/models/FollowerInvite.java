package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;

import java.util.Collection;
import java.util.Map;

@Model.Meta(name = "bbs.follower.invite", label = "邀请关注者", logAccess = BoolState.False)
@Model.Service(remove = "@all")
public class FollowerInvite extends ValueModel {
    static Field user_id = Field.Many2one("rbac.user").label("关注者");

    @Override
    @Model.ServiceMethod(auth = Constants.ANONYMOUS, label = "根据关联字段查询数据", ids = false)
    public Map<String, Object> searchByField(Records rec,
                                             @Doc(doc = "关联的字段") String relatedField,
                                             @Doc(doc = "查询条件") Criteria criteria,
                                             @Doc(doc = "偏移量") Integer offset,
                                             @Doc(doc = "行数") Integer limit,
                                             @Doc(doc = "读取的字段") Collection<String> fields,
                                             @Doc(doc = "排序") String order) {
        return (Map<String, Object>) callSuper(rec, relatedField, criteria, offset, limit, fields, order);
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "获取邀请信息")
    public String getInviteMessage(Records rec, String resModel, String resId) {
        Records doc = rec.getEnv().get(resModel, resId);
        return rec.l10n("你好！%s 邀请你关注 %s：%s", rec.getEnv().getUser().get("present"), rec.l10n(doc.getMeta().getLabel()), doc.get("present"));
    }
}
