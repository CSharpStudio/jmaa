@Manifest(
    name = "md-product",
    label = "产品基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Defect.class,
        Bom.class,
        BomVersion.class,
        BomDetails.class,
        Material.class,
        ProductModel.class,
        ProductFamily.class,
    },
    demo = {
        "demo/md.defect.csv",
        "demo/md.bom.csv",
        "demo/md.bom_version.csv",
        "demo/md.bom_details.csv",
    },
    data = {
        "views/bom.xml",
        "views/defect.xml",
        "views/material.xml",
        "views/menus.xml",
        "views/product_model.xml",
        "views/product_family.xml",
    },
    depends = {
        "md-enterprise"
    },
    application = false)
package jmaa.modules.md.product;

import jmaa.modules.md.product.models.*;
import org.jmaa.sdk.Manifest;
