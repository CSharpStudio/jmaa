package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Arrays;

/**
 * @author eric
 */
@Model.Meta(inherit = "sales.order_line")
public class SalesOrderLine extends Model {
    static Field delivery_qty = Field.Float().label("发货数量").required().defaultValue(0);
    static Field return_qty = Field.Float().label("退货数量").required().defaultValue(0);

    /**
     * 用于建单时输入发货数量
     */
    static Field commit_qty = Field.Float().label("发货数量").store(false);

    @OnSaved("sales_qty")
    public void updateUncommitQty(Records records) {
        Cursor cr = records.getEnv().getCursor();
        for (Records line : records) {
            Double qty = line.getDouble("sales_qty");
            String sql = "select sum(request_qty) from wms_sales_delivery_line where so_line_id=%s";
            cr.execute(sql, Arrays.asList(line.getId()));
            double dQty = Utils.toDouble(cr.fetchOne()[0]);
            if (Utils.large(dQty, qty)) {
                throw new ValidationException(records.l10n("销售订单[%s]行号[%s]物料[%s]总建单数量[%s]大于销售数量[%s]",
                    line.getRec("so_id").get("code"), line.get("line_no"), line.getRec("material_id").get("code"), dQty, qty));
            }
            line.set("uncommit_qty", qty - dQty);
        }
    }
}
