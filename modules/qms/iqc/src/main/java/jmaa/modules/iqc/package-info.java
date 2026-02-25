@Manifest(
    name = "iqc",
    label = "来料质量控制",
    category = "质量运营管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        IqcInspectItem.class,
        IqcSheet.class,
        QualityClassSpec.class,
        IqcSheetMobile.class,
        IqcInspectItemMobile.class,
        Mrb.class,
        ResConfig.class,
    },
    demo = {
        "demo/qsd.quality_class_spec.csv",
        "demo/qsd.quality_class_spec_item.csv",
    },
    data = {
        "views/iqc_sheet.xml",
        "views/iqc_sheet_mobile.xml",
        "views/mrb.xml",
        "views/res_config.xml",
    },
    depends = {
        "wms-qc",
        "md-qsd"
    })
package jmaa.modules.iqc;

import jmaa.modules.iqc.models.*;
import org.jmaa.sdk.Manifest;
