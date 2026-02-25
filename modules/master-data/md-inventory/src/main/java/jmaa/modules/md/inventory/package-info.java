@Manifest(
    name = "md-inventory",
    label = "仓库基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Material.class,
        StockClass.class,
        StoreArea.class,
        StoreLocation.class,
        User.class,
        Warehouse.class,
    },
    demo = {
        "demo/md.warehouse.csv",
        "demo/md.stock_class.csv",
        "demo/md.store_area.csv",
        "demo/md.store_location.csv",
        "demo/md.material.csv",
    },
    data = {
        "views/menus.xml",
        "views/material.xml",
        "views/stock_class.xml",
        "views/user.xml",
        "views/warehouse.xml",
    },
    depends = {
        "md-enterprise",
    },
    application = false)
package jmaa.modules.md.inventory;

import jmaa.modules.md.inventory.models.*;
import org.jmaa.sdk.Manifest;
