package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author eric
 */
@Model.Meta(inherit = "stock.stock_in")
public class StockIn extends Model {
    static Field details_ids = Field.One2many("stock.stock_in_details", "stock_in_id").label("入库明细");

    /**
     * 标签条码/批次号/LPN/物料编码 入库
     */
    @ServiceMethod(label = "扫码入库")
    public Object stockIn(Records record,
                          @Doc("标签条码/批次号/LPN/物料编码") String code,
                          @Doc("物料ID") String materialId,
                          @Doc("确认数量") Double confirmQty,
                          @Doc("库位编码/ID") String location,
                          @Doc("是否确认提交") boolean submit) {
        if ("mfg.product_storage_notice".equals(record.getString("type")) || "wms.sales_return".equals(record.getString("type"))) {
            // 成品入库
            // TODO 销售退货跟成品入库有差异，不能通过最外层包含入库，退回来什么标签就入库什么标签
            return productStockIn(record, code, location, submit);
        } else {
            return materialStockIn(record, code, materialId, confirmQty, location, submit, true);
        }
    }

    /**
     * 产成品入库
     */
    public Object productStockIn(Records record, String code, String location, boolean submit) {
        // 成品入库, 解析成品标签,包装标签 后续待定
        Environment env = record.getEnv();
        Records productLabel = null;
        // lbl.product_label 其实跟lbl.material_label 同一个表
        if (env.getRegistry().contains("lbl.product_label")) {
            productLabel = env.get("lbl.product_label");
        }
        if (null != productLabel && (productLabel = productLabel.find(Criteria.equal("sn", code))).any()) {
            // 有数据
            return stockInWithLabel(record, code, productLabel, location, submit);
        }
        Records mdPackage = env.get("packing.package");
        if (env.getRegistry().contains("packing.package")) {
            mdPackage = env.get("packing.package");
        }
        if (null != mdPackage && (mdPackage = mdPackage.find(Criteria.equal("code", code))).any()) {
            // 判断是不是最外层条码?
            if (mdPackage.getRec("parent_id").any()) {
                //TODO 销售退货应该允许扫描内包装入库
                throw new ValidationException("请直接扫描最外层包装标签");
            }
            // 不需要校验是否关联到内包,后续会加包装拆分功能,
            return stockInWithPackage(record, mdPackage, location, submit);
        }
        throw new ValidationException(record.l10n("标签[%s]无法识别", code));
    }

    public Object stockInWithPackage(Records records, Records mdPackage, String locationCode, boolean submit) {
        Records material = mdPackage.getRec("material_id");
        String code = mdPackage.getString("code");
        Records details = findAndLockDetails(records, material);
        Records toStock = details.filter(r -> Utils.equals("to-stock", r.getString("status")));
        if (!toStock.any()) {
            throw new ValidationException(records.l10n("物料[%s]无待入库明细", material.get("code")));
        }
        toStock = toStock.filter(r -> Utils.equals(code, r.getString("sn")));
        if (!toStock.any()) {
            throw new ValidationException(records.l10n("包装标签号[%s]无待入库明细", code));
        }
        Records warehouse = getWarehouse(records, toStock);
        Map<String, Object> result = new HashMap<>();
        double qty = mdPackage.getDouble("package_qty");
        if (submit) {
            Records location = findLocation(records, warehouse, locationCode);
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                toStock.call("stockInWithLabel", location);
            } else {
                toStock.call("stockInWithQty", qty, location);
            }
            result.put("submit", true);
            result.put("message", records.l10n("条码[%s]确认成功，入库数量[%s]", code, qty));
        } else {
            result.put("message", records.l10n("条码[%s]识别成功，待确认", code));
        }
        Map<String, Object> stockInInfo = getStockInInfo(records, details, qty);
        result.put("data", stockInInfo);
        return result;
    }

    /**
     * 原材料入库
     */
    public Object materialStockIn(Records record, String code, String materialId, Double confirmQty, String location, boolean submit, boolean changeable) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            if (material.any()) {
                if (!Utils.equals(materialId, material.getId())) {
                    if (!changeable) {
                        throw new ValidationException(env.l10n("标签物料[%s]与当前入库物料不一致", material.get("code")));
                    }
                    //物料切换需要确认操作
                    submit = false;
                }
                String stockRule = material.getString("stock_rule");
                if ("sn".equals(stockRule)) {
                    Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                    if (label.any()) {
                        return stockInWithLabel(record, label.getString("sn"), label, location, submit);
                    }
                } else {
                    return stockInWithLotOrNum(record, material, confirmQty, location, codes, submit);
                }
            }
        }
        Records pkg = env.get("packing.package").find(Criteria.equal("code", code));
        if (pkg.any()) {
            Records material = pkg.getRec("material_id");
            if (!Utils.equals(materialId, material.getId())) {
                if (!changeable) {
                    throw new ValidationException(env.l10n("标签物料[%s]与当前入库物料不一致", material.get("code")));
                }
                //物料切换需要确认操作
                submit = false;
            }
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records labels = env.get("lbl.material_label").find(Criteria.equal("package_id", pkg.getId()).and("status", "=", "received"));
                if (!labels.any()) {
                    throw new ValidationException(record.l10n("包装标签[%s]没找到待入库物料标签", code));
                }
                return stockInWithLabel(record, code, labels, location, submit);
            } else if ("num".equals(stockRule)) {
                return stockInWithLotOrNum(record, material, confirmQty, location, new String[]{Utils.toString(pkg.getDouble("qty"))}, submit);
            } else {
                // 批次包装
                Records lotPackage = env.get("md.lot_package").find(Criteria.equal("package_id", pkg.getId()).and(Criteria.equal("material_id", material.getId())));
                if (!lotPackage.any()) {
                    throw new ValidationException(record.l10n("批次包装标签[%s]无关联批次数据,请检查数据", code));
                }
                return stockInWithLotPackage(record, material, confirmQty, location, lotPackage, code, submit);
            }
        }
        Records material = env.get("md.material").find(Criteria.equal("code", code));
        if (material.any()) {
            if (!Utils.equals(materialId, material.getId())) {
                if (!changeable) {
                    throw new ValidationException(env.l10n("标签物料[%s]与当前入库物料不一致", material.get("code")));
                }
                //物料切换需要确认操作
                submit = false;
            }
            String stockRule = material.getString("stock_rule");
            if (!"num".equals(stockRule)) {
                throw new ValidationException(record.l10n("[%s]物料[%s]不能使用物料编码入库", material.getSelection("stock_rule"), material.get("code")));
            }
            return stockInWithLotOrNum(record, material, confirmQty, location, null, submit);
        }
        // 委外采购入库,也会走这里, 也行吧
        Records label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
        if (label.any()) {
            return stockInWithLabel(record, label.getString("sn"), label, location, submit);
        }
        throw new ValidationException(record.l10n("条码[%s]无法识别", code));
    }

    /**
     * 批次或者数量入库
     */
    public Object stockInWithLotOrNum(Records records, Records material, Double confirmQty, String locationCode, String[] codes, boolean submit) {
        Environment env = records.getEnv();
        Map<String, Object> result = new HashMap<>();
        String stockRule = material.getString("stock_rule");
        String lotNum = null;
        String sn = null;
        Double labelQty = null;
        if (Utils.isNotEmpty(codes)) {
            labelQty = Utils.toDouble(codes[codes.length - 1]);
            if ("lot".equals(stockRule)) {
                lotNum = codes[2];
                sn = codes[0];
            }
        }
        boolean lotInQtyFlag = records.getEnv().getConfig().getBoolean("lot_in_qty");
        if ("lot".equals(stockRule) && lotInQtyFlag) {
            // 先判断当前条码是否使用
            Records lotStatus = env.get("lbl.lot_status").find(Criteria.equal("order_id", records.getId())
                .and(Criteria.equal("type", "stock.stock_in")).and(Criteria.equal("material_id",material.getId()))
                .and(Criteria.equal("lot_num", lotNum)).and(Criteria.equal("sn", sn)));
            if (lotStatus.any()) {
                throw new ValidationException(records.l10n("当前批次标签序列号[%s]已有记录，不能重复使用", sn));
            }
        }
        Records details = findAndLockDetails(records, material);
        Records toStock = details.filter(r -> "to-stock".equals(r.getString("status")));
        if (!toStock.any()) {
            throw new ValidationException(records.l10n("物料[%s]无待入库明细", material.get("code")));
        }
        if (Utils.isNotEmpty(lotNum)) {
            String lot = lotNum;
            toStock = toStock.filter(r -> Utils.equals(lot, r.getString("lot_num")));
        }
        if (!toStock.any()) {
            throw new ValidationException(records.l10n("批次号[%s]无待入库明细", lotNum));
        }
        Records warehouse = getWarehouse(records, toStock);
        if (confirmQty == null) {
            confirmQty = labelQty;
        }
        if (submit && confirmQty != null) {
            Records location = findLocation(records, warehouse, locationCode);
            // 新增到临时表
            if ("lot".equals(stockRule) && lotInQtyFlag) {
                Map<String, Object> data = new HashMap<>();
                data.put("order_id", records.getId());
                data.put("sn", sn);
                data.put("material_id", material.getId());
                data.put("lot_num", lotNum);
                data.put("type", "stock.stock_in");
                env.get("lbl.lot_status").create(data);
            }
            toStock.call("stockInWithQty", confirmQty, location);
            result.put("data", getStockInInfo(records, details, labelQty));
            result.put("submit", true);
            result.put("message", records.l10n("物料[%s]确认成功，入库数量[%s]", material.get("code"), confirmQty));
            return result;
        }
        Map<String, Object> stockInInfo = getStockInInfo(records, details, labelQty);
        result.put("data", stockInInfo);
        result.put("message", records.l10n("物料[%s]识别成功，待确认", material.get("code")));
        return result;
    }

    /**
     * 按批次包装入库
     */
    public Object stockInWithLotPackage(Records records, Records material, Double confirmQty, String locationCode, Records lotPackage, String code, boolean submit) {
        Environment env = records.getEnv();
        Map<String, Object> result = new HashMap<>();
        Records details = findAndLockDetails(records, material);
        Records toStock = details.filter(r -> "to-stock".equals(r.getString("status")));
        if (!toStock.any()) {
            throw new ValidationException(records.l10n("物料[%s]无待入库明细", material.get("code")));
        }
        double pkgQty = lotPackage.stream().mapToDouble(e -> e.getDouble("qty")).sum();
        double toStockQty = Utils.round(toStock.stream().mapToDouble(d -> d.getDouble("qty") - d.getDouble("stock_qty") - d.getDouble("return_qty")).sum());
        Records warehouse = getWarehouse(records, toStock);
        if (submit) {
            if (confirmQty == null) {
                confirmQty = Math.min(pkgQty, toStockQty);
            }
            if (Utils.large(confirmQty, toStockQty)) {
                throw new ValidationException(env.l10n("入库数量[%s]不能大于待入库数量[%s]", confirmQty, toStockQty));
            }
            Records location = findLocation(records, warehouse, locationCode);
            // 第一次扫码时,已经控制了数量,这次进来了数量不会乱
            // 一个包装标签,可能对应多个批次号,
            // 例子: 包装标签  PKG  500
            // 批次号    P1     400
            //          p2     100
            double leftQty = confirmQty;
            for (Records pkg : lotPackage) {
                String lotNum = pkg.getString("lot_num");
                double qty = pkg.getDouble("qty");
                // 按照例子记录, 这里只能查到1条数据, 要么400 要么100
                // 同时,他可能拆包单独扫, 如果数量对不上,报错,让他自己慢慢扫,不然数量就有问题,
                // 批次包装只是为了方便, 非要拿出来单独扫,那就全部扫完,不然就只扫外包编码,做不到既要又要
                Records lotToStock = toStock.filter(d -> Utils.equals(lotNum, d.getString("lot_num")));
                if (lotToStock.any()) {
                    // 当前明细的数量
                    double detailQty = Utils.round(lotToStock.stream().mapToDouble(d -> d.getDouble("qty") - d.getDouble("stock_qty") - d.getDouble("return_qty")).sum());
                    double stockQty = Math.min(detailQty, qty);
                    if (Utils.largeOrEqual(leftQty, stockQty)) {
                        lotToStock.call("stockInWithQty", stockQty, location);
                        leftQty = Utils.round(leftQty - stockQty);
                    } else {
                        lotToStock.call("stockInWithQty", leftQty, location);
                        leftQty = 0d;
                    }
                }
            }
            if (!Utils.equals(leftQty, 0d)) {
                // 扫包装,数量肯定要用完,如果没用完,就肯定有问题, 正常不会有问题,不会进入这里
                throw new ValidationException("入库数量与批次包装数量不一致，请使用批次标签入库");
            }
            result.put("data", getStockInInfo(records, details, confirmQty));
            result.put("submit", true);
            result.put("message", records.l10n("物料[%s]确认成功，入库数量[%s]", material.get("code"), confirmQty));
            return result;
        }
        Map<String, Object> data = getStockInInfo(records, details, pkgQty);
        data.put("pkg", true);
        result.put("data", data);
        result.put("message", records.l10n("物料[%s]识别成功，待确认", material.get("code")));
        return result;
    }

    /**
     * 条码入库
     */
    public Object stockInWithLabel(Records records, String code, Records labels, String locationCode, boolean submit) {
        List<String> labelIds = labels.stream().map(r -> r.getId()).collect(Collectors.toList());
        Records material = labels.first().getRec("material_id");
        Records details = findAndLockDetails(records, material);
        Records toStock = details.filter(r -> labelIds.contains(r.getRec("label_id").getId()));
        if (!toStock.any()) {
            throw new ValidationException(records.l10n("条码[%s]无待入库明细", code));
        }
        Records warehouse = getWarehouse(records, toStock);
        for (Records row : toStock) {
            if (!"to-stock".equals(row.getString("status"))) {
                Records label = row.getRec("label_id");
                throw new ValidationException(records.l10n("条码[%s]入库状态为[%s]，无法使用", label.get("sn"), row.getSelection("status")));
            }
        }
        double qty = Utils.round(toStock.stream().mapToDouble(d -> d.getDouble("qty")).sum());
        double labelQty = Utils.round(labels.stream().mapToDouble(d -> d.getDouble("qty")).sum());
        Map<String, Object> result = new HashMap<>();
        if (submit) {
            boolean locationManage = warehouse.getBoolean("location_manage");
            if (locationManage && Utils.isEmpty(locationCode)) {
                throw new ValidationException(records.l10n("仓库[%s]启用库位管理，库位不能为空", warehouse.get("present")));
            }
            Records location = findLocation(records, warehouse, locationCode);
            toStock.call("stockInWithLabel", location);
            result.put("submit", true);
            result.put("message", records.l10n("条码[%s]确认成功，入库数量[%s]", code, qty));
        } else {
            result.put("message", records.l10n("条码[%s]识别成功，待确认", code));
        }
        Map<String, Object> stockInInfo = getStockInInfo(records, details, labelQty);
        result.put("data", stockInInfo);
        return result;
    }

    public Records findAndLockDetails(Records records, Records material) {
        Records details = records.getEnv().get("stock.stock_in_details").find(Criteria.equal("stock_in_id", records.getId())
            .and("material_id", "=", material.getId()));
        Cursor cr = records.getEnv().getCursor();
        //事务锁定明细行，防止并发
        cr.execute("update stock_stock_in_details set id=id where id in %s", Utils.asList(Utils.asList(details.getIds())));
        return details;
    }

    public Map<String, Object> getStockInInfo(Records records, Records details, Double qty) {
        double left_qty = Utils.round(details.stream().mapToDouble(d -> d.getDouble("qty") - d.getDouble("stock_qty") - d.getDouble("return_qty")).sum());
        double should_qty = Utils.round(details.stream().mapToDouble(d -> d.getDouble("qty") - d.getDouble("return_qty")).sum());
        double stock_qty = Utils.round(details.stream().mapToDouble(d -> d.getDouble("stock_qty")).sum());
        Records detail = details.first();
        Map<String, Object> data = new HashMap<>();
        Records material = detail.getRec("material_id");
        Records unit = material.getRec("unit_id");
        boolean lockQty = false;
        String stockRule = material.getString("stock_rule");
        if ("sn".equals(stockRule)) {
            lockQty = true;
        } else if ("lot".equals(stockRule)) {
            lockQty = records.getEnv().getConfig().getBoolean("lot_in_qty");
        }
        data.put("lock_qty", lockQty);
        data.put("unit_id", unit.getPresent());
        data.put("unit_accuracy", unit.getInteger("accuracy"));
        data.put("stock_rule", material.get("stock_rule"));
        data.put("material_id", material.getPresent());
        data.put("material_name_spec", material.get("name_spec"));
        data.put("should_qty", should_qty);
        data.put("stock_qty", stock_qty);
        data.put("left_qty", left_qty);
        data.put("confirm_qty", qty == null ? left_qty : qty);
        Records warehouse = detail.getRec("warehouse_id");
        data.put("warehouse_id", warehouse.getPresent());
        data.put("location_manage", warehouse.get("location_manage"));
        data.put("suggest_location", findSuggestLocation(records, warehouse.getId(), material.getId()));
        return data;
    }

    public String findSuggestLocation(Records records, String warehouseId, String materialId) {
        Records onhand = records.getEnv().get("stock.onhand").find(Criteria.equal("material_id", materialId)
            .and(Criteria.equal("warehouse_id", warehouseId)), 0, 1, "stock_in_time desc");
        Records location = onhand.getRec("location_id");
        if (location.any()) {
            return location.getString("code");
        } else {
            Records locations = (Records) records.getEnv().get("md.material").call("findLocation");
            for (Records l : locations) {
                if (Utils.equals(l.getRec("warehouse_id").getId(), warehouseId)) {
                    return l.getString("code");
                }
            }
        }
        return null;
    }

    public void updateStockInStatus(Records stockIn) {
        stockIn.flush();
        Cursor cr = stockIn.getEnv().getCursor();
        String sql = "select distinct status from stock_stock_in_details where stock_in_id=%s";
        for (Records row : stockIn) {
            cr.execute(sql, Arrays.asList(row.getId()));
            boolean done = cr.fetchAll().stream().map(r -> (String) r[0]).allMatch("done"::equals);
            if (done) {
                row.set("status", "done");
                stockIn.getEnv().get("lbl.lot_status").find(Criteria.equal("order_id", row.getId())
                    .and(Criteria.equal("type", "stock.stock_in"))).delete();
            } else {
                row.set("status", "stocking");
            }
        }
    }

    /**
     * 获取入库仓库，校验仓库权限
     */
    public Records getWarehouse(Records records, Records details) {
        Records warehouse = details.first().getRec("warehouse_id");
        Records warehouses = records.getEnv().getUser().getRec("warehouse_ids");
        if (!warehouses.contains(warehouse)) {
            throw new ValidationException(records.l10n("当前用户没有仓库[%s]权限", warehouse.get("present")));
        }
        return warehouse;
    }

    /**
     * 根据库位编码或者id查找库位
     */
    public Records findLocation(Records records, Records warehouse, String locationCode) {
        Records location = records.getEnv().get("md.store_location");
        if (Utils.isNotEmpty(locationCode)) {
            location = location.find(Criteria.equal("warehouse_id", warehouse.getId())
                .and(Criteria.equal("id", locationCode).or(Criteria.equal("code", locationCode))));
            if (!location.any()) {
                throw new ValidationException(records.l10n("仓库[%s]不存在库位[%s]", warehouse.getString("present"), locationCode));
            }
        }
        boolean locationManage = warehouse.getBoolean("location_manage");
        if (locationManage && !location.any()) {
            throw new ValidationException(records.l10n("仓库[%s]启用库位管理，库位不能为空", warehouse.get("present")));
        }
        return location;
    }

    /**
     * 更新采购入库数量
     */
    public void updatePoStockIn(Records records, Map<String, Double> materialQty) {
        Cursor cr = records.getEnv().getCursor();
        for (Records record : records) {
            if (!"purchase".equals(record.getString("type"))) {
                continue; // 只有采购入库单才需要回写 采购订单数
            }
            Records receipt = record.getRec("related_id");
            if (Utils.isEmpty(receipt)) {
                continue;  // 没有接收单到这里,应该不存在这种
            }
            //改用sql加锁防止并发
            cr.execute("select po_line_id from wms_material_receipt_line where receipt_id=%s and material_id in %s and po_line_id is not null",
                Utils.asList(receipt.getId(), materialQty.keySet()));
            List<String> poLineIds = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
            cr.execute("update purchase_order_line set id=id where id in %s", Utils.asList(poLineIds));
            Records poLines = record.getEnv().get("purchase.order_line", poLineIds);
            for (Records poLine : poLines) {
                Records material = poLine.getRec("material_id");
                double qty = Utils.toDouble(materialQty.get(material.getId()));
                if (Utils.equals(qty,0d)){
                    continue;
                }
                double leftQty = Utils.round(poLine.getDouble("purchase_qty") - poLine.getDouble("stock_in_qty"));
                if (Utils.lessOrEqual(leftQty, 0)) {
                    continue;
                }
                if (Utils.lessOrEqual(qty, leftQty)) {
                    materialQty.remove(material.getId());
                    poLine.set("stock_in_qty", qty + poLine.getDouble("stock_in_qty"));
                } else {
                    poLine.set("stock_in_qty", leftQty + poLine.getDouble("stock_in_qty"));
                    materialQty.put(material.getId(), Utils.round(qty - leftQty));
                }
            }
        }
    }
}
