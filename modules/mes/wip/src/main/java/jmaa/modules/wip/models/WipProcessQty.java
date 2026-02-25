package jmaa.modules.wip.models;

import org.jmaa.sdk.BoolState;
import org.jmaa.sdk.Callable;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "wip.process_qty", label = "过站数量", logAccess = BoolState.False)
@Model.Service(remove = "@edit")
public class WipProcessQty extends Model {
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");
    static Field process_id = Field.Many2one("md.work_process").label("工序");
    static Field qty = Field.Float().label("过站数量");
    static Field ok_qty = Field.Float().label("合格数量");
    static Field ng_qty = Field.Float().label("不合格数量").compute(Callable.script("r->r.get('qty') - r.get('ok_qty')"));
    static Field left_qty = Field.Float().label("待过站数量");
}
