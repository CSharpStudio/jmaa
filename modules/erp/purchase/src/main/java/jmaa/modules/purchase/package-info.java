@Manifest(
    name = "purchase",
    label = "采购管理",
    category = "ERP",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        PurchaseOrder.class,
        PurchaseOrderLine.class,
        PurchaseOrderReport.class,
        SupplierMaterial.class,
        Material.class,
    },
    demo = {
        "demo/purchase.order.csv",
        "demo/purchase.order_line.csv",
    },
    depends = {
        "md-enterprise"
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/purchase_order.xml",
        "views/purchase_order_report.xml",
        "views/supplier_material.xml",
    })
package jmaa.modules.purchase;

import jmaa.modules.purchase.models.*;
import org.jmaa.sdk.Manifest;
