package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "wms.location_move", inherit = {"mixin.material"}, label = "库位移动")
public class LocationMove extends ValueModel {
    static Field code = Field.Char().label("条码").required();
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").lookup("searchWarehouse").required();
    static Field qty = Field.Float().label("数量").min(0D).required();
    static Field location_code_source = Field.Char().label("源库位");
    static Field location_code_target = Field.Char().label("目标库位").required();
    static Field msg = Field.Text().label("提示信息").store(false);

    /**
     * 带出当前用户有权限的仓库
     */
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    /**
     * @param records
     * @param code    条码（必填）
     * @return
     */
    public Map<String, Object> findMaterialByCode(Records records, @Doc("物料条码") String code) {
        Environment env = records.getEnv();
        Map<String, Object> result = new HashMap<>();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        Records label = env.get("lbl.material_label");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = label.find(Criteria.equal("sn", codes[0]));
            }
        } else {
            label = label.find(Criteria.equal("sn", code));
            if (!label.any()) {
                throw new ValidationException(records.l10n("条码[%s]无效", code));
            }
            material = label.getRec("material_id");
        }
        if (label.any()) {
            Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
            result.put("qty", label.get("qty"));
            result.put("code_type", "sn");
            result.put("location_code_source", onhand.getRec("location_id").getString("code"));
        }
        result.put("material_id", material.getPresent());
        result.put("material_name_spec", material.getString("name_spec"));
        result.put("unit_id", material.getRec("unit_id").getId());
        return result;
    }

    public Records findOnhand(Records records, String code, String warehouseId, String locationCodeSource) {
        // 1.验证参数
        if (Utils.isBlank(warehouseId)) {
            throw new ValidationException(records.l10n("请选择仓库"));
        }
        Environment env = records.getEnv();
        Records warehouse = env.get("md.warehouse", warehouseId);
        Records warehouses = env.getUser().getRec("warehouse_ids");
        if (!warehouses.contains(warehouse)) {
            throw new ValidationException(env.l10n("当前用户没有仓库[%s]权限", warehouse.get("present")));
        }
        Records location = env.get("md.store_location");
        if (Utils.isNotBlank(locationCodeSource)) {
            location = location.find(Criteria.equal("warehouse_id", warehouseId).and(Criteria.equal("code", locationCodeSource)));
            if (!location.any()) {
                throw new ValidationException(records.l10n("仓库[%s]不存在库位[%s]", warehouse.get("present"), locationCodeSource));
            }
        }
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        String lotNum = null;
        Records label = env.get("lbl.material_label");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                label = label.find(Criteria.equal("sn", codes[0]));
                lotNum = label.getString("lot_num");
            } else if ("lot".equals(stockRule)) {
                lotNum = codes[2];
            }
        } else {
            label = label.find(Criteria.equal("sn", code));
            if (!label.any()) {
                throw new ValidationException(records.l10n("条码[%s]无效", code));
            }
            material = label.getRec("material_id");
            lotNum = label.getString("lot_num");
        }
        Records onhand = env.get("stock.onhand");
        if (label.any()) {
            onhand = onhand.find(Criteria.equal("label_id", label.getId()));
            if (!onhand.any()) {
                throw new ValidationException(records.l10n("标签[%s]不在库", label.get("sn")));
            }
            if (!warehouse.equals(onhand.getRec("warehouse_id"))) {
                throw new ValidationException(env.l10n("标签当前在仓库[%s]", onhand.getRec("warehouse_id").getPresent()));
            }
            if (!Utils.equals(locationCodeSource, onhand.getRec("location_id").getString("code"))) {
                throw new ValidationException(env.l10n("标签当前在库位[%s]", onhand.getRec("location_id").getString("code")));
            }
        } else {
            Criteria criteria = Criteria.equal("material_id", material.getId()).and(Criteria.equal("label_id", null))
                .and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", location.getId()))
                .and(Criteria.equal("lot_num", lotNum));
            onhand = onhand.find(criteria);
            if (!onhand.any()) {
                if (location.any()) {
                    throw new ValidationException(records.l10n("条码[%s]不在库位[%s]", code, location.get("code")));
                }
                throw new ValidationException(records.l10n("条码[%s]不在库", code));
            }
        }
        return onhand;
    }

    public Object move(Records records,
                       @Doc("物料条码") String code,
                       @Doc("仓库id") String warehouseId,
                       @Doc("源库位") String locationCodeSource,
                       @Doc("库位条码") String locationCodeTarget,
                       @Doc("数量") double qty) {
        if (Utils.lessOrEqual(qty, 0d)) {
            throw new ValidationException(records.l10n("移动数量必须大于0"));
        }
        if (Utils.isEmpty(locationCodeTarget)) {
            throw new ValidationException(records.l10n("目标库位不能为空"));
        }
        if (Utils.equals(locationCodeSource, locationCodeTarget)) {
            throw new ValidationException(records.l10n("目标库位不能与源库位相同"));
        }
        Records onhand = findOnhand(records, code, warehouseId, locationCodeSource);
        if (Utils.less(onhand.getDouble("usable_qty"), qty)) {
            throw new ValidationException(records.l10n("移动数量不能超过[%s]", onhand.getDouble("usable_qty")));
        }
        // 获得目标库位
        Environment env = records.getEnv();
        Records warehouse = env.get("md.warehouse", warehouseId);
        Records locationTarget = env.get("md.store_location").find(Criteria.equal("warehouse_id", warehouseId).and(Criteria.equal("code", locationCodeTarget)));
        if (!locationTarget.any()) {
            throw new ValidationException(records.l10n("仓库[%s]不存在库位[%s]", warehouse.get("present"), locationCodeTarget));
        }
        // sn管控
        if (onhand.getRec("label_id").any()) {
            // 修改库存库位
            onhand.set("location_id", locationTarget.getId());
            // 修改标签库位
            onhand.getRec("label_id").set("location_id", locationTarget.getId());
        } else {
            Criteria criteria = Criteria.equal("location_id", locationTarget.getId())
                .and(Criteria.equal("material_id", onhand.getRec("material_id").getId()))
                .and(Criteria.equal("lot_num", onhand.get("lot_num")));
            // 获得新库位相同物料库存
            Records onhandMoveTarget = records.getEnv().get("stock.onhand").find(criteria);
            // 已存在物料,修改数量即可
            if (onhandMoveTarget.any()) {
                updateOnhandBySource(onhand, qty);
                Cursor cursor = env.getCursor();
                cursor.execute("update stock_onhand set usable_qty=usable_qty+%s,ok_qty=ok_qty+%s where id=%s", Utils.asList(qty, qty, onhandMoveTarget.getId()));
                cursor.execute("delete from stock_onhand where ok_qty=0 and ng_qty=0 and usable_qty=0 and id=%s", Utils.asList(onhand.getId()));
            } else {
                // 新库位不存在物料库存
                if (Utils.equals(onhand.getDouble("total_qty"), qty)) {
                    // 转移数量和总数一致时，修改库位即可
                    onhand.set("location_id", locationTarget.getId());
                } else {
                    updateOnhandBySource(onhand, qty);
                    // 转移数量和总数不一致，创建新库存保存
                    List<String> fieldNames = onhand.getMeta().getFields().values().stream().map(MetaField::getName).collect(Collectors.toList());
                    Map<String, Object> toCreate = onhand.read(fieldNames).get(0);
                    toCreate.put("location_id", locationTarget.getId());
                    toCreate.put("usable_qty", qty);
                    toCreate.put("ok_qty", qty);
                    toCreate.put("ng_qty", 0D);
                    toCreate.put("allot_qty", 0D);
                    toCreate.put("frozen_qty", 0D);
                    onhandMoveTarget.create(toCreate);
                }
            }
        }
        return records.l10n("仓库[%s]物料[%s]成功移动数量[%s]到库位[%s]", warehouse.get("present"), onhand.getRec("material_id").get("present"), qty, locationCodeTarget);
    }

    /**
     * 更新旧库位物料数量
     *
     * @param records
     * @param qty
     */
    public void updateOnhandBySource(Records records, double qty) {
        Cursor cursor = records.getEnv().getCursor();
        // 修改原来库位物料数量
        cursor.execute("update stock_onhand set usable_qty=usable_qty-%s,ok_qty=ok_qty-%s where usable_qty >= %s and id=%s", Utils.asList(qty, qty, qty, records.getId()));
        if (Utils.equals(cursor.getRowCount(), 0)) {
            throw new ValidationException(records.l10n("更新库存失败"));
        }
    }
}
