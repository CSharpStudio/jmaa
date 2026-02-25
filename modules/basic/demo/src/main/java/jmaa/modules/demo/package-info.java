@Manifest(
    name = "demo",
    category = "基础模块",
    label = "示例",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Many2One.class,
        One2Many.class,
        One2ManyDetails.class,
        Many2Many.class,
        FieldType.class,
        AttrAction.class,
        MobileDemo.class
    },
    demo = {
        "demo/demo.field_type.csv",
    },
    data = {
        "data/field_type_data.xml",
        "views/menus.xml",
        "views/field_type_views.xml",
        "views/attr_action.xml",
        "views/field_type_mobile.xml",
    }, application = false)
package jmaa.modules.demo;

import jmaa.modules.demo.models.actions.AttrAction;
import jmaa.modules.demo.models.fields.*;
import org.jmaa.sdk.Manifest;
