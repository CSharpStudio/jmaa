package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(inherit = "res.config")
public class ResConfig extends ValueModel {
    static Field stock_in_timeout = Field.Integer().label("入库时效管控(小时)");
    static Field abc_exceed = Field.Selection(new Options() {{
        put("A", "A类");
        put("B", "B类");
        put("C", "C类");
    }}).label("ABC分类超发管控").help("勾选表示允许超发");
    static Field fifo_date = Field.Selection(new Options() {{
        put("product_date", "生产日期");
        put("stock_in_time", "入库日期");
    }}).label("先进先出管控日期").defaultValue("product_date").required();
    /*static Field replay = Field.Boolean().label("是否复盘").defaultValue(false);*/
    static Field lot_in_qty = Field.Boolean().label("批次入库数量").defaultValue(false).help("开启为控制批次管控物料入库数量不允许修改");
    static Field lot_out_qty = Field.Boolean().label("批次出库数量").defaultValue(false).help("开启为控制批次管控物料出库数量不允许修改");
    static Field receipt_auto_inspect = Field.Boolean().label("收料自动报检").defaultValue(false).help("开启为,收货单行收满后自动报检;不开启,则全部行收满后自动报检");
    static Field pick_out_auto_audit = Field.Boolean().label("挑选后自动审核").defaultValue(true).help("MBR挑选后自动审核");
}
