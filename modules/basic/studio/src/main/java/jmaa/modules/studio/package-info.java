@Manifest(
    name = "studio",
    label = "低代码平台",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        IrUiMenu.class,
        IrModel.class,
        Studio.class,
    },
    data = {
        "views/ir_ui_menu.xml",
        "views/ir_model.xml",
        "views/studio.xml",
    })
package jmaa.modules.studio;

import org.jmaa.sdk.Manifest;
import jmaa.modules.studio.models.*;
