@Manifest(
    name = "api-doc",
    label = "API文档",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    controllers = {
        ApiController.class
    },
    data = {
        "views/menus.xml"
    },
    autoInstall = true)
package jmaa.modules.api.doc;

import org.jmaa.sdk.Manifest;
import jmaa.modules.api.doc.controllers.ApiController;
