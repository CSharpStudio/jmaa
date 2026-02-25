@Manifest(
    name = "md-qsd",
    label = "质量标准定义",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        AcReMixin.class,
        SamplingPlan.class,
        SamplingProcess.class,
        AqlCustom.class,
        AqlTable.class,
        SampleSize.class,
        LevelLetterTable.class,
        Material.class,
        QualityClass.class,
        InspectItem.class,
        MaterialSpec.class,
        MaterialSpecItem.class,
        QualityClassSpec.class,
        QualityClassSpecItem.class,
        MrbInspectItem.class,
    },
    demo = {
        "demo/qsd.sampling_process.csv",
        "demo/qsd.quality_class.csv",
        "demo/md.material.csv",
    },
    data = {
        "data/level_letter_table.xml",
        "data/GB2828.1-2012.xml",
        "views/menus.xml",
        "views/sampling_plan.xml",
        "views/sampling_process.xml",
        "views/quality_class.xml",
        "views/material.xml",
        "views/quality_class_spec.xml",
        "views/material_spec.xml",
    },
    depends = {
        "md-enterprise"
    },
    application = false)
package jmaa.modules.md.qsd;

import jmaa.modules.md.qsd.models.*;
import org.jmaa.sdk.Manifest;
