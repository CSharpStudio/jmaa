@Manifest(
    name = "weixin-mp",
    category = "基础模块",
    label = "微信公众号",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        WeiXinMp.class,
        ResConfig.class,
    },
    controllers = {
        WeiXinLoginController.class,
    },
    data = {
        "views/login.xml",
    })
package jmaa.modules.weixin.mp;

import jmaa.modules.weixin.mp.controllers.WeiXinLoginController;
import jmaa.modules.weixin.mp.models.ResConfig;
import jmaa.modules.weixin.mp.models.WeiXinMp;
import org.jmaa.sdk.Manifest;
