@Manifest(
    name = "md-packing",
    label = "包装基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        PackingRule.class,
        PackingLevel.class,
        MaterialPackingRule.class,
        ProductPackingRule.class,
        PrintRulePackage.class,
        Material.class,
        PackingCodeRule.class,
        PackingPackage.class,
    },
    demo = {
        "demo/md.packing_rule.csv",
        "demo/md.packing_level.csv",
        "demo/md.material_packing_rule.csv",
    },
    data = {
        "data/code_coding.xml",
        "data/print_template.xml",
        "views/menus.xml",
        "views/packing_rule.xml",
        "views/product_packing_rule.xml",
        "views/material.xml",
        "views/package.xml",
    },
    depends = {
        "md-enterprise",
        "print",
    },
    application = false)
package jmaa.modules.md.packing;

import jmaa.modules.md.packing.models.*;
import org.jmaa.sdk.Manifest;
