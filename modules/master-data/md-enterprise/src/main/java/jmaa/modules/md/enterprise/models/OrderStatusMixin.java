package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "mixin.order_status", description = "支持单据状态管理的单据基类", label = "单据基类", inherit = {"bbs.thread"})
public class OrderStatusMixin extends AbstractModel {
    static Field status = Field.Selection(new Options(){{
        put("draft", "草稿");
        put("commit", "已提交");
        put("approve", "已审核");
        put("reject", "驳回");
        put("done", "完成");
        put("cancel", "取消");
        put("close", "关闭");
    }}).label("状态").defaultValue("draft").tracking();

    @Model.ServiceMethod(label = "提交", doc = "提交单据，状态改为已提交")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"draft".equals(orderStatus) && !"reject".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以提交", record.getSelection("status")));
            }
        }
        if (values != null) {
            records.update(values);
        }
        String body = records.l10n("提交") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "commit");
        return Action.success();
    }

    @Model.ServiceMethod(label = "审核", doc = "审核单据，从提交状态改为已审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "approve");
        return Action.success();
    }

    @Model.ServiceMethod(label = "驳回", doc = "驳回单据，从提交或审核状态改为驳回")
    public Object reject(Records records, @Doc(doc = "驳回原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus) && !"approve".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以驳回", record.getSelection("status")));
            }
        }
        String body = records.l10n("驳回") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "reject");
        return Action.success();
    }

    @Model.ServiceMethod(label = "关闭", doc = "关闭单据，从任意状态改为关闭")
    public Object close(Records records, @Doc(doc = "关闭原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("close".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态已关闭"));
            }
        }
        String body = records.l10n("关闭") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "close");
        return Action.success();
    }

    @Model.ServiceMethod(label = "重新修改", doc = "重新修改，从任意状态改为草稿")
    public Object reopen(Records records, @Doc(doc = "重新修改原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("draft".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以重新修改", record.getSelection("status")));
            }
        }
        String body = records.l10n("重新修改") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "draft");
        return Action.success();
    }
}
