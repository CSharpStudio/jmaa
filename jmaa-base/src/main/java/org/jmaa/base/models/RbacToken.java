package org.jmaa.base.models;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jmaa.base.utils.Claims;
import org.jmaa.base.utils.Jwt;
import org.jmaa.sdk.*;
import org.jmaa.sdk.util.KvMap;

/**
 * 令牌，记录用户登录的令牌
 *
 * @author Eric Liang
 */
@Model.Meta(name = "rbac.token", label = "令牌", logAccess = BoolState.False, authModel = "rbac.user")

public class RbacToken extends Model {
    static Field user_id = Field.Many2one("rbac.user");
    static Field token = Field.Char();
    static Field date = Field.DateTime();
    static Field expiration = Field.DateTime();
    static Field platform = Field.Selection(new Options() {{
        put("pda", "PDA");
        put("pc", "PC");
    }});

    /**
     * 生成新token。uuid+yyyyMMddHHmmss+uid
     */
    public String newToken(Records rec, String uid) {
        String data = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + uid;
        byte[] bytes = data.getBytes();
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.allocate(bytes.length + 16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        bb.put(bytes);
        return Base64.getEncoder().encodeToString(bb.array());
    }

    /**
     * 根据令牌查询用户。
     *
     * @return 令牌不存在或者已过期，返回null
     */
    public String getUserId(Records rec, String token) {
        if (Utils.isEmpty(token)) {
            return null;
        }
        if (token.contains(".")) {
            try {
                return (String) rec.getEnv().get("rbac.jwt").call("validateJwt", token);
            } catch (Exception e) {
                return null;
            }
        } else {
            Records r = rec.find(Criteria.equal("token", token));
            if (r.any()) {
                Date now = new Date();
                Timestamp t = (Timestamp) r.get("expiration");
                if (t.before(now)) {
                    return null;
                }
                String uid = ((Records) r.get("user_id")).getId();
                if (t.before(Utils.addHours(now, 1))) {
                    r.set("expiration", Utils.addHours(now, 8));
                }
                return uid;
            }
        }
        return null;
    }

    /**
     * 更新令牌
     */
    public Claims updateToken(Records rec, Records user, String platform, int hours) {
        Claims claims = new Claims();
        String uid = user.getId();
        if (user.getBoolean("shared")) {
            String token = Jwt.create(user.getString("login"), hours);
            claims.setKey(token);
            claims.setExp(Utils.addHours(new Date(), hours));
        } else {
            Records r = rec.find(Criteria.equal("user_id", uid).and(Criteria.equal("platform", platform)));
            String token = newToken(rec, uid);
            Date now = new Date();
            // TODO token限制ip使用
            Date dt = Utils.addHours(now, hours);
            if (!r.any()) {
                r.create(new KvMap()
                    .set("user_id", uid)
                    .set("token", token)
                    .set("date", now)
                    .set("platform", platform)
                    .set("expiration", dt));
            } else {
                r.set("token", token);
                r.set("date", now);
                r.set("expiration", dt);
            }
            claims.setKey(token);
            claims.setExp(dt);
        }
        return claims;
    }

    /**
     * 删除令牌
     */
    public void removeToken(Records rec, String uid) {
        Records r = rec.find(Criteria.equal("user_id", uid));
        r.delete();
    }
}
