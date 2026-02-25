@Manifest(
    name = "md-resource",
    label = "生产资源基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        EquipmentType.class,
        EquipmentModel.class,
        Equipment.class,
        WorkResource.class,
    },
    demo = {
        "demo/md.equipment_type.csv",
        "demo/md.equipment_model.csv",
        "demo/md.equipment.csv",
        "demo/md.work_resource.csv",
    },
    data = {
        "views/menus.xml",
        "views/equipment_type.xml",
        "views/equipment_model.xml",
        "views/equipment.xml",
        "views/work_resource.xml",
    },
    depends = {
        "md-enterprise",
    },
    application = false)
package jmaa.modules.md.resource;

import jmaa.modules.md.resource.models.*;
import org.jmaa.sdk.Manifest;
