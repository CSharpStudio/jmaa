@Manifest(
    name = "wms-qc",
    label = "仓库质量",
    category = "质量运营管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        IqcExemptionList.class,
        IqcSheet.class,
        Mrb.class,
        OqcSheet.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/iqc_exemption_list.xml",
        "views/iqc_sheet.xml",
        "views/iqc_sheet_mobile.xml",
        "views/oqc_sheet.xml",
        "views/mrb.xml",
    },
    depends = {
        "md-enterprise",
    })
package jmaa.modules.wms.qc;

import jmaa.modules.wms.qc.models.*;
import org.jmaa.sdk.Manifest;
