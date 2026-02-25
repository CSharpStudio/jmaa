package org.jmaa.base.utils;

import com.alibaba.fastjson.JSONObject;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.tools.DigestUtils;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.util.SecurityCode;

import java.util.Base64;
import java.util.Date;

public class Jwt {
    /**
     * 创建令牌。格式 payload.signature
     *
     * @param login
     * @param hours
     * @return
     */
    public static String create(String login, int hours) {
        Claims claims = new Claims();
        claims.setKey(login);
        claims.setExp(Utils.addHours(new Date(), hours));
        String json = JSONObject.toJSONString(claims);
        String payload = Base64.getEncoder().encodeToString(json.getBytes());
        String signature = DigestUtils.md5Hex(DigestUtils.DES.encode(json));
        return payload + "." + signature;
    }

    /**
     * 校验令牌
     *
     * @param jwt
     * @return
     */
    public static Claims validate(String jwt) {
        if (Utils.isEmpty(jwt)) {
            throw new IllegalArgumentException("jwt");
        }
        String[] parts = jwt.split("\\.");
        if (parts.length != 2) {
            throw new AccessException("无效的jwt", SecurityCode.TOKEN_ERROR);
        }
        String json = new String(Base64.getDecoder().decode(parts[0]));
        String signature = DigestUtils.md5Hex(DigestUtils.DES.encode(json));
        if (!Utils.equals(signature, parts[1])) {
            throw new AccessException("无效的jwt", SecurityCode.TOKEN_ERROR);
        }
        Claims claims = JSONObject.parseObject(json, Claims.class);
        Date exp = claims.getExp();
        if (exp.before(new Date())) {
            throw new AccessException("令牌过期", SecurityCode.TOKEN_EXPIRED);
        }
        return claims;
    }
}
