@Manifest(
    name = "md-process",
    label = "工艺基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Bom.class,
        BomProcess.class,
        WorkProcess.class,
        WorkProcessStep.class,
        WorkStation.class,
        WorkStep.class,
        Defect.class,
        ProductFamily.class,
    },
    demo = {
        "demo/md.product_family.csv",
        "demo/md.work_process.csv",
        "demo/md.work_process_step.csv",
        "demo/md.work_station.csv",
    },
    data = {
        "views/menus.xml",
        "views/bom.xml",
        "views/work_process.xml",
        "views/work_station.xml",
        "views/product_family.xml",
        "views/defect.xml",
    },
    depends = {
        "md-product",
        "md-resource"
    },
    application = false)
package jmaa.modules.md.process;

import jmaa.modules.md.process.models.*;
import org.jmaa.sdk.Manifest;
