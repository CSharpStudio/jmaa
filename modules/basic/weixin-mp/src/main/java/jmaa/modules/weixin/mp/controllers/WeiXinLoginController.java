package jmaa.modules.weixin.mp.controllers;

import jmaa.modules.weixin.mp.utils.WeiXinUtils;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.RequestHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class WeiXinLoginController extends Controller {
    @RequestMapping(value = "/*/wxmp/qrcode", method = RequestMethod.GET)
    @RequestHandler(auth = RequestHandler.AuthType.NONE, type = RequestHandler.HandlerType.HTTP)
    public String generateQrCode() {
        Environment env = getEnv();
        return (String) env.get("weixin.mp").call("generateQrCode");
    }

    @RequestMapping(value = "/*/wxmp/login", method = RequestMethod.GET)
    public String checkLogin(@RequestParam String tempId) {
        Environment env = getEnv();
        return (String) env.get("weixin.mp").call("login", tempId);
    }

    /**
     * 微信服务器回调接口（接收关注/扫码事件）
     */
    @RequestMapping(value = "/wxmp/callback", method = RequestMethod.POST)
    public String wxCallback(HttpServletRequest request) {
        try {
            // 解析微信推送的XML消息
            WxMpXmlMessage wxMessage = WxMpXmlMessage.fromXml(request.getInputStream());
            // 处理消息（关注事件、扫码事件）
            //WxMpXmlOutMessage outMessage = messageRouter.route(wxMessage);
            //return outMessage == null ? "success" : outMessage.toXml();
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "success"; // 微信要求回调必须返回success，否则会重复推送
        }
    }

    /**
     * 验证微信回调接口（公众号配置时的验证）
     */
    @RequestMapping(value = "/wxmp/callback", method = RequestMethod.GET)
    public String validateCallback(
        @RequestParam String signature,
        @RequestParam String timestamp,
        @RequestParam String nonce,
        @RequestParam String echostr
    ) {
        WxMpService wxMpService = WeiXinUtils.getWxMpService();
        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr; // 验证通过，返回随机字符串
        }
        return "error";
    }
}
