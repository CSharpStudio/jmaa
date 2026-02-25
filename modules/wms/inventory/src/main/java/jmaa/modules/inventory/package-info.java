@Manifest(
    name = "inventory",
    label = "库存管理",
    category = "仓库运营管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        ResConfig.class,
        Warehouse.class,
        //入库
        StockIn.class,
        StockInDetails.class,
        StockInDialog.class,
        //出入库
        StockOut.class,
        StockOutDetails.class,
        Onhand.class,
        //采购
        PurchaseOrderLine.class,
        //销售
        SalesOrder.class,
        SalesOrderLine.class,
        SalesOrderReport.class,
        //来料
        MaterialReceipt.class,
        MaterialReceiptLine.class,
        MaterialReceiptDetails.class,
        MaterialReceiptMobile.class,
        MaterialReceiptDialog.class,
        MaterialReceiptPallet.class,
        IqcSheet.class,
        IqcDetails.class,
        MaterialLabelStatusMobile.class,
        PalletPrintDialog.class,
        //来料入库
        MaterialStockInMobile.class,
        //发料
        MaterialIssue.class,
        MaterialIssueLine.class,
        MaterialIssueDetails.class,
        MaterialIssueDialog.class,
        MaterialIssueReport.class,
        MaterialIssueMobile.class,
        //退供应商
        ReturnSupplier.class,
        ReturnSupplierDetails.class,
        ReturnSupplierLine.class,
        ReturnSupplierDialog.class,
        ReturnSupplierReport.class,
        ReturnSupplierMobile.class,
        //销售发货
        SalesDelivery.class,
        SalesDeliveryLine.class,
        SalesDeliveryDetails.class,
        SalesDeliveryDialog.class,
        SalesDeliveryMobile.class,
        OqcSheet.class,
        OqcDetails.class,
        //销售退货
        SalesReturn.class,
        SalesReturnLine.class,
        SalesReturnDetails.class,
        SalesReturnReport.class,
        SalesReturnDialog.class,
        SalesReturnMobile.class,
        //库存报表
        OnhandReportBase.class,
        MaterialOnhandReport.class,
        WarehouseOnhandReport.class,
        OnhandReport.class,
        //成品打印模板
        MaterialLabelStatusMobile.class,
        // 挑选
        PickOut.class,
        PickOutDetails.class,
        PickOutMobile.class,
        Mrb.class,
        // 库位
        LocationMove.class,
        LocationMoveMobile.class,
        // 调拨
        TransferOrder.class,
        TransferOrderDetails.class,
        TransferOrderLine.class,
        TransferOrderDialog.class,
        TransferOrderMobile.class,
        TransferOrderReport.class,
        TransferOrderPrintRule.class,
        //生产退料
        MaterialReturn.class,
        MaterialReturnLine.class,
        MaterialReturnDetails.class,
        MaterialReturnDialog.class,
        MaterialReturnMobile.class,
        //成品入库
        ProductLabel.class,
        // 仓库盘点
        InventoryCheck.class,
        InventoryCheckLine.class,
        InventoryCheckDetails.class,
        InventoryCheckFirstMobile.class,
        InventoryCheckSecondMobile.class,
        InventoryBalance.class,
        // 其它入库
        OtherStockIn.class,
        OtherStockInLine.class,
        OtherStockInDetails.class,
        OtherStockInDialog.class,
        OtherStockInMobile.class,
        // 其它出库
        OtherStockOut.class,
        OtherStockOutLine.class,
        OtherStockOutDetails.class,
        OtherStockOutDialog.class,
        OtherStockOutMobile.class,
        MaterialSplitMobile.class,
        InitialInventory.class,
        InitialInventoryMobile.class,
        LotStatus.class,
        // 报表
        StockInReport.class,
        StockInDetailsReport.class,
        // 条码解析
        SupplierMaterial.class,
        CodeParse.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/res_config.xml",
        "views/warehouse.xml",
        "views/sales_order.xml",//销售订单
        "views/sales_order_report.xml",//销售出库查询
        "views/material_receipt.xml",//来料接收
        "views/material_receipt_mobile.xml",
        "views/material_issue.xml",//发料
        "views/material_issue_report.xml",//发料明细查询
        "views/material_issue_mobile.xml",//发料
        "views/return_supplier.xml",//退供应商
        "views/return_supplier_mobile.xml",//退供应商
        "views/return_supplier_report.xml",//退供应商明细
        "views/sales_delivery.xml",//销售发货
        "views/sales_delivery_mobile.xml",//销售发货
        "views/sales_return.xml",//销售退货
        "views/sales_return_mobile.xml",//销售退货
        "views/sales_return_report.xml",//销售退货明细
        "views/stock_in.xml",//入库
        "views/stock_out.xml",//出库
        "views/report/material_onhand_report.xml",//物料库存查询
        "views/report/warehouse_onhand_report.xml",//仓库库存查询
        "views/report/onhand_report.xml",//库存明细查询
        "views/iqc_sheet.xml",
        "views/iqc_sheet_mobile.xml",
        "views/oqc_sheet.xml",
        "views/material_stock_in_mobile.xml",//来料入库移动端
        "views/material_label_status_mobile.xml",
        "views/pick_out.xml",
        "views/pick_out_mobile.xml",
        "views/location_move_mobile.xml", // 库存
        "views/transfer_order.xml", //调拨
        "views/transfer_order_mobile.xml",
        "views/transfer_order_report.xml",
        // 仓库盘点
        "views/inventory_check.xml",
        "views/inventory_check_first_mobile.xml",
        "views/inventory_check_second_mobile.xml",
        "views/inventory_balance.xml",
        // 其它入库
        "views/other_stock_in.xml",
        "views/other_stock_in_mobile.xml",
        // 其它出库
        "views/other_stock_out.xml",
        "views/other_stock_out_mobile.xml",
        "views/material_split_mobile.xml",
        "views/initial_inventory.xml",
        "views/initial_inventory_mobile.xml",
        // 报表
        "views/report/stock_in_report.xml",
        "views/supplier_material.xml",
    },
    depends = {
        "stock",
        "label-manager",
        "mfg-logistics",
        "wms-qc",
        "purchase",
    },
    scripts = {
        "sql/onhand_report.xml"
    })
package jmaa.modules.inventory;

import jmaa.modules.inventory.models.*;
import jmaa.modules.inventory.models.report.*;
import org.jmaa.sdk.Manifest;
