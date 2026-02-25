@Manifest(
    name = "bbs",
    category = "基础模块",
    label = "讨论",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Message.class,
        Notification.class,
        TrackingValue.class,
        Thread.class,
        Follower.class,
        FollowerInvite.class,
        ChannelUser.class,
        Channel.class,
        MessageFeed.class,
        Template.class,
    },
    demo = {
    },
    data = {
        "data/message.xml",
        "views/menus.xml",
        "views/notify.xml",
        "views/message.xml",
        "views/template.xml",
    },
    controllers = {
        BbsController.class
    })
package jmaa.modules.bbs;

import org.jmaa.sdk.Manifest;
import jmaa.modules.bbs.controllers.BbsController;
import jmaa.modules.bbs.models.*;
import jmaa.modules.bbs.models.Thread;
