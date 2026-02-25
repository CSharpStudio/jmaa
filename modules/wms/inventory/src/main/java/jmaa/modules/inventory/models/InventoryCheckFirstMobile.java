package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.BinaryOp;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;


@Model.Meta(name = "wms.inventory_check_first_mobile", label = "仓库盘点-初盘-移动端", table = "wms_inventory_check", inherit = "wms.inventory_check")
public class InventoryCheckFirstMobile extends ValueModel {

    // 扫描标签  所有仓库都要盘点
    @ServiceMethod(auth = "read", label = "标签扫码")
    public Object scanLabelCode(Records record, String code, String warehouseId, String locationCode) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        String labelCode = null;
        if (codes.length > 1) {
            // 解析出来包括外部标签,外部标签这里会解析成功,当做正常标签处理
            material = material.find(Criteria.equal("code", codes[1]));
            labelCode = codes[0];
        } else {
            // 就一个序列号
            Records materialLable = env.get("lbl.material_label").find(Criteria.equal("sn", code));
            if (!materialLable.any()) {
                throw new ValidationException(record.l10n("标签[%s]无法识别,请检查标签数据", code));
            }
            material = materialLable.getRec("material_id");
            labelCode = code;
        }
        String locationId = checkLocation(record, locationCode);
        Records inventoryCheckLine = env.get("wms.inventory_check_line");
        Map<String, Object> resultMap = new HashMap<>();
        Criteria criteria = Criteria.equal("inventory_check_id", record.getId())
            .and(Criteria.equal("material_id", material.getId()));
        inventoryCheckLine = inventoryCheckLine.find(criteria);
        if (!inventoryCheckLine.any()) {
            throw new ValidationException(record.l10n("标签[%s]解析成功,但当前物料无需盘点,请检查数据", code));
        }
        criteria.and(Criteria.equal("sn", labelCode));
        Records details = env.get("wms.inventory_check_details").find(criteria);
        List<String> fields = Utils.asList("material_id", "material_name_spec");
        if (details.any()) {
            // 已经扫了,给提示,然后让修改
            resultMap.put("message", record.l10n("标签[%s]识别成功,库位[%s],可修改数量", code,details.getRec("location_id").getString("code")));
            // 仓库,库区,库位,物料,数量,名称,结果
            fields.add("qty");
            fields.add("blind_qty");
            fields.add("first_qty");
            Map<String, Object> data = details.read(fields).get(0);
            if (record.getBoolean("blind")) {
                data.put("blind_qty", "***");
            }
            resultMap.put("data", data);
        } else {
            // 不存在,那就生成
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("material_id", material.getId()).and(Criteria.equal("sn", labelCode)));
                if (!label.any()) {
                    throw new ValidationException(record.l10n("标签[%s]解析成功,但无标签数据记录,请检查数据", code));
                }
                // 之前已经冻结了,
                Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", label.getId()).and(Criteria.equal("warehouse_id", warehouseId)));
                Map<String, Object> data = null;
                if (onhand.any()) {
                    fields.add("usable_qty");
                    fields.add("location_id");
                    fields.add("frozen_qty");
                    data = onhand.read(fields).get(0);
                    if (record.getBoolean("blind")) {
                        data.put("blind_qty", "***");
                    } else {
                        data.put("blind_qty", data.get("frozen_qty"));
                    }
                    Records location = onhand.getRec("location_id");
                    if ((location.any() && !location.getId().equals(locationId)) || (!location.any() && null != locationId)) {
                        throw new ValidationException(record.l10n("标签[%s]识别成功,标签所属库位[%s]",
                            code, location.getString("code")));
                    }
                    // 库存数量对不上
                    data.put("qty", data.get("frozen_qty"));
                    resultMap.put("message", record.l10n("标签[%s]识别成功", code));
                } else {
                    //fields.add("qty");
                    data = label.read(fields).get(0);
                    resultMap.put("message", record.l10n("标签[%s]识别成功", code));
                }
                resultMap.put("data", data);
            } else if ("lot".equals(stockRule)) {
                // 校验是不是本地批次号,不是不能识别
                Records records = env.get("lbl.lot_num").find(Criteria.equal("material_id", material.getId()).and(Criteria.equal("code", codes[2])));
                if (!records.any()) {
                    throw new ValidationException(record.l10n("标签[%s]非系统批次标签,请检查数据",code));
                }
                // 看看有没有库存了,
                Records onhand = env.get("stock.onhand").find(Criteria.equal("material_id", material.getId())
                    .and(Criteria.equal("lot_num", codes[2])).and(Criteria.equal("warehouse_id", warehouseId)));
                // 这种可能存在多条,
                Map<String, Object> data = null;
                if (!onhand.any()) {
                    // 没库存数据
                    data = new HashMap<>();
                    data.put("material_id", material.getId());
                    data.put("material_name_spec", material.getString("name_spec"));
                    data.put("lot_num", codes[2]);
                    resultMap.put("message", record.l10n("标签[%s]识别成功", code));
                } else {
                    // 有库存的
                    fields.add("usable_qty");
                    fields.add("location_id");
                    fields.add("frozen_qty");
                    Optional<Records> locationOptional = null;
                    if (null == locationId) {
                        locationOptional = onhand.stream().filter(e -> !e.getRec("location_id").any()).findFirst();
                    } else {
                        locationOptional = onhand.stream().filter(e -> e.getRec("location_id").any() && e.getRec("location_id").getId().equals(locationId)).findFirst();
                    }
                    if (locationOptional.isPresent()) {
                        onhand = locationOptional.get();
                        data = onhand.read(fields).get(0);
                        data.put("qty", data.get("frozen_qty"));
                        resultMap.put("message", record.l10n("标签[%s]识别成功", code));
                    } else {
                        // 跟当前库位对不上,
                        onhand = onhand.first();
                        Records location = onhand.getRec("location_id");
                        throw new ValidationException(record.l10n("标签[%s]识别成功,标签所属库位[%s],请放回",
                            code, location.getString("code")));
                    }
                    if (record.getBoolean("blind")) {
                        data.put("blind_qty", "***");
                    } else {
                        data.put("blind_qty", data.get("qty"));
                    }
                }
                resultMap.put("data", data);
            } else {
                // 数量管控的,
                Records onhand = env.get("stock.onhand").find(Criteria.equal("material_id", material.getId()).and(Criteria.equal("warehouse_id", warehouseId)));
                Map<String, Object> data = null;
                if (onhand.any()) {
                    // 同上
                    fields.add("usable_qty");
                    fields.add("location_id");
                    fields.add("frozen_qty");
                    Optional<Records> locationOptional = null;
                    if (null == locationId) {
                        locationOptional = onhand.stream().filter(e -> !e.getRec("location_id").any()).findFirst();
                    } else {
                        locationOptional = onhand.stream().filter(e -> e.getRec("location_id").any() && e.getRec("location_id").getId().equals(locationId)).findFirst();
                    }
                    if (locationOptional.isPresent()) {
                        onhand = locationOptional.get();
                        data = onhand.read(fields).get(0);
                        data.put("qty", data.get("frozen_qty"));
                        resultMap.put("message", record.l10n("标签[%s]识别成功", code));
                    } else {
                        onhand = onhand.first();
                        Records location = onhand.getRec("location_id");
                        throw new ValidationException(record.l10n("标签[%s]识别成功,标签所属库位[%s],请放回",
                            code, location.getString("code")));
                    }
                    if (record.getBoolean("blind")) {
                        data.put("blind_qty", "***");
                    } else {
                        data.put("blind_qty", data.get("qty"));
                    }
                } else {
                    // 无库存数据的,
                    data = new HashMap<>();
                    data.put("material_id", material.getId());
                    data.put("material_name_spec", material.getString("name_spec"));
                    resultMap.put("message", record.l10n("标签[%s]识别成功", code));
                }
                resultMap.put("data", data);
            }
        }
        return resultMap;
    }

    @ServiceMethod(label = "扫码确认", auth = "read")
    public Object scanConfirm(Records record, String code, String warehouseId, String locationCode, Double qty, String materialId, Double firstQty) {
        Environment env = record.getEnv();
        // 校验标签
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        String labelCode = null;
        if (codes.length > 1) {
            // 解析出来包括外部标签,外部标签这里会解析成功,当做正常标签处理
            material = material.find(Criteria.equal("code", codes[1]));
            labelCode = codes[0];
        } else {
            // 就一个序列号
            Records materialLable = env.get("lbl.material_label").find(Criteria.equal("sn", code));
            if (!materialLable.any()) {
                throw new ValidationException(record.l10n("标签[%s]无法识别,请检查标签数据", code));
            }
            material = materialLable.getRec("material_id");
            labelCode = code;
        }
        String stockRule = material.getString("stock_rule");
        Records inventoryCheckDetails = env.get("wms.inventory_check_details");
        if ("approve".equals(record.getString("status"))) {
            record.set("status", "first_running");
        }
        // 回写line的数量
        String locationId = null;
        if (!Utils.isBlank(locationCode)) {
            locationId = env.get("md.store_location").find(Criteria.equal("warehouse_id",warehouseId).and(Criteria.equal("code", locationCode))).getId();
        }
        Records line = env.get("wms.inventory_check_line").find(Criteria.equal("inventory_check_id", record.getId()).and(Criteria.equal("material_id", material.getId())));
        // 必有, 前面就已经校验过了,
        Records details = inventoryCheckDetails.find(Criteria.equal("inventory_check_id", record.getId())
            .and(Criteria.equal("material_id", material.getId())).and(Criteria.equal("sn", labelCode)));
        Map<String, Object> resultMap = new HashMap<>();
        if (details.any()) {
            details.ensureOne();
            // 原来的数量
            double oldFirstQty = details.getDouble("first_qty");
            details.set("first_qty", firstQty);
            line.set("first_qty", Utils.round(line.getDouble("first_qty") + firstQty - oldFirstQty));
            resultMap.put("message", record.l10n("标签[%s],修改数量[%s]成功", code, firstQty));
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("inventory_check_id", record.getId());
            data.put("warehouse_id", warehouseId);
            data.put("location_id", locationId);
            data.put("material_id", materialId);
            data.put("sn", labelCode);
            data.put("qty", qty);
            String category = material.getString("category");
            if ("sn".equals(stockRule) || "semi-finished".equals(category) || "finished".equals(category)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("material_id", material.getId()).and(Criteria.equal("sn", labelCode)));
                label.set("status", "checking");
                data.put("label_id", label.getId());
                data.put("lot_num", label.get("lot_num"));
                // 操作日志
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.inventory_check:first_scan");
                log.put("related_id", record.getId());
                log.put("related_code", record.get("code"));
                label.call("logStatus", log);
            } else if ("lot".equals(stockRule)) {
                data.put("lot_num", codes[2]);
            }
            data.put("first_qty", firstQty);
            data.put("status", "first_running");
            resultMap.put("message", record.l10n("标签[%s]使用成功,数量[%s]", code, firstQty));
            inventoryCheckDetails.create(data);
            line.set("first_qty", Utils.round(line.getDouble("first_qty") + firstQty));
        }
        return resultMap;
    }

    // 处理定位明细查询
    @Override
    public Map<String, Object> searchByField(Records rec, String relatedField, Criteria criteria, Integer offset, Integer limit, Collection<String> fields, String order) {
        // todo 先定好, 如果是物料, 解析出来就只有1 , 如果是标签,那就按照标签正常解析
        if (criteria.hasField("code")) {
            String code = null;
            for (Object item : criteria) {
                BinaryOp binaryOp = (BinaryOp) item;
                Object field = binaryOp.getField();
                if ("code".equals(field)) {
                    criteria.remove(binaryOp);
                    code = (String) binaryOp.getValue();
                    break;
                }
            }
            String[] codes = (String[]) rec.getEnv().get("lbl.code_parse").call("parse", code);
            if (codes.length > 1) {
                // 物料编码
                code = codes[1];
            }
            criteria.and(Criteria.equal("material_id.code", code));
        }
        return super.searchByField(rec, relatedField, criteria, offset, limit, fields, order);
    }

    @ServiceMethod(auth = "read", label = "盘点完成")
    public void firstCheckFinal(Records rec) {
        Environment env = rec.getEnv();
        Records inventoryBalance = env.get("wms.inventory_balance");
        rec.set("status", "first_done");
        Records lineIds = rec.getRec("line_ids");
        Records detailsIds = rec.getRec("details_ids");
        lineIds.set("status", "first_done");
        detailsIds.set("status", "first_done");
        boolean secondFlag = rec.getBoolean("second_flag");
        if (!secondFlag) {
            // 不需要复盘就生成盘点平账单, 需要复盘的就不管
            Map<String, Object> balanceMap = new HashMap<>();
            balanceMap.put("inventory_check_id", rec.getId());
            inventoryBalance = inventoryBalance.create(balanceMap);
            lineIds.set("inventory_balance_id", inventoryBalance.getId());
            detailsIds.set("inventory_balance_id", inventoryBalance.getId());
            rec.set("status", "done");
        }
    }

    @ServiceMethod(auth = "read", label = "校验库位")
    public String checkLocation(Records record, String locationCode) {
        Records warehouseId = record.getRec("warehouse_id");
        Records storeAreaIds = record.getRec("store_area_ids");
        Records storeLocationIds = record.getRec("store_location_ids");
        if (Utils.isNotEmpty(locationCode)) {
            Criteria criteria = Criteria.equal("warehouse_id", warehouseId.getId());
            if (storeAreaIds.any()) {
                criteria.and(Criteria.in("area_id", Utils.asList(storeAreaIds.getIds())));
            }
            if (storeLocationIds.any()) {
                criteria.and(Criteria.in("id", Utils.asList(storeLocationIds.getIds())));
            }
            criteria.and(Criteria.equal("code", locationCode));
            Records location = record.getEnv().get("md.store_location").find(criteria);
            if (!location.any()) {
                throw new ValidationException(record.l10n("库位[%s]不存在/非当前盘点单需盘库位", locationCode));
            }
            return location.getId();
        } else {
            // 没有库位, 盘点整个库位的时候才让空,如果不是,则报错
            if (storeLocationIds.any() || storeLocationIds.any()) {
                // 存在一个就报错
                throw new ValidationException(record.l10n("请先扫描库位"));
            }
            return null;
        }
    }
}

