package jmaa.modules.md.product.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "md.bom_version", label = "BOM版本", present = {"version", "code"}, presentFormat = "{code}({version})", order = "version desc", authModel = "md.bom")
@Model.UniqueConstraint(name = "code_unique", fields = {"code", "bom_id"})
public class BomVersion extends Model {
    static Field code = Field.Char().label("BOM编码").required();
    static Field version = Field.Char().label("版本").required();
    static Field bom_id = Field.Many2one("md.bom").label("产品").required();
    static Field is_default = Field.Boolean().label("是否默认").defaultValue(false);
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field source = Field.Selection(new Options() {{
        put("sync", "系统同步");
        put("manual", "自建");
    }}).label("数据来源").defaultValue("manual");
    static Field details_ids = Field.One2many("md.bom_details", "version_id").label("BOM清单");

    @ServiceMethod(label = "设为默认", doc = "设置默认版本")
    public Action setDefault(Records record) {
        Records bom = record.getRec("bom_id");
        record.getEnv().getCursor().execute("update md_bom_version set is_default=%s where bom_id=%s", Utils.asList(false, bom.getId()));
        record.set("is_default", true);
        return Action.success();
    }

    @OnSaved("active")
    public void onActiveSaved(Records records) {
        for (Records record : records) {
            if (record.getBoolean("is_default") && !record.getBoolean("active")) {
                throw new ValidationException(record.l10n("默认版本不能设置为无效"));
            }
        }
    }

    @Model.ServiceMethod(label = "导入", doc = "根据唯一索引更新或者插入数据", ids = false)
    @Doc("返回值")
    public Object importData(Records record, @Doc("参考模型字段") List<Map<String, Object>> values) {
        Set<String> bomCodes = new HashSet<>();
        for (Map<String, Object> map : values) {
            String bomId = Utils.toString(map.get("bom_id"));
            bomCodes.add(bomId);
        }
        Set<String> materialCodes = new HashSet<>(bomCodes);
        Records materials = record.getEnv().get("md.material").find(Criteria.in("code", bomCodes));
        for (Records material : materials) {
            materialCodes.remove(material.getString("code"));
        }
        if (!materialCodes.isEmpty()) {
            throw new ValidationException(record.l10n("物料不存在[%s]", String.join(",", materialCodes)));
        }
        Records boms = record.getEnv().get("md.bom").find(Criteria.in("material_id", materials.getIds()));
        for (Records bom : boms) {
            bomCodes.remove(bom.getRec("material_id").getString("code"));
        }
        if (!bomCodes.isEmpty()) {
            Map<String, String> materialIdsMap = materials.stream().collect(Collectors.toMap(e -> e.getString("code"), Records::getId));
            List<Map<String, Object>> list = new ArrayList<>();
            for (String bomCode : bomCodes) {
                if(Utils.isBlank(materialIdsMap.get(bomCode))){
                    throw new ValidationException(record.l10n("物料不存在[%s]", bomCode));
                }
                Map<String, Object> map = new HashMap<>();
                map.put("material_id", materialIdsMap.get(bomCode));
                list.add(map);
            }
            boms.createBatch(list);
        }
        return record.getEnv().get("ir.import").call("importValues", record.getMeta(), values);
    }
}
