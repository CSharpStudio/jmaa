@Manifest(
    name = "md-craft",
    label = "制程基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        CraftType.class,
        CraftProcess.class,
        CraftRoute.class,
        CraftRouteNode.class,
        WorkResource.class,
        Material.class,
        CraftRouteProduct.class,
        CraftRouteBom.class,
        EquipmentType.class,
        Equipment.class,
    },
    demo = {
        "demo/md.craft_type.csv",
        "demo/md.craft_process.csv",
        "demo/md.craft_route.csv",
        "demo/md.craft_route_node.csv",
        "demo/md.material.csv",
        "demo/md.equipment_type.csv",
    },
    data = {
        "views/menus.xml",
        "views/craft_type.xml",
        "views/craft_process.xml",
        "views/craft_route.xml",
        "views/craft_route_product.xml",
        "views/material.xml",
        "views/work_resource.xml",
        "views/equipment_type.xml",
    },
    depends = {
        "md-resource"
    },
    application = false)
package jmaa.modules.md.craft;

import jmaa.modules.md.craft.models.*;
import org.jmaa.sdk.Manifest;
