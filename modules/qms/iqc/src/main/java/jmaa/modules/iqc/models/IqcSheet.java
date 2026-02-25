package jmaa.modules.iqc.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.DateUtils;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ServerDate;

import java.util.*;

@Model.Meta(inherit = {"iqc.sheet", "bbs.thread"})
public class IqcSheet extends Model {
    static Field inspection_item_ids = Field.One2many("iqc.inspect_item", "sheet_id").label("检验项目");
    static Field quality_class_spec_id = Field.Many2one("qsd.quality_class_spec").label("分类检验标准").tracking();
    static Field material_spec_id = Field.Many2one("qsd.material_spec").label("料号检验标准").tracking();
    static Field mode = Field.Selection(Selection.related("qsd.quality_class_spec_item", "mode")).label("检验方式").required().defaultValue("normal");
    static Field status = Field.Selection().addSelection(new Options() {{
        put("inspecting", "检验中");
        put("inspected", "已检验");
        put("close", "关闭");
    }});
    static Field attachments = Field.Binary().label("附件");
    static Field material_name = Field.Char().label("物料名称").related("material_id.name");
    static Field mrb_id = Field.Many2one("qsd.mrb").label("MRB").ondelete(DeleteMode.SetNull);
    static Field inspect_uid = Field.Many2one("rbac.user").label("检验人");
    static Field inspect_date = Field.DateTime().label("检验时间");

    @OnSaved("material_id")
    public void onMaterialSave(Records records) {
        Environment env = records.getEnv();
        for (Records record : records) {
            Records material = record.getRec("material_id");
            if (material.any()) {
                Records qualityClassSpec = record.getRec("quality_class_spec_id");
                if (!qualityClassSpec.any()) {
                    qualityClassSpec = env.get("qsd.quality_class_spec").find(Criteria.equal("class_id.material_ids", material.getId())
                        .and("type", "=", record.getMeta().getName()).and("active", "=", true), 0, 1, "version desc");
                    record.set("quality_class_spec_id", qualityClassSpec);
                }
                Records materialSpec = record.getRec("material_spec_id");
                if (!materialSpec.any()) {
                    materialSpec = env.get("qsd.material_spec").find(Criteria.equal("material_id", material.getId())
                        .and("type", "=", record.getMeta().getName()).and("active", "=", true), 0, 1, "version desc");
                    record.set("material_spec_id", materialSpec);
                }
            }
        }
    }

    @Model.ServiceMethod(label = "开始检验", auth = "commit")
    public Object commence(Records records, Map<String, Object> values, @Doc(doc = "说明") String comment) {
        if (values != null) {
            records.update(values);
        }
        Environment env = records.getEnv();
        for (Records record : records) {
            if (!"to-inspect".equals(record.getString("status"))) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以开始检验", record.getSelection("status")));
            }
            Records material = record.getRec("material_id");
            Records supplier = record.getRec("supplier_id");
            boolean exempted = (boolean) record.call("isExempt", material, supplier);
            if (exempted) {
                //免检
                record.set("status", "exempted");
                continue;
            }
            Records items = record.getRec("inspection_item_ids");
            if (!items.any()) {
                String mode = record.getString("mode");
                List<Map<String, Object>> itemList = new ArrayList<>();
                Records classSpec = record.getRec("quality_class_spec_id");
                if (classSpec.any()) {
                    Records classItems = env.get("qsd.quality_class_spec_item").find(Criteria.equal("spec_id", classSpec.getId())
                        .and("mode", "=", mode));
                    for (Records row : classItems) {
                        itemList.add(getInspectItem(record, row));
                    }
                }
                Records materialSpec = record.getRec("material_spec_id");
                if (materialSpec.any()) {
                    Records materialItems = env.get("qsd.material_spec_item").find(Criteria.equal("spec_id", materialSpec.getId())
                        .and("mode", "=", mode));
                    for (Records row : materialItems) {
                        itemList.add(getInspectItem(record, row));
                    }
                }
                if (itemList.isEmpty()) {
                    throw new ValidationException(records.l10n("找不到检验项目，请维护分类检验标准或料号检验标准"));
                }
                items = items.createBatch(itemList);
            }
            for (Records item : items) {
                Records sampling = item.getRec("sampling_process_id");
                Map<String, Object> sample = (Map<String, Object>) sampling.call("getSample", Utils.toInt(record.getDouble("qty")));
                item.set("sample_size", sample.get("sample_size"));
                item.set("ac", sample.get("ac"));
            }
            record.set("status", "inspecting");
        }
        String body = records.l10n("开始检验") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        return Action.reload(records.l10n("操作成功"));
    }

    public Map<String, Object> getInspectItem(Records record, Records item) {
        MetaModel meta = record.getEnv().getRegistry().get("qsd.inspect_item");
        Set<String> fields = meta.getFields().keySet();
        Map<String, Object> result = item.read(fields).get(0);
        result.put("sheet_id", record.getId());
        return result;
    }

    @Model.ServiceMethod(label = "提交", doc = "提交检验的检验单，状态改为已检验", auth = "commit")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        if (values != null) {
            records.update(values);
        }
        Records inspectItems = records.getEnv().get("iqc.inspect_item");
        for (Records record : records) {
            if (!"inspecting".equals(record.getString("status"))) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以提交", record.getSelection("status")));
            }
            if (Utils.isEmpty(record.get("result"))) {
                // 检查检验项目是否有NG状态，如果有就判定单据为NG
                long ng_qty = inspectItems.count(Criteria.equal("sheet_id", record.getId()).and(Criteria.equal("result", "ng")));
                if (Utils.large(ng_qty, 0)) {
                    record.set("result", "ng");
                } else {
                    throw new ValidationException(records.l10n("还有未完成的检验项目"));
                }
            }
        }
        String body = records.l10n("提交") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("inspect_uid", records.getEnv().getUserId());
        records.set("inspect_date", new ServerDate());
        boolean autoApprove = records.getEnv().getConfig().getBoolean("iqc_auto_approve");
        records.set("status", autoApprove ? "done" : "inspected");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "审核", doc = "审核已提交检验的检验单，状态改为已完成", auth = "approve")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"inspected".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "done");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "驳回", doc = "已完成的检验单，如对检验结果有问题，可以驳回重新检验，状态改为检验中", auth = "reject")
    public Object reject(Records records, @Doc(doc = "驳回原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"inspected".equals(orderStatus) && !"done".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以驳回", record.getSelection("status")));
            }
        }
        String body = records.l10n("驳回") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "inspecting");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "关闭", doc = "关闭检验单，未完成的检验单，不需要检验可以进行关闭操作", auth = "close")
    public Object close(Records records, @Doc(doc = "关闭原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("to-inspect".equals(orderStatus) || "done".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以关闭", record.getSelection("status")));
            }
        }
        String body = records.l10n("关闭") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "close");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "重新修改", doc = "重新修改，从任意状态改为草稿", auth = "close")
    public Object reopen(Records records, @Doc(doc = "重新修改原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("to-inspect".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以重新修改", record.getSelection("status")));
            }
        }
        String body = records.l10n("重新修改") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "to-inspect");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "创建MRB")
    public Object createMrb(Records record) {
        callSuper(record);
        record.getRec("details_ids").set("status", "mrb");
        return Action.reload(record.l10n("操作成功"));
    }
}
