package jmaa.modules.weixin.mp.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(inherit = "res.config")
public class ResConfig extends Model {
    static Field app_id = Field.Char().label("App Id");
    static Field app_secret = Field.Char().label("App Secret");
    static Field token = Field.Char().label("令牌");
    static Field aes_key = Field.Char().label("AES密钥");
}
