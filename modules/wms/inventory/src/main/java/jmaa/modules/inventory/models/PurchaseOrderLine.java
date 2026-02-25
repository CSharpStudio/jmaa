package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(inherit = "purchase.order_line")
public class PurchaseOrderLine extends Model {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("收货仓库").store(false).lookup("searchWarehouse");
    static Field status = Field.Selection(new Options() {{
        put("new", "新建");
        put("received", "已收货");
        put("done", "已完成");
        put("close", "关闭");
    }}).label("状态").defaultValue("new");

    /**
     * 带出当前用户有权限的仓库
     */
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    static Field uncommit_qty = Field.Float().label("可建单数");

    @OnSaved({"purchase_qty", "return_qty"})
    public void updateUncommitQty(Records records) {
        Cursor cr = records.getEnv().getCursor();
        for (Records line : records) {
            double qty = line.getDouble("purchase_qty");
            double returnQty = line.getDouble("return_qty");
            String sql = "select sum(request_qty) from wms_material_receipt_line where po_line_id=%s";
            cr.execute(sql, Arrays.asList(line.getId()));
            double dQty = Utils.round(Utils.toDouble(cr.fetchOne()[0]) - returnQty);
            if (Utils.large(dQty, qty)) {
                throw new ValidationException(records.l10n("采购单[%s]行号[%s]物料[%s]总建单数量[%s]大于采购数量[%s]",
                    line.getRec("po_id").get("code"), line.get("line_no"), line.getRec("material_id").get("code"), dQty, qty));
            }
            line.set("uncommit_qty", qty - dQty);
        }
    }
}
