package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "wms.initial_inventory_mobile", label = "初始化标签-移动端", inherit = {"wms.initial_inventory"}, table = "wms_initial_inventory")
public class InitialInventoryMobile extends ValueModel {
    static Field material_code = Field.Char().label("物料编码").store(false);
    static Field location_code = Field.Char().label("库位").store(false);
    static Field auto_print = Field.Boolean().label("打印标签").store(false);

    @ServiceMethod(label = "生成条码", auth = "read")
    public Map<String, Object> createLabelCode(Records rec,
                                               @Doc("物料ID") String materialId,
                                               @Doc("打印数量") Double qty,
                                               @Doc("生产周期") Date productDate,
                                               @Doc("生产批次") String productLot,
                                               @Doc("批次属性") String lotAttr,
                                               @Doc("打印模板") String templateId,
                                               @Doc("供应商") String supplierId,
                                               @Doc("客户") String customerId) {
        return (Map<String, Object>) rec.getEnv().get("wms.initial_inventory").call("createLabelCode", materialId, qty, productDate, productLot, lotAttr, templateId, supplierId, customerId);
    }

    @ServiceMethod(label = "识别物料", auth = "read")
    public Map<String, Object> findMaterialByCode(Records record, String materialCode) {
        Environment env = record.getEnv();
        Map<String, Object> map = new HashMap<String, Object>();
        Records material = env.get("md.material").find(Criteria.equal("code", materialCode));
        if (!material.any()) {
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", materialCode);
            if (codes.length > 1) {
                material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            }else {
                throw new ValidationException(record.l10n("物料[%s]无法识别", materialCode));
            }
        }
        map.put("material_id", material.getId());
        map.put("material_code", material.getString("code"));
        map.put("unit_id", material.getRec("unit_id").getId());
        map.put("material_name_spec", material.getString("name_spec"));
        map.put("template_id", material.getRec("print_tpl_id").getId());
        map.put("material_category", material.get("category"));
        map.put("stock_rule", material.get("stock_rule"));
        map.put("lot_attr", !"num".equals(material.get("stock_rule")));
        return map;
    }

    @ServiceMethod(label = "识别库位", auth = "read")
    public Map<String, Object> findLocationByCode(Records record, String warehouseId, String locationCode) {
        Environment env = record.getEnv();
        Map<String, Object> map = new HashMap<String, Object>();
        Records location = env.get("md.store_location").find(Criteria.equal("warehouse_id", warehouseId).and(Criteria.equal("code", locationCode)));
        if (!location.any()) {
            throw new ValidationException(record.l10n("库位[%s],在仓库[%s]中不存在", locationCode,env.get("md.warehouse",warehouseId).getString("present")));
        }
        map.put("location_id", location.getId());
        return map;
    }

    @ServiceMethod(label = "初始化标签", auth = "read")
    public Map<String, Object> initial(Records record, Map<String, Object> data) {
        Environment env = record.getEnv();
        Map<String, Object> resultMap = new HashMap<>();
        if (Utils.toBoolean(data.get("is_store")) && Utils.isEmpty(data.get("location_id"))) {
            Records location = env.get("md.store_location").find(Criteria.equal("warehouse_id", data.get("warehouse_id")).and(Criteria.equal("code", data.get("location_code"))));
            if (!location.any()) {
                throw new ValidationException(record.l10n("库位[%s],在仓库[%s]中不存在", data.get("location_code"),env.get("md.warehouse", (String) data.get("warehouse_id")).getString("present")));
            }
            data.put("location_id",location.getId());
        }
        Map<String, Object> dataMap = (Map<String, Object>) record.getEnv().get("wms.initial_inventory").call("initial", data);
        String initialInventoryId = (String) dataMap.get("initialInventoryId");
        Records initial = env.get("wms.initial_inventory", initialInventoryId);
        boolean autoPrint = Utils.toBoolean(data.get("auto_print"));
        if (autoPrint) {
            return (Map<String, Object>) initial.call("reprintLabel");
        }
        return resultMap;
    }
}
