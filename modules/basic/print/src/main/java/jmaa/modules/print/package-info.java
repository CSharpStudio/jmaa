@Manifest(
    name = "print",
    label = "打印设置",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        PrintRule.class,
        PrintTemplate.class,
        BtwAdapter.class,
        ClientAdapter.class,
        StimulSoftAdapter.class,
    },
    data = {
        "views/menus.xml",
        "views/print_template.xml",
    },
    controllers = {
        PrintTemplateController.class,
        PrintClientController.class
    })
package jmaa.modules.print;

import jmaa.modules.print.controllers.*;
import jmaa.modules.print.models.*;
import org.jmaa.sdk.Manifest;
