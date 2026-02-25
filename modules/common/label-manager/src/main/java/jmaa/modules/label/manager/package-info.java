@Manifest(
    name = "label-manager",
    label = "标签管理",
    category = "生产运营管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        MaterialLabel.class,
        MaterialLabelPrintDialog.class,
        LabelSplit.class,
        ProductLabel.class,
        PackingPackage.class,
        LotNum.class,
        CodeParse.class,
        PrintRuleBlankLabel.class,
        PrintTemplate.class,
        MaterialLabelStatus.class,
        LabelPrint.class,
        LotPackage.class,
    },
    data = {
        "views/menus.xml",
        "views/material_label.xml",
        "views/material_label_status.xml",
        "views/material_label_print_dialog.xml",
        "views/package.xml",
        "views/label_print.xml",
    },
    depends = {
        "md-label",
        "md-inventory",
        "print"
    })
package jmaa.modules.label.manager;

import jmaa.modules.label.manager.models.*;
import org.jmaa.sdk.Manifest;
