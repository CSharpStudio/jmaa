package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(inherit = "stock.stock_out")
public class StockOut extends Model {
    public void stockOut(Records records) {
        Environment env = records.getEnv();
        Cursor cr = env.getCursor();
        records.flush();
        Records details = env.get("stock.stock_out_details").find(Criteria.in("stock_out_id", records.getIds()).and("status", "=", "new"));
        for (Records detail : details) {
            Records material = detail.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            String category = material.getString("category");
            if ("sn".equals(stockRule) || "finished".equals(category)|| "semi-finished".equals(category)) {
                Records label = detail.getRec("label_id");
                String labelSn = detail.getString("sn");
                if (label.any()) {
                    label.set("status", "stock-out");
                    label.set("warehouse_id", null);
                    label.set("location_id", null);
                    cr.execute("delete from stock_onhand where label_id=%s", Arrays.asList(label.getId()));
                    Records stock = detail.getRec("stock_out_id");
                    Map<String, Object> log = new HashMap<>();
                    log.put("operation", "stock.stock_out");
                    log.put("related_id", stock.getId());
                    log.put("related_code", stock.get("code"));
                    label.call("logStatus", log);
                } else {
                    // 可能是包装标签,
                    Records mdPackage = env.get("packing.package").find(Criteria.equal("code", labelSn));
                    if (mdPackage.any()) {
                        mdPackage.set("state", "stock-out");
                        cr.execute("delete from stock_onhand where sn=%s", Arrays.asList(mdPackage.getString("code")));
                    } else {
                        // 先报错,不然走到这里就停止,后续查问题都不好查, 可能包装标签还会有lot_num 类型的吧  todo
                        throw new ValidationException(records.l10n("标签无法识别"));
                    }
                }
            } else {
                // 处理完以后要删除
                Records stockOnhand = records.getEnv().get("stock.onhand");
                Records warehouse = detail.getRec("warehouse_id");
                Records location = detail.getRec("location_id");
                double qty = detail.getDouble("qty");
                Criteria criteria = null;
                if ("lot".equals(stockRule)) {
                    String lotNum = detail.getString("lot_num");
                    // 扣减库存数量
                    criteria = Criteria.equal("lot_num", lotNum)
                        .and(Criteria.equal("material_id", material.getId()))
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", location.getId()));
                } else {
                    criteria = Criteria.equal("material_id", material.getId())
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", location.getId()))
                        .and(Criteria.equal("lot_num", null))
                        .and(Criteria.equal("label_id", null));
                }
                // 数量管控的数据, 只有一个物料可以查,
                stockOnhand = stockOnhand.find(criteria);
                // 这种就只能有一条,
                stockOnhand.set("ok_qty", Utils.round(stockOnhand.getDouble("ok_qty") - qty));
                stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") - qty));
                if (Utils.equals(stockOnhand.getDouble("usable_qty"), 0d) && Utils.equals(stockOnhand.getDouble("ok_qty"), 0d) && Utils.equals(stockOnhand.getDouble("allot_qty"), 0d)) {
                    stockOnhand.delete();
                }
            }
        }
        details.set("status", "done");
        records.set("status", "done");
    }
}
