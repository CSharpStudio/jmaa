package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "wms.pick_out_details", label = "挑选明细", authModel = "wms.pick_out", inherit = {"mixin.material"})
public class PickOutDetails extends Model {
    static Field pick_out_id = Field.Many2one("wms.pick_out").label("挑选单号");
    static Field sn = Field.Char().label("序列号");
    static Field qty = Field.Float().label("标签数量");
    static Field material_code = Field.Char().related("material_id.code").label("物料编码");
    static Field material_stock_rule = Field.Selection().related("material_id.stock_rule").label("库存规则");
    static Field status = Field.Selection(new Options() {{
        put("new", "已扫描");
        put("done", "已完成");
    }}).label("状态").defaultValue("new");

    @Override
    public boolean delete(Records records) {
        for (Records rec : records) {
            Records material = rec.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records materialLabel = rec.getEnv().get("lbl.material_label").find(Criteria.equal("sn", rec.getString("sn")));
                // 走到挑选这里来的,都是收料过来, 不可能是其他状态,
                materialLabel.set("status", "received");
                Records pickOut = rec.getRec("pick_out_id");
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.pick_out:delete");
                log.put("related_id", pickOut.getId());
                log.put("related_code", pickOut.getString("code"));
                materialLabel.call("logStatus", log);
            } else if ("lot".equals(stockRule) && rec.getEnv().getConfig().getBoolean("lot_in_qty")) {
                // 删除批次号的时候,删除临时表数据
                rec.getEnv().get("lbl.lot_status").find(Criteria.equal("order_id", rec.getRec("pick_out_id").getId())
                    .and(Criteria.equal("type", "wms.pick_out")).and(Criteria.equal("material_id",material.getId()))
                    .and(Criteria.equal("lot_num", rec.getString("sn")))).delete();
            }
        }
        return (Boolean) records.callSuper(PickOutDetails.class, "delete");
    }
}
