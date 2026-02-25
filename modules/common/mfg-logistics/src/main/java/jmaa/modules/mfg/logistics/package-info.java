@Manifest(
    name = "mfg-logistics",
    label = "生产物流",
    category = "生产运营管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        MaterialIssue.class,
        MaterialIssueLine.class,
        MaterialReturn.class,
        MaterialReturnLine.class,
        MaterialReturnDetails.class,
        MaterialReturnDialog.class,
        MaterialReturnMobile.class,
        ProductStorageNotice.class,
        ProductStorageNoticeDetails.class,
        ProductStorageNoticeMobile.class,
        ProductStorageNoticeDialog.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/material_issue.xml",
        "views/material_return.xml",
        "views/material_return_mobile.xml",
        "views/product_storage_notice.xml",
        "views/product_storage_notice_mobile.xml",
    },
    depends = {
        "manufacturing",
        "md-packing",
        "stock"
    })
package jmaa.modules.mfg.logistics;

import jmaa.modules.mfg.logistics.models.*;
import org.jmaa.sdk.Manifest;
