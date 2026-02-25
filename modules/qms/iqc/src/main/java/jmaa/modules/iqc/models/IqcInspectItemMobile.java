package jmaa.modules.iqc.models;

import org.jmaa.sdk.Doc;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.ValueModel;

import java.util.Map;


@Model.Meta(name="iqc.inspect_item_mobile", inherit = "iqc.inspect_item", table="iqc_inspect_item", authModel = "iqc.sheet_mobile" , label = "来料检验项目移动端")
@Model.Service(remove = "@edit")
public class IqcInspectItemMobile extends ValueModel {
    @Model.ServiceMethod(label = "定性项目一键合格", auth = "read")
    public Object allPass(Records record, @Doc("来料检验单id") String sheetId) {
        return callSuper(record, sheetId);
    }

    @ServiceMethod(label = "读取检验明细", auth = "read")
    public Object readInspectItemDetail(Records records, @Doc("检验单") String sheetId, @Doc("检验项") String itemId, @Doc("序列号") Integer sn, @Doc(doc = "步法类型", sample = "-1,0,1") int stepType) {
        return callSuper(records, sheetId,itemId,sn,stepType);
    }

    @ServiceMethod(label = "更新检验明细", auth = "read")
    public void update(Records records, Map<String, Object> values) {
        callSuper(records, values);
    }
}
