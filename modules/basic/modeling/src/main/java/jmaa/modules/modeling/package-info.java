@Manifest(
    name = "modeling",
    label = "开发建模",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Diagram.class,
        DesignModel.class,
        DesignModelField.class,
        DiagramShape.class,
        IrModule.class
    },
    data = {
        "views/diagram.xml"
    },
    controllers = {
        CodeController.class
    })
package jmaa.modules.modeling;

import org.jmaa.sdk.Manifest;
import jmaa.modules.modeling.controllers.CodeController;
import jmaa.modules.modeling.models.*;
