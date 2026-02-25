@Manifest(
    name = "manufacturing",
    label = "生产管理",
    category = "生产运营管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        ProductOrder.class,
        WorkOrder.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/work_order.xml",
        "views/product_order.xml",
    },
    depends = {
        "md-resource",
        "sales"
    })
package jmaa.modules.manufacturing;

import jmaa.modules.manufacturing.models.ProductOrder;
import jmaa.modules.manufacturing.models.WorkOrder;
import org.jmaa.sdk.Manifest;
