@Manifest(
    name = "stock",
    label = "库存数据",
    category = "仓库运营管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Onhand.class,
        StockIn.class,
        StockInDetails.class,
        StockOut.class,
        StockOutDetails.class,
    },
    depends = {
        "md-inventory",
        "md-label",
    },
    application = false)
package jmaa.modules.stock;

import jmaa.modules.stock.models.*;
import org.jmaa.sdk.Manifest;
