@Manifest(
    name = "account",
    label = "会计",
    category = "ERP",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        PurchaseOrderLine.class,
        PurchasePrice.class,
        PurchasePriceDetails.class,
    },
    data = {
        "views/menus.xml",
        "views/purchase_order.xml",
        "views/purchase_price.xml"
    },
    depends = {
        "md-enterprise",
        "md-account",
        "purchase",
        "sales",
    })
package jmaa.modules.account;

import jmaa.modules.account.models.*;
import org.jmaa.sdk.Manifest;
