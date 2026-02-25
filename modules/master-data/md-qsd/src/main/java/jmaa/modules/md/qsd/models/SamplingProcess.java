package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "qsd.sampling_process", label = "抽样过程", inherit = {"mixin.companies", "mixin.ac_re"})
public class SamplingProcess extends Model {
    static Field name = Field.Char().label("抽样过程").required().unique();
    static Field company_ids = Field.Many2many("res.company", "qsd_sampling_process_company", "process_id", "company_id");
    static Field type = Field.Selection(new Options() {{
        put("full", "全检");
        put("percent", "百分比");
        put("fixed", "固定数量");
        put("dynamic", "抽样方案");
    }}).label("抽样方式").required();
    static Field percent = Field.Float().label("样本比例");
    static Field sample_size = Field.Integer().label("样本数量");
    static Field sampling_plan_id = Field.Many2one("qsd.sampling_plan").label("抽样方案")
        .lookup("lookupSamplingPlan");
    static Field aql = Field.Char().label("AQL");
    static Field strictness = Field.Selection(new Options() {{
        put("normal", "正常");
        put("tightened", "加严");
        put("reduced", "放宽");
    }}).label("严格度").defaultValue("normal");
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().defaultValue(true).label("是否有效");

    public Criteria lookupSamplingPlan(Records rec) {
        return Criteria.equal("sub_levels", false);
    }


    /**
     * 根据抽样方案加载AQL.
     * <pre>
     * 动态抽样才有AQL.全检、百分比抽样、固定抽样没有AQL.
     * 子检验水平(特殊检验水平1-4，一般检验水平Ⅰ、Ⅱ、Ⅲ)的AQL保存在父方案
     * </pre>
     */
    @Model.ServiceMethod(auth = "read")
    public List<String> getAqlList(Records record, String samplingPlanId) {
        Records plan = record.getEnv().get("qsd.sampling_plan", samplingPlanId);
        Cursor cr = plan.getEnv().getCursor();
        Records parent = plan.getRec("parent_id");
        if (parent.any()) {
            plan = parent;
        }
        if (plan.getBoolean("sub_levels")) {
            cr.execute("select distinct name from qsd_aql_table where plan_id=%s and strictness='normal'", Utils.asList(plan.getId()));
        } else {
            cr.execute("select distinct name from qsd_aql_custom where plan_id=%s", Utils.asList(plan.getId()));
        }
        List<Object[]> rows = cr.fetchAll();
        return rows.stream().map(array -> (String) array[0]).sorted((x, y) -> {
            if (Utils.Numbers.isParsable(x)) {
                if (Utils.Numbers.isParsable(y)) {
                    return Utils.large(Utils.toDouble(x, Integer.MAX_VALUE), Utils.toDouble(y, Integer.MAX_VALUE)) ? 1 : -1;
                }
                return -1;
            } else {
                if (Utils.Numbers.isParsable(y)) {
                    return 1;
                }
                return Utils.Strings.compare(x, y);
            }
        }).collect(Collectors.toList());
    }

    /**
     * 计算样本数和Ac/Re
     */
    public Map<String, Object> getSample(Records record, int qty) {
        Map<String, Object> result = new HashMap<>();
        String type = record.getString("type");
        if ("dynamic".equals(type)) {
            String aql = record.getString("aql");
            String strictness = record.getString("strictness");
            Records plan = record.getRec("sampling_plan_id");
            return (Map<String, Object>) plan.call("getSample", aql, strictness, qty);
        } else {
            if ("full".equals(type)) {
                result.put("sample_size", qty);
            } else if ("percent".equals(type)) {
                double percent = record.getDouble("percent");
                result.put("sample_size", Utils.toInt(qty * percent / 100));
            } else if ("fixed".equals(type)) {
                result.put("sample_size", record.getInteger("sample_size"));
            }
            result.put("ac", record.get("ac"));
        }
        return result;
    }
}
