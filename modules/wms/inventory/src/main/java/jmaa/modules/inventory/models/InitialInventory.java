package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;

@Model.Meta(name = "wms.initial_inventory", label = "初始化标签", inherit = {"mixin.material"})
public class InitialInventory extends Model {
    static Field label_id = Field.Many2one("lbl.material_label").label("标签条码");
    static Field sn = Field.Char().label("条码");
    static Field qty = Field.Float().label("数量").required();
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库");
    static Field lot_num = Field.Char().label("批次号");
    static Field product_lot = Field.Char().label("生产批次");
    static Field product_date = Field.Date().label("生产日期").required();
    static Field template_id = Field.Many2one("print.template").label("标签模板").required();
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field is_store = Field.Boolean().label("是否入库").defaultValue("true");

    @ActionMethod
    public Action onMaterialChange(Records rec) {
        AttrAction action = Action.attr();
        Records material = rec.getRec("material_id");
        Records unit = material.getRec("unit_id");
        action.setValue("unit_id", unit);
        action.setValue("unit_accuracy", unit.get("accuracy"));
        action.setValue("material_name_spec", material.getString("name_spec"));
        action.setValue("template_id", material.getRec("print_tpl_id"));
        action.setValue("material_category", material.get("category"));
        action.setValue("stock_rule", material.get("stock_rule"));
        action.setVisible("lot_attr", !"num".equals(material.get("stock_rule")));
        action.setVisible("lpn", "sn".equals(material.get("stock_rule")));
        return action;
    }

    // 生成条码
    @ServiceMethod(label = "生成条码")
    public Map<String, Object> createLabelCode(Records rec,
                                               @Doc("物料ID") String materialId,
                                               @Doc("打印数量") Double qty,
                                               @Doc("生产周期") Date productDate,
                                               @Doc("生产批次") String productLot,
                                               @Doc("批次属性") String lotAttr,
                                               @Doc("打印模板") String templateId,
                                               @Doc("供应商") String supplierId,
                                               @Doc("客户") String customerId) {
        Environment env = rec.getEnv();
        Map<String, Object> data = new HashMap<>();
        if (Utils.isNotEmpty(supplierId)) {
            data.put("supplier_id", supplierId);
        }
        if (Utils.isNotEmpty(customerId)) {
            data.put("customer_id", customerId);
        }
        return (Map<String, Object>) env.get("lbl.material_label").call("printLabel", materialId, qty, qty, productDate, productLot, lotAttr, templateId, null, data);
    }

    @ServiceMethod(label = "初始化标签")
    public Map<String, Object> initial(Records record, Map<String, Object> data) {
        Map<String, Object> resultMap = new HashMap<>();
        String sn = (String) data.get("sn");
        if (record.find(Criteria.equal("sn", sn)).any()) {
            throw new ValidationException(record.l10n("当前标签已初始化,请不要重复保存"));
        }
        record = record.create(data);
        resultMap.put("initialInventoryId", record.getId());
        Records material = record.getRec("material_id");
        String stockRule = material.getString("stock_rule");
        Records materialLabel = null;
        if ("sn".equals(stockRule)) {
            materialLabel = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", sn));
            if (!materialLabel.any()) {
                throw new ValidationException(record.l10n("请重新生成条码"));
            }
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.initial_inventory:initial");
            materialLabel.call("logStatus", log);
        }
        boolean isStore = record.getBoolean("is_store");
        if (isStore) {
            Environment env = record.getEnv();
            Records stockOnhand = env.get("stock.onhand");
            Records warehouse = null;
            if (Utils.isEmpty(data.get("warehouse_id"))) {
                String locationId = (String) data.get("location_id");
                Records location = env.get("md.store_location", locationId);
                warehouse = location.getRec("warehouse_id");
            } else {
                warehouse = env.get("md.warehouse", (String) data.get("warehouse_id"));
            }
            // 从实际场景出发,
            String warehouseId = warehouse.getId();
            if ("sn".equals(stockRule)) {
                String materialLabelId = materialLabel.getId();
                Map<String, Object> onhandData = new HashMap<>();
                onhandData.put("material_id", data.get("material_id"));
                onhandData.put("ok_qty", data.get("qty"));
                onhandData.put("usable_qty", data.get("qty"));
                onhandData.put("company_id", warehouse.getRec("company_id").getId());
                onhandData.put("lot_num", data.get("lot_num"));
                onhandData.put("warehouse_id", warehouseId);
                onhandData.put("location_id", data.get("location_id"));
                onhandData.put("stock_in_time", new Date());
                onhandData.put("status", "onhand");
                onhandData.put("label_id", materialLabelId);
                onhandData.put("sn", sn);
                stockOnhand.create(onhandData);
                materialLabel.set("status", "onhand");
            } else if ("lot".equals(stockRule)) {
                String lot_num = (String) data.get("lot_num");
                stockOnhand = stockOnhand.find(Criteria.equal("lot_num", lot_num)
                    .and(Criteria.equal("material_id", data.get("material_id")))
                    .and(Criteria.equal("warehouse_id", warehouseId))
                    .and(Criteria.equal("location_id", data.get("location_id"))));
                if (stockOnhand.any()) {
                    stockOnhand.set("ok_qty", Utils.round(stockOnhand.getDouble("ok_qty") + Utils.toDouble(data.get("qty"))));
                    stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") + Utils.toDouble(data.get("qty"))));
                } else {
                    Map<String, Object> onhandData = new HashMap<>();
                    onhandData.put("material_id", data.get("material_id"));
                    onhandData.put("ok_qty", data.get("qty"));
                    onhandData.put("lot_num", lot_num);
                    onhandData.put("usable_qty", data.get("qty"));
                    onhandData.put("company_id", warehouse.getRec("company_id").getId());
                    onhandData.put("warehouse_id", warehouse.getId());
                    onhandData.put("location_id", data.get("location_id"));
                    onhandData.put("stock_in_time", new Date());
                    onhandData.put("status", "onhand");
                    stockOnhand.create(onhandData);
                }
            } else {
                // 数量管控数据,
                stockOnhand = stockOnhand.find(Criteria.equal("material_id", data.get("material_id"))
                    .and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", data.get("location_id"))));
                if (stockOnhand.any()) {
                    stockOnhand.set("ok_qty", Utils.round(stockOnhand.getDouble("ok_qty") + Utils.toDouble(data.get("qty"))));
                    stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") + Utils.toDouble(data.get("qty"))));
                } else {
                    Map<String, Object> onhandData = new HashMap<>();
                    onhandData.put("material_id", data.get("material_id"));
                    onhandData.put("ok_qty", data.get("qty"));
                    onhandData.put("usable_qty", data.get("qty"));
                    onhandData.put("company_id", warehouse.getRec("company_id").getId());
                    onhandData.put("warehouse_id", warehouse.getId());
                    onhandData.put("location_id", data.get("location_id"));
                    onhandData.put("stock_in_time", new Date());
                    onhandData.put("status", "onhand");
                    stockOnhand.create(onhandData);
                }
            }
        }
        return resultMap;
    }

    @Override
    public boolean delete(Records records) {
        for (Records rec : records) {
            Environment env = rec.getEnv();
            boolean isStore = rec.getBoolean("is_store");
            String sn = rec.getString("sn");
            Records material = rec.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", sn));
                label.set("status", "delete-initial");
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.initial_inventory:delete");
                label.call("logStatus", log);
            }
            if (isStore) {
                String materialId = material.getId();
                String warehouseId = rec.getRec("warehouse_id").getId();
                String locationId = rec.getRec("location_id").getId();
                if ("sn".equals(stockRule)) {
                    Records label = env.get("lbl.material_label").find(Criteria.equal("sn", sn));
                    env.get("stock.onhand").find(Criteria.equal("label_id", label.getId())).delete();
                } else if ("lot".equals(stockRule)) {
                    String lotNumStr = rec.getString("lot_num");
                    double qty = rec.getDouble("qty");
                    Records onhand = env.get("stock.onhand").find(Criteria.equal("lot_num", lotNumStr)
                        .and(Criteria.equal("warehouse_id", warehouseId))
                        .and(Criteria.equal("location_id", locationId))
                        .and(Criteria.equal("material_id", materialId)));
                    double usableQty = onhand.getDouble("usable_qty");
                    if (Utils.large(usableQty, qty)) {
                        onhand.set("ok_qty", Utils.round(onhand.getDouble("ok_qty") - qty));
                        onhand.set("usable_qty", Utils.round(onhand.getDouble("usable_qty") - qty));
                    } else {
                        onhand.delete();
                    }
                } else {
                    double qty = rec.getDouble("qty");
                    Records onhand = env.get("stock.onhand").find(Criteria.equal("material_id", materialId)
                        .and(Criteria.equal("warehouse_id", warehouseId))
                        .and(Criteria.equal("location_id", locationId)));
                    double usableQty = onhand.getDouble("usable_qty");
                    if (Utils.large(usableQty, qty)) {
                        onhand.set("ok_qty", Utils.round(onhand.getDouble("ok_qty") - qty));
                        onhand.set("usable_qty", Utils.round(onhand.getDouble("usable_qty") - qty));
                    } else {
                        onhand.delete();
                    }
                }
            }
            rec.callSuper(InitialInventory.class, "delete");
        }
        return true;
    }

    @ServiceMethod(label = "条码打印")
    public Map<String, Object> reprintLabel(Records records) {
        Records printTemplate = records.first().getRec("template_id");
        for (Records record : records) {
            if (!Utils.equals(printTemplate.getId(), record.getRec("template_id").getId())) {
                throw new ValidationException(records.l10n("补打的标签存在不同的打印模板"));
            }
        }
        List<Map<String, Object>> labels = new ArrayList<>();
        for (Records record : records) {
            Map<String, Object> map = new HashMap<>();
            Records material = record.getRec("material_id");
            Records supplier = record.getRec("supplier_id");
            map.put("material_code", material.getString("code"));
            map.put("material_name", material.getString("name"));
            map.put("material_spec", material.getString("spec"));
            map.put("unit", material.getRec("unit_id").get("name"));
            map.put("supplier_name", supplier.getString("name"));
            map.put("supplier_chars", supplier.get("chars"));
            map.put("supplier_code", supplier.get("code"));
            map.put("product_lot", record.get("product_lot"));
            map.put("product_date", Utils.format(record.getDate("product_date"), "yyyy-MM-dd"));
            map.put("qty", record.get("qty"));
            map.put("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            // code是给二维码用的,
            // 进过测试,批次号管控需要拼接
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                map.put("code", record.getString("sn") + "|" + material.getString("code"));
            } else {
                map.put("code", record.getString("sn") + "|" + material.getString("code") + "|" + record.getString("lot_num") + "|" + record.get("qty"));
            }
            map.put("sn", record.getString("sn"));
            map.put("lot_num", record.getString("lot_num"));
            labels.add(map);
        }
        return (Map<String, Object>) printTemplate.call("print", new HashMap<String, Object>() {{
            put("data", labels);
        }});
    }
}
