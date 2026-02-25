package jmaa.modules.iqc.models;

import org.jmaa.sdk.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Model.Meta(name="iqc.sheet_mobile", inherit = "iqc.sheet", table="iqc_sheet", label = "来料检验移动端")
@Model.Service(remove = "@edit")
public class IqcSheetMobile extends ValueModel {

    @ServiceMethod(auth = "read", label = "查询来料检验列表数据")
    @Doc(doc = "根据查询条件查询对应的来料检验列表数据")
    public Object searchList(Records records, List<String> fields, String keyword, Integer offset, Integer limit, String order) {
        Criteria criteria = Criteria.in("status", Arrays.asList("to-inspect","inspecting"));
        if(Utils.isNotEmpty(keyword)){
            criteria =  Criteria.equal("code",keyword).or(Criteria.equal("related_code",keyword)).or(Criteria.equal("material_id.code",keyword));
        }
        return records.search(fields, criteria, offset, limit, order);
    }
    @Model.ServiceMethod(label = "开始检验", auth = "read")
    public Object commence(Records records, Map<String, Object> values, @Doc(doc = "说明") String comment) {
        return callSuper(records, values, comment);
    }

    @Model.ServiceMethod(label = "提交", doc = "提交检验的检验单，状态改为已检验", auth = "read")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        return callSuper(records, values, comment);
    }
    @ServiceMethod(label = "更新表单", auth = "read")
    public void update(Records records, Map<String, Object> values) {
        callSuper(records, values);
    }
    @ServiceMethod(label = "获得数据", auth = "read")
    public List<Map<String, Object>> read(Records rec, Collection<String> fields){
        return  rec.getEnv().get("iqc.sheet").browse(rec.getIds()).read(fields);
    }
}
