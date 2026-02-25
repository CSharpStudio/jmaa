@Manifest(
    name = "md-label",
    label = "标签基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Material.class,
        MaterialCodeRule.class,
        MaterialLabelMixin.class,
        MaterialLabel.class,
        PrintRuleMaterialLabel.class,
        PrintRuleProductLabel.class,
        ProductLabel.class,
    },
    demo = {
        "demo/md.material.csv",
        "demo/code.matcher.csv",
    },
    data = {
        "data/code_coding.xml",
        "data/print_template.xml",
        "views/material.xml",
    },
    depends = {
        "md-packing",
    },
    application = false)
package jmaa.modules.md.label;

import jmaa.modules.md.label.models.*;
import org.jmaa.sdk.Manifest;
