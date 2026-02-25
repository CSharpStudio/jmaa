@Manifest(
    name = "md-process-routing",
    label = "工艺路线基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Route.class,
        RouteVersion.class,
        RouteNode.class,
        RouteProduct.class,
        ProductRoute.class,
        ResourceRoute.class,
    },
    demo = {
        "demo/pr.route.csv",
        "demo/pr.route_version.csv",
        "demo/pr.route_node.csv",
        "demo/update/pr.route_node.csv",
    },
    data = {
        "views/menus.xml",
        "views/route.xml",
        "views/route_version.xml",
        "views/product_route.xml",
        "views/resource_route.xml",
    },
    depends = {
        "md-process"
    },
    application = false)
package jmaa.modules.md.process.routing;

import jmaa.modules.md.process.routing.models.*;
import org.jmaa.sdk.Manifest;
