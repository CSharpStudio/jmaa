package jmaa.modules.iqc.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "iqc.inspect_item", label = "来料检验项目", inherit = {"qsd.inspect_item", "mixin.ac_re"}, authModel = "iqc.sheet", order = "id ASC")
public class IqcInspectItem extends Model {
    static Field sheet_id = Field.Many2one("iqc.sheet").label("来料检验单");
    static Field sample_size = Field.Integer().label("样本数").min(1);
    static Field test_values = Field.Char().label("测试值").help("记录样本的测试值").length(2000);
    static Field result = Field.Selection(new Options() {{
        put("ok", "合格");
        put("ng", "不合格");
    }}).label("检验结果").help("不良数大于等于Re值判定为不合格");
    static Field ng_qty = Field.Integer().label("不良数").min(0);
    static Field category = Field.Selection(Selection.related("qsd.inspect_item", "category")).useCatalog(false);

    @Constrains("result")
    public void checkResult(Records records) {
        Records first = records.first();
        Records sheetId = first.getRec("sheet_id");
        long checkAllNum = records.count(Criteria.equal("sheet_id", sheetId.getId()));
        long checkOverNum = records.count(Criteria.equal("sheet_id", sheetId.getId()).and(Criteria.notEqual("result", null)));
        if (!Utils.equals(checkAllNum, checkOverNum)) {
            sheetId.set("result", null);
            return;
        }
        long ng_qty = records.count(Criteria.equal("sheet_id", sheetId.getId()).and(Criteria.equal("result", "ng")));
        if (Utils.large(ng_qty, 0)) {
            sheetId.set("result", "ng");
        } else {
            sheetId.set("result", "ok");
        }
    }

    /**
     * 定性项目一键合格
     */
    @Model.ServiceMethod(label = "检验", doc = "定性项目一键合格", auth = "inspect")
    public Object allPass(Records record, String sheetId) {
        Records records = record.getEnv().get("iqc.inspect_item").find(Criteria.equal("sheet_id", sheetId)
            .and(Criteria.equal("result", null)).and(Criteria.equal("mark", "qual")));
        records.set("result", "ok");
        records.set("ng_qty", 0);
        return Action.reload(record.l10n("操作成功"));
    }

    /**
     * 读取检验明细
     */
    @ServiceMethod(label = "读取检验明细", auth = "read")
    public Map<String, Object> readInspectItemDetail(Records record, @Doc("检验单") String sheetId, @Doc("检验项") String itemId, @Doc("序列号") Integer sn, @Doc(doc = "步法类型", sample = "-1,0,1") int stepType) {
        Records inspectItems = record.getEnv().get("iqc.inspect_item");
        //查询所有检验结果为空的检验项目
        Records lines = null;
        long unCheckInspectItemSize = record.getEnv().get("iqc.inspect_item").count(Criteria.equal("sheet_id", sheetId).and(Criteria.equal("result", null)));
        if (Utils.equals(unCheckInspectItemSize, 0)) {
            throw new ValidationException(record.l10n("没有待检验的项目"));
        }
        if (Utils.isEmpty(itemId)) {
            throw new ValidationException(record.l10n("没有待检验的项目"));
        }

        if (Utils.equals(stepType, 0)) {
            lines = inspectItems.find(Criteria.equal("id", itemId).and(Criteria.equal("sheet_id", sheetId)));
        } else if (Utils.less(stepType, 0)) {
            //上一个
            Criteria criteria = Criteria.equal("sheet_id", sheetId).and(Criteria.equal("result", null)).and(Criteria.less("id", itemId));
            lines = inspectItems.find(criteria, 0, 1, "id desc");
            if (!lines.any()) {
                //没有就查最后一个
                lines = inspectItems.find(Criteria.equal("sheet_id", sheetId).and(Criteria.equal("result", null)), 0, 1, "id desc");
            }
        } else {
            //下一个
            Criteria criteria = Criteria.equal("sheet_id", sheetId).and(Criteria.equal("result", null)).and(Criteria.greater("id", itemId));
            lines = inspectItems.find(criteria, 0, 1, "id asc");
            if (!lines.any()) {
                //没有就查第一个
                lines = inspectItems.find(Criteria.equal("sheet_id", sheetId).and(Criteria.equal("result", null)), 0, 1, "id asc");
            }
        }

        if (!lines.any()) {
            return new KvMap();
        }

        if (!Utils.equals(stepType, 0) && Utils.equals(lines.getId(), itemId)) {
            throw new ValidationException(record.l10n("没有其它待检验项目"));
        }

        List<String> fieldNames = lines.getMeta().getFields().values().stream().map(MetaField::getName).collect(Collectors.toList());
        Map<String, Object> itemData = lines.read(fieldNames).get(0);
        itemData.put("sn", sn);
        return itemData;
    }
}

