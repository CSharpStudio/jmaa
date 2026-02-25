@Manifest(
    name = "md-enterprise",
    label = "企业基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        EnterpriseModel.class,
        EnterpriseTemplate.class,
        Factory.class,
        Workshop.class,
        MaterialType.class,
        Material.class,
        MaterialMixin.class,
        Unit.class,
        UnitConversion.class,
        Staff.class,
        Supplier.class,
        Customer.class,
        OrderStatusMixin.class,
    },
    demo = {
        "demo/md.customer.csv",
        "demo/md.supplier.csv",
        "demo/md.material_type.csv",
        "demo/md.material.csv",
        "demo/md.enterprise_model.csv",
    },
    data = {
        "data/enterprise_template.xml",
        "data/unit.xml",
        "views/menus.xml",
        "views/enterprise_template.xml",
        "views/enterprise_model.xml",
        "views/material_type.xml",
        "views/material.xml",
        "views/unit.xml",
        "views/supplier.xml",
        "views/customer.xml",
        "views/staff.xml",
    },
    depends = {
        "code-rule",
        "bbs"
    },
    application = false)
package jmaa.modules.md.enterprise;

import org.jmaa.sdk.Manifest;
import jmaa.modules.md.enterprise.models.*;
