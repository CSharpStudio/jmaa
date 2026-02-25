package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "wms.other_stock_out_line", label = "其它出库-物料明细", authModel = "wms.other_stock_out", inherit = "mixin.material")
public class OtherStockOutLine extends Model {
    static Field other_stock_out_id = Field.Many2one("wms.other_stock_out").label("其它出库单");
    static Field status = Field.Selection(new Options(){{
        put("new", "未发货");
        put("delivering", "发货中");
        put("delivered", "已备齐");
        put("done", "已完成");
    }}).label("行状态").readonly(true).defaultValue("new");
    static Field request_qty = Field.Float().label("需出库数量").required().min(0d).defaultValue(0d);
    static Field scan_qty = Field.Float().label("已扫码数量").defaultValue(0);

    @OnSaved("request_qty")
    public void onRequestQtySaved(Records records) {
        for (Records record : records) {
            Double requestQty = record.getDouble("request_qty");
            if (Utils.less(requestQty, 0)) {
                throw new ValidationException(record.l10n("物料[%s]需求数量必须大于0", record.getRec("material_id").get("code")));
            }
            Double scanQty = record.getDouble("scan_qty");
            if(Utils.less(requestQty, scanQty)){
                throw new ValidationException(record.l10n("物料[%s]需求数量不能小于已发数量[%s]", record.getRec("material_id").get("code"), scanQty));
            }
            if (Utils.largeOrEqual(scanQty, requestQty)) {
                record.set("status", "delivered");
            }
        }
    }

    @OnSaved("scan_qty")
    public void onScanQtySaved(Records records) {
        for (Records record : records) {
            Double scanQty = record.getDouble("scan_qty");
            Double requestQty = record.getDouble("request_qty");
            if (Utils.largeOrEqual(scanQty, requestQty)) {
                record.set("status", "delivered");
            } else if (Utils.large(scanQty, 0)) {
                record.set("status", "delivering");
            }
        }
    }
}
