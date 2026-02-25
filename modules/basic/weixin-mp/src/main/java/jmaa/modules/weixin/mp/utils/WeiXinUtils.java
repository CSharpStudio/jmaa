package jmaa.modules.weixin.mp.utils;

import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.mp.constant.WxMpEventConstants;
import org.jmaa.sdk.tools.SpringUtils;

import java.util.function.Supplier;

public class WeiXinUtils {
    static WxMpService service;

    public static WxMpService getWxMpService(Supplier<WxMpService> creator) {
        if (service == null) {
            service = creator.get();
        }
        return service;
    }

    public static WxMpService getWxMpService() {
        return service;
    }

    public static WxMpService createWxMpService(String appId, String appSecret, String token, String aesKey) {
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(appId);
        config.setSecret(appSecret);
        config.setToken(token);
        config.setAesKey(aesKey);

        WxMpService wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(config);

        WxMpMessageRouter router = new WxMpMessageRouter(wxMpService);

        router.rule()
            .async(false)
            .msgType("event")
            .event("subscribe") // 关注事件
            .event("SCAN") // 已关注用户扫码事件
            .handler((wxMessage, context, service, sessionManager) -> {
                // 获取场景值（即生成二维码时的tempId）
                String tempId = wxMessage.getEventKey();
                // 去掉前缀（微信推送的eventKey可能带"qrscene_"）
                if (tempId.startsWith("qrscene_")) {
                    tempId = tempId.substring(8);
                }
                // 获取用户OpenID
                String openId = wxMessage.getFromUser();
                // 处理登录逻辑
                //wxLoginService.handleSubscribeEvent(tempId, openId);

                return null; // 无需回复消息
            })
            .end();
        SpringUtils.registerBean(router);
        return wxMpService;
    }
}
