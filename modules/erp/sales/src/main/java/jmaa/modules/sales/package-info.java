@Manifest(
    name = "sales",
    label = "销售管理",
    category = "ERP",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        SalesOrder.class,
        SalesOrderLine.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/sales_order.xml",
    },
    depends = {
        "md-enterprise",
    })
package jmaa.modules.sales;

import jmaa.modules.sales.models.*;
import org.jmaa.sdk.Manifest;
