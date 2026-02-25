package jmaa.modules.md.packing.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Options;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "packing.package", label = "包装", inherit = {"mixin.material"})
@Model.Service(remove = "@edit")
public class PackingPackage extends Model {

    static Field code = Field.Char().label("标签条码").unique().required();
    static Field upper_code = Field.Char().label("上层条码").index();
    static Field top_code = Field.Char().label("顶层条码").index();
    static Field qty = Field.Float().label("数量").help("已打包主单位总数量");
    static Field package_qty = Field.Float().label("包装数量");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field parent_id = Field.Many2one("packing.package").label("外层包装");
    static Field child_ids = Field.One2many("packing.package", "package_id").label("内层包装");
    static Field state = Field.Selection(new Options() {{
        put("normal", "正常");
        put("scrap", "报废");
        put("stock-out", "出库");
        put("stock-in", "在库");
    }}).label("状态").defaultValue("normal");
    static Field related_code = Field.Char().label("相关单据");
    static Field related_model = Field.Selection(new Options(){{
        put("mfg.work_order", "工单");
        put("wms.asn", "ASN单");
        put("wms.material_receipt", "收料单");
        put("wms.suppler_pallet", "码盘");
    }}).label("单据类型");
    static Field related_id = Field.Many2oneReference("related_model").label("相关单据");
    static Field packing_level = Field.Selection(Selection.related("md.packing_level", "packing_level")).related("packing_level_id.packing_level");
    static Field packing_level_id = Field.Many2one("md.packing_level").label("包装层级");
}
