@Manifest(
    name = "tenant",
    category = "基础模块",
    label = "租户管理",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        TenantDataSource.class,
        TenantInfo.class
    },
    data = {
        "views/menus.xml",
        "views/tenant_info.xml",
        "views/tenant_data_source.xml",
    })
package jmaa.modules.tenant;

import org.jmaa.sdk.Manifest;
import jmaa.modules.tenant.models.*;
