package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;

/**
 * @author 梁荣振
 * 物料条码状态记录
 */
@Model.Meta(name = "lbl.material_label_status", label = "标签条码状态", order = "update_date desc,operation desc")
@Model.Service(remove = "@edit")
public class MaterialLabelStatus extends Model {
    static Field sn = Field.Char().label("序列号").index();
    static Field lot_num = Field.Char().label("批次号").index();
    static Field label_id = Field.Many2one("lbl.material_label").label("标签条码");
    static Field status = Field.Selection(Selection.related("lbl.material_label", "status")).label("状态");
    static Field operation = Field.Selection(new Options() {{
        put("wms.material_receipt", "收料");
        put("stock.stock_in", "入库");
        put("stock.onhand:location", "绑定库位");
        put("mfg.material_issue", "发料");
        put("wms.transfer_order", "仓库调拨");
        put("wms.transfer_order:receipt", "调拨入库");
        put("stock.stock_out", "出库");
        put("lbl.material_label:split", "拆减");
        put("lbl.material_label", "新拆");
        put("wms.return_supplier", "退供应商");
        put("wms.return_supplier:delete", "取消退料");
        put("mfg.work_station_material", "上料");
        put("mfg.work_station_material:unload", "下料");
        put("move.wip.production", "用毕");
        put("mfg.material_return", "生产退料");
        put("mfg.material_return:delete", "取消退料");
        put("mfg.product_storage_notice", "成品入库通知");
        put("mfg.product_storage_notice:delete", "取消入库");
        put("wms.pick_out", "挑选");
        put("wms.pick_out:delete", "挑选");
        put("wms.inventory_check:frozen", "盘点冻结");
        put("wms.inventory_check:first_scan", "初盘扫码");
        put("wms.inventory_check:second_scan", "复盘扫码");
        put("wms.inventory_check:scan_balance", "已扫平账");
        put("wms.inventory_check:non_balance", "库存平账");
        put("wms.sales_delivery", "销售发货");
        put("wms.sales_delivery:delete", "取消发货");
        put("wms.sales_return", "销售退货");
        put("wms.sales_return:delete", "取消退货");
        put("wms.other_stock_in", "其它入库扫码");
        put("wms.other_stock_in:delete", "取消其它入库");
        put("wms.other_stock_in:stock_in", "其它入库");
        put("wms.other_stock_out", "其它出库扫码");
        put("wms.other_stock_out:delete", "取消其它出库");
        put("wms.other_stock_out:stock_out", "其它出库");
        put("packing.package:split", "包装拆分");
        put("packing.package:merge", "包装合并");
        put("wms.initial_inventory:initial", "初始化标签");
        put("wms.initial_inventory:delete", "取消标签");
    }}).label("变更操作");
    static Field related_id = Field.Char().label("相关单据ID");
    static Field related_code = Field.Char().label("关联单号");
    static Field material_id = Field.Many2one("md.material").label("物料编码");
    static Field material_spec = Field.Char().label("物料规格").related("material_id.spec");
    static Field material_name = Field.Char().label("物料名称").related("material_id.name");
    static Field qty = Field.Float().label("数量");
    static Field company_id = Field.Many2one("res.company").label("组织").readonly();
    static Field location = Field.Char().label("位置");
    static Field quality_status = Field.Selection(Selection.related("lbl.material_label", "quality_status")).label("质量状态");
}
