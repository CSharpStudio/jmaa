package jmaa.modules.purchase.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 继承code.auto_code，自动生成订单编码，
 * 继承mixin.company，通过company_id字段区分采购订单公司
 * 继承mixin.order_status，包含status字段
 *
 * @author eric
 */
@Model.Meta(name = "purchase.order", label = "采购订单", present = {"code"}, presentFormat = "{code}", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class PurchaseOrder extends Model {
    static Field code = Field.Char().label("采购订单号").unique();
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required().tracking();
    static Field type = Field.Selection(new Options() {{
        put("purchase", "标准采购");
        put("outsource", "委外");
    }}).label("订单类型").defaultValue("purchase").required();
    static Field order_date = Field.Date().label("订单日期").required().tracking();
    static Field remark = Field.Char().label("备注");
    static Field line_ids = Field.One2many("purchase.order_line", "po_id").label("订单明细");
    static Field is_return = Field.Boolean().label("是否退货");

    @OnSaved("status")
    public void onStatusCommit(Records records) {
        for (Records record : records) {
            if ("commit".equals(record.get("status"))) {
                Records lines = record.getRec("line_ids");
                if (!lines.any()) {
                    throw new ValidationException(records.l10n("采购订单[%s]没有订单明细", record.get("code")));
                }
            }
        }
    }

    public void updatePurchaseOrderStatus(Records records) {
        //执行sql前flush保存
        records.flush();
        Cursor cr = records.getEnv().getCursor();
        String sql = "select distinct status from purchase_order_line where po_id=%s";
        for (Records record : records) {
            cr.execute(sql, Arrays.asList(record.getId()));
            List<String> status = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
            boolean done = status.stream().allMatch(s -> "done".equals(s));
            if (done) {
                // 如果全部为完成状态，则更新为已完成状态
                record.set("status", "done");
            }
        }
    }
}
