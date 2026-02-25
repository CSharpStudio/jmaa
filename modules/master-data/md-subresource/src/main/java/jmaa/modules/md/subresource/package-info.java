@Manifest(
    name = "md-subresource",
    label = "辅助资源基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        SubResourceType.class,
        SubResourceModel.class,
        SubResource.class,
        SubResourceProduct.class,
        SubResourceSubProduct.class,
        SubWorkResource.class,
        WorkResource.class,
        WorkSubResource.class,
    },
    data = {
        "data/md.sub_resource_type.csv",
        "views/menus.xml",
        "views/sub_resource_type.xml",
        "views/sub_resource_model.xml",
        "views/sub_resource.xml",
        "views/work_resource.xml",
    },
    depends = {
        "md-resource",
    },
    application = false)
package jmaa.modules.md.subresource;

import jmaa.modules.md.subresource.models.*;
import org.jmaa.sdk.Manifest;
