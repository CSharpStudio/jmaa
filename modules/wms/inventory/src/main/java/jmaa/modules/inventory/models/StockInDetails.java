package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.ServerDate;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(inherit = "stock.stock_in_details")
public class StockInDetails extends Model {
    static Field stock_in_id = Field.Many2one("stock.stock_in").label("入库单号");
    static Field should_stock_qty = Field.Float().label("应入库数量").compute("computeShouldStock");
    static Field stock_qty = Field.Float().label("已入库数量").defaultValue(0);
    static Field wait_stock_qty = Field.Float().label("待入库数量").compute("computeWaitStock");
    static Field inventory_balance_id = Field.Many2one("wms.inventory_balance").label("盘点平账单");

    public double computeShouldStock(Records records) {
        return Utils.round(records.getDouble("qty") - records.getDouble("return_qty"));
    }

    public double computeWaitStock(Records records) {
        return Utils.round(records.getDouble("qty") - records.getDouble("return_qty") - records.getDouble("stock_qty"));
    }

    @OnSaved("status")
    public void onStatusSaved(Records records) {
        Set<String> stockInIds = new HashSet<>();
        for (Records record : records) {
            String status = record.getString("status");
            if ("to-stock".equals(status)) {
                stockInIds.add(record.getRec("stock_in_id").getId());
            }
        }
        if (!stockInIds.isEmpty()) {
            Records stockIn = records.getEnv().get("stock.stock_in", stockInIds)
                .filter(r -> "to-inspect".equals(r.getString("status")));
            stockIn.set("status", "new");
        }
    }

    public void stockInWithLabel(Records records, Records location) {
        Records detail = records.first();
        Records warehouse = detail.getRec("warehouse_id");
        Records material = detail.getRec("material_id");
        for (Records row : records) {
            if (!"to-stock".equals(row.get("status"))) {
                throw new ValidationException(records.l10n("状态为[%s]，不能入库", row.getSelection("status")));
            }
            if (!warehouse.equals(row.getRec("warehouse_id"))) {
                throw new ValidationException(records.l10n("不同仓库不能一起操作"));
            }
            if (!material.equals(row.getRec("material_id"))) {
                throw new ValidationException(records.l10n("不同物料不能一起操作"));
            }
        }
        Map<String, Double> materialQty = new HashMap<>();
        Set<String> stockInIds = new HashSet<>();
        for (Records row : records) {
            Records label = row.getRec("label_id");
            Records stockIn = row.getRec("stock_in_id");
            String stockInId = stockIn.getId();
            String stockInCode = stockIn.getString("code");
            if (label.any()) {
                label.set("status", "onhand");
                label.set("warehouse_id", warehouse.getId());
                label.set("location_id", location.getId());
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "stock.stock_in");
                log.put("related_id", stockInId);
                log.put("related_code", stockInCode);
                label.call("logStatus", log);
                createOnhand(row, material, warehouse, location, label.getDouble("qty"), label.getId(), label.getString("lot_num"), label.getString("sn"));
            } else {
                // 成品标签
                Records mdPackage = records.getEnv().get("packing.package").find(Criteria.equal("code", row.getString("sn")));
                if (!mdPackage.any()) {
                    // 正常不会进入这里, 加个报错,看会不会有异常情况
                    throw new ValidationException("包装标签无法识别,请检查标签数据");
                }
                mdPackage.set("state", "stock-in");
                createOnhand(row, material, warehouse, location, mdPackage.getDouble("package_qty"), null, null, row.getString("sn"));
            }
            row.set("status", "done");
            row.set("stock_qty", row.get("qty"));
            stockInIds.add(stockIn.getId());
            String materialId = row.getRec("material_id").getId();
            if (materialQty.containsKey(materialId)) {
                materialQty.put(materialId, Utils.round(materialQty.get(materialId) + row.getDouble("qty")));
            } else {
                materialQty.put(materialId, row.getDouble("qty"));
            }
        }
        records.set("location_id", location.getId());
        Records stockIn = records.getEnv().get("stock.stock_in", stockInIds);
        stockIn.call("updateStockInStatus");
        // 回写采购订单入库数
        stockIn.call("updatePoStockIn", materialQty);
    }

    public void stockInWithQty(Records records, double qty, Records location) {
        Records detail = records.first();
        Records material = detail.getRec("material_id");
        Records warehouse = detail.getRec("warehouse_id");
        String lotNum = detail.getString("lot_num");
        for (Records row : records) {
            if (!"to-stock".equals(row.get("status"))) {
                throw new ValidationException(records.l10n("状态为[%s]，不能入库", row.getSelection("status")));
            }
            if (!warehouse.equals(row.getRec("warehouse_id"))) {
                throw new ValidationException(records.l10n("不同仓库不能一起操作"));
            }
            if (!material.equals(row.getRec("material_id"))) {
                throw new ValidationException(records.l10n("不同物料不能一起操作"));
            }
            if (!Utils.equals(lotNum, row.getString("lot_num"))) {
                throw new ValidationException(records.l10n("不同物料批次不能一起操作"));
            }
        }
        // 可以明确,能走到这里的一定是相同的物料
        double toStockQty = Utils.round(records.stream().mapToDouble(l -> l.getDouble("qty") - l.getDouble("stock_qty") - l.getDouble("return_qty")).sum());
        if (Utils.large(qty, toStockQty)) {
            if (Utils.isNotEmpty(lotNum)) {
                throw new ValidationException(records.l10n("批次[%s]入库数量[%s]不能大于待入库数量[%s]", lotNum, qty, toStockQty));
            }
            throw new ValidationException(records.l10n("入库数量[%s]不能大于待入库数量[%s]", qty, toStockQty));
        }
        Set<String> stockInIds = new HashSet<>();
        double leftQty = qty;
        for (Records row : records) {
            double total = Utils.round(row.getDouble("qty") - row.getDouble("return_qty"));
            double stockQty = row.getDouble("stock_qty");
            if (Utils.largeOrEqual(leftQty, total - stockQty)) {
                row.set("stock_qty", total);
                row.set("status", "done");
                leftQty = Utils.round(leftQty - (total - stockQty));
            } else if (Utils.large(leftQty, 0)) {
                row.set("stock_qty", Utils.round(stockQty + leftQty));
            }
            stockInIds.add(row.getRec("stock_in_id").getId());
        }
        records.set("location_id", location.getId());
        String sql = "update stock_onhand set ok_qty=ok_qty+%s,usable_qty=usable_qty+%s,stock_in_time=%s where material_id=%s and warehouse_id=%s";
        List<Object> params = new ArrayList<>(Arrays.asList(qty, qty, new ServerDate(), material.getId(), warehouse.getId()));
        if (location.any()) {
            sql += " and location_id=%s";
            params.add(location.getId());
        } else {
            sql += " and location_id is null ";
        }
        if (Utils.isNotEmpty(lotNum)) {
            sql += " and lot_num=%s";
            params.add(lotNum);
        } else {
            sql += " and lot_num is null";
        }
        Cursor cr = records.getEnv().getCursor();
        cr.execute(sql, params);
        if (cr.getRowCount() <= 0) {
            createOnhand(records, material, warehouse, location, qty, null, lotNum, null);
        }
        Records stockIn = records.getEnv().get("stock.stock_in", stockInIds);
        stockIn.call("updateStockInStatus");
        // 回写采购订单入库数
        Map<String, Double> materialQty = new HashMap<>();
        materialQty.put(material.getId(), toStockQty);
        stockIn.call("updatePoStockIn", materialQty);
    }

    public void createOnhand(Records record, Records material, Records warehouse, Records location, double qty, String labelId, String lotNum, String sn) {
        Map<String, Object> data = new HashMap<>();
        data.put("material_id", material.getId());
        data.put("ok_qty", qty);
        data.put("usable_qty", qty);
        data.put("company_id", warehouse.getRec("company_id").getId());
        data.put("label_id", labelId);
        data.put("lot_num", lotNum);
        data.put("warehouse_id", warehouse.getId());
        data.put("location_id", location.getId());
        data.put("stock_in_time", new ServerDate());
        data.put("sn", sn);
        record.getEnv().get("stock.onhand").create(data);
    }
}
