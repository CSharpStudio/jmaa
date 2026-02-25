package jmaa.modules.weixin.mp.models;

import jmaa.modules.weixin.mp.utils.WeiXinUtils;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.jmaa.sdk.AbstractModel;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

import me.chanjar.weixin.mp.api.WxMpService;

import java.util.UUID;

@Model.Meta(name = "weixin.mp")
public class WeiXinMp extends AbstractModel {
    public String generateQrCode(Records records) {
        // 生成唯一临时标识（用于关联用户和二维码）
        String tempId = UUID.randomUUID().toString().replace("-", "");
        WxMpService wxMpService = WeiXinUtils.getWxMpService(() -> {
            Records config = records.getEnv().getConfig();
            String appId = config.getString("app_id");
            String appSecret = config.getString("app_secret");
            String token = config.getString("token");
            String aesKey = config.getString("aes_key");
            return WeiXinUtils.createWxMpService(appId, appSecret, token, aesKey);
        });
        try {
            // 二维码参数：scene_id=tempId
            WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket(
                tempId, // 场景值（自定义，关联临时标识）
                1800    // 有效期（秒），最大1800
            );
            // 获取二维码图片URL（前端可直接展示）
            String qrCodeUrl = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
            //TODO 缓存临时标识，状态为"未登录"，有效期30分钟
            // 返回：临时标识 + 二维码URL
            return records.getEnv().getRegistry().getTenant().getName() + "|" + tempId + "|" + qrCodeUrl;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String login(String tempId) {
        //TODO 根据缓存临时标识验证登录
        return "";
    }
}
