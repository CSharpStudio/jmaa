@Manifest(
    name = "report",
    label = "查询报表",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        ReportXmlLoader.class,
        Report.class,
        DataSet.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/dataset.xml",
        "views/report.xml",
        "reports/demo.xml",
    },
    depends = {
        "code-rule"
    },
    controllers = {
        ReportController.class
    })
package jmaa.modules.report;

import org.jmaa.sdk.Manifest;
import jmaa.modules.report.controllers.ReportController;
import jmaa.modules.report.models.*;
