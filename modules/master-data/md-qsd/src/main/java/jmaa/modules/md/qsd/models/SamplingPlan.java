package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽样方案。包括全检、百分比抽样、固定数量抽样、动态抽样(AQL)四种类型。
 * <pre>
 * 全检：样本数量=送检数量，根据Ac Re判断结果。
 * 百分比抽样：样本数量=送检数量*样本比例/100，根据Ac Re判断结果。
 * 固定数量抽样：样本数量=设定的样本数量，根据Ac Re判断结果。
 * 动态抽样：分为有子检验水平(特殊检验水平1-4，一般检验水平Ⅰ、Ⅱ、Ⅲ)和没有子检验水平两种情况。
 *      有子检验水平使用字码表维护，样本数量根据字码样本数计算(qsd.sample_size)，Ac Re根据AQL计算(qsd.aql_table)，计算逻辑参考国标GB2828.1-2012
 *      没有子检验水平不使用字码表，样本数量和Ac Re直接保存在qsd.aql_custom
 *      动态规则(未实现)：正常、加严、放宽
 * </pre>
 */
@Model.Meta(name = "qsd.sampling_plan", label = "抽样方案", inherit = {"mixin.companies"})
public class SamplingPlan extends Model {
    static Field name = Field.Char().label("抽样方案").required().unique();
    static Field company_ids = Field.Many2many("res.company", "qsd_sampling_plan_company", "plan_id", "company_id");
    static Field source = Field.Selection(new Options() {{
        put("system", "系统");
        put("manual", "自建");
    }}).label("方案来源").defaultValue("manual").copy(false);
    static Field aql_custom_ids = Field.One2many("qsd.aql_custom", "plan_id").label("抽样标准");
    static Field aql_normal_ids = Field.One2many("qsd.aql_table", "plan_id").label("正常AQL").lookup(Criteria.equal("strictness", "normal"));
    static Field aql_tightened_ids = Field.One2many("qsd.aql_table", "plan_id").label("加严AQL").lookup(Criteria.equal("strictness", "tightened"));
    static Field aql_reduced_ids = Field.One2many("qsd.aql_table", "plan_id").label("放宽AQL").lookup(Criteria.equal("strictness", "reduced"));
    static Field sample_size_ids = Field.One2many("qsd.sample_size", "plan_id").label("样本数");
    static Field sub_levels = Field.Boolean().label("是否有多个检验水平").help("有多个检验水平时，使用字码表检索");
    static Field active = Field.Boolean().defaultValue(true).label("是否有效");
    static Field remark = Field.Char().label("备注");
    static Field level = Field.Selection(new Options() {{
        put("s1", "S-1");
        put("s2", "S-2");
        put("s3", "S-3");
        put("s4", "S-4");
        put("g1", "Ⅰ");
        put("g2", "Ⅱ");
        put("g3", "Ⅲ");
    }}).label("检验水平");
    static Field parent_id = Field.Many2one("qsd.sampling_plan");

    @OnSaved("name")
    public void onNameSaved(Records records) {
        for (Records record : records) {
            if (record.getBoolean("sub_levels")) {
                Records children = record.withContext("#companyLess", true).find(Criteria.equal("parent_id", record.getId()));
                for (Records child : children) {
                    String level = child.getSelection("level");
                    child.set("name", record.getString("name") + String.format("(%s)", level));
                }
            }
        }
    }

    @OnSaved("active")
    public void onActiveSaved(Records records) {
        for (Records record : records) {
            if (record.getBoolean("sub_levels")) {
                record.withContext("#companyLess", true).find(Criteria.equal("parent_id", record.getId())).set("active", record.get("active"));
            }
        }
    }

    @OnSaved("company_ids")
    public void onCompanySaved(Records records) {
        for (Records record : records) {
            if (record.getBoolean("sub_levels")) {
                record.withContext("#companyLess", true).find(Criteria.equal("parent_id", record.getId())).set("company_ids", record.get("company_ids"));
            }
        }
    }

    @ServiceMethod(auth = "read")
    public List<Map<String, Object>> getLevelLetterTable(Records record) {
        Records table = record.getEnv().get("qsd.level_letter_table");
        Collection<String> fields = table.getMeta().getFields().values().stream().filter(f -> !f.isAuto()).map(f -> f.getName()).collect(Collectors.toList());
        return table.search(fields, new Criteria());
    }

    @ServiceMethod(auth = "read")
    public Map<String, Object> getAqlTable(Records record, String strictness) {
        Records table = record.getEnv().get("qsd.aql_table");
        Collection<String> fields = table.getMeta().getFields().values().stream().filter(f -> !f.isAuto()).map(f -> f.getName()).collect(Collectors.toList());
        Object aql = table.search(fields, Criteria.equal("plan_id", record.getId()).and("strictness", "=", strictness));
        Object sampleSize = record.getEnv().get("qsd.sample_size").search(Arrays.asList("letter", "size"), Criteria.equal("plan_id", record.getId()).and("strictness", "=", strictness));
        return new HashMap<String, Object>() {{
            put("aql", aql);
            put("sample", sampleSize);
        }};
    }

    /**
     * 计算样本数和Ac/Re
     */
    public Map<String, Object> getSample(Records record, String aql, String strictness, int qty) {
        Map<String, Object> result = new HashMap<>();
        String level = record.getString("level");
        if (Utils.Strings.isNotBlank(level)) {
            result = getSampleFromLetterTable(record, qty, aql, strictness, level);
        } else {
            //企标
            Records aqlCustom = record.getEnv().get("qsd.aql_custom").find(Criteria.equal("plan_id", record.getId())
                .and(Criteria.equal("strictness", strictness)).and(Criteria.lessOrEqual("limit_lower", qty)
                    .and(Criteria.greaterOrEqual("limit_upper", qty))));
            int ac = aqlCustom.getInteger("ac");
            int sampleSize = Math.min(aqlCustom.getInteger("sample_size"), qty);
            result.put("ac", ac);
            result.put("sample_size", sampleSize);
        }
        return result;
    }

    public Map<String, Object> getSampleFromLetterTable(Records record, int qty, String aql, String strictness, String level) {
        Map<String, Object> result = new HashMap<>();
        //先去查询样本对应的字码
        Records letterData = record.getEnv().get("qsd.level_letter_table").find(Criteria.lessOrEqual("limit_lower", qty)
            .and(Criteria.greaterOrEqual("limit_upper", qty)));
        if (!letterData.any()) {
            throw new ValidationException(record.l10n("送检数量[%s]在抽样方案中未匹配到样本字码批量区间", qty));
        }
        String letter = letterData.getString(level);
        //根据 aql查询 Ac Re
        Records parent = record.getRec("parent_id");
        if (!parent.any()) {
            return result;
        }
        Records aqlData = record.getEnv().get("qsd.aql_table").find(Criteria.equal("plan_id", parent.getId())
            .and(Criteria.equal("strictness", strictness)).and(Criteria.equal("name", aql)));
        int ac = aqlData.getInteger(letter.toLowerCase());
        if (ac >= 0) {
            result.put("ac", ac);
        } else {
            Map<String, Object> value = findAc(aqlData, letter.toLowerCase());
            ac = (int) value.get("ac");
            result.put("ac", ac);
            letter = value.get("code").toString();
        }
        //查询样本数量
        Records sampleSizeData = record.getEnv().get("qsd.sample_size").find(Criteria.equal("plan_id", parent.getId())
            .and(Criteria.equal("strictness", strictness)).and(Criteria.equal("letter", letter.toUpperCase())));
        int size = sampleSizeData.getInteger("size");
        int sampleSize = Math.min(size, qty);
        result.put("sample_size", sampleSize);
        return result;
    }


    public static Map<String, Object> findAc(Records records, String letter) {
        List<String> keys = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s");
        int startIndex = keys.indexOf(letter);
        if (startIndex == -1) {
            throw new ValidationException("起始键不存在: " + letter);
        }
        int currentIndex = startIndex;
        Map<String, Object> result = new HashMap<>();
        String currentLetter = keys.get(currentIndex);
        int value = records.getInteger(currentLetter);
        if (value > -1) {
            result.put("code", currentLetter);
            result.put("ac", value);
            return result;
        }
        int sign = value == -1 ? 1 : -1;
        while (true) {
            if (currentIndex >= keys.size() || currentIndex < 0) {
                throw new ValidationException("超出集合边界");
            }
            currentIndex += sign;
            currentLetter = keys.get(currentIndex);
            value = records.getInteger(currentLetter);
            if (value > -1) {
                result.put("code", currentLetter);
                result.put("ac", value);
                return result;
            }
        }
    }

    public Map<String, Object> copyData(Records root, Records rec, Map<String, Object> defaultValues) {
        Map<String, Object> defaults = new HashMap<>(16);
        if (defaultValues != null) {
            defaults.putAll(defaultValues);
        }
        for (Map.Entry<String, MetaField> e : rec.getMeta().getFields().entrySet()) {
            String fieldName = e.getKey();
            MetaField field = e.getValue();
            if (Constants.ID.equals(fieldName) || LOG_ACCESS_COLUMNS.contains(fieldName) || defaults.containsKey(fieldName) || !field.isCopy()) {
                continue;
            }
            if (Constants.ONE2MANY.equals(field.getType())) {
                Records o2m = (Records) rec.get(fieldName);
                List<List<Object>> lines = new ArrayList<>();
                for (Records r : o2m) {
                    lines.add(Arrays.asList(0, 0, copyData(root, r, null)));
                }
                defaults.put(fieldName, lines);
            } else if (Constants.MANY2MANY.equals(field.getType())) {
                List<String> ids = Arrays.asList(((Records) rec.get(fieldName)).getIds());
                defaults.put(fieldName, Collections.singletonList(Arrays.asList(6, 0, ids)));
            } else {
                defaults.put(fieldName, field.convertToWrite(rec.get(field), rec));
            }
        }
        MetaModel model = rec.getMeta();
        if (model.getName().equals("qsd.sampling_plan")) {
            for (org.jmaa.sdk.core.UniqueConstraint u : model.getUniques()) {
                for (String field : u.getFields()) {
                    boolean isDefaultValue = defaultValues != null && defaultValues.containsKey(field);
                    if (!isDefaultValue && defaults.containsKey(field)) {
                        String v = rec.l10n("%s (副本)", defaults.get(field));
                        defaults.put(field, v);
                    }
                }
            }
        }
        return defaults;
    }
}
