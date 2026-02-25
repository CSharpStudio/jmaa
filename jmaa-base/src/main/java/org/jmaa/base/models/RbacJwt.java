package org.jmaa.base.models;

import org.jmaa.base.utils.Claims;
import org.jmaa.base.utils.Jwt;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.DigestUtils;
import org.jmaa.sdk.tools.ThrowableUtils;
import org.jmaa.sdk.util.SecurityCode;

import java.util.Collections;
import java.util.Date;

/**
 * JWT即JSON Web Token。
 * 令牌使用加密算法生成, 相同秘钥生成的不同令牌可以同时登录
 *
 * @author : Eric Liang
 **/
@Model.Meta(name = "rbac.jwt", inherit = "rbac.user", table = "rbac_user", label = "Web令牌", present = {"name", "login"}, presentFormat = "{name}({login})")
public class RbacJwt extends ValueModel {
    static Field mode = Field.Selection().label("类型").defaultValue("jwt");
    static Field secret = Field.Char().label("密钥").compute("computeSecret");

    /**
     * 计算密钥
     *
     * @param rec
     * @return
     */
    public String computeSecret(Records rec) {
        String login = rec.getString("login");
        return DigestUtils.DES.encode(login);
    }

    /**
     * 获取JWT
     *
     * @param rec
     * @param secret
     * @return
     */
    public String getJwt(Records rec, String login, String secret) {
        Criteria criteria = Criteria.equal("active", true)
            .and(Criteria.equal("login", login));
        Records user = rec.find(criteria, 0, 1, null);
        if (!user.any()) {
            throw new AccessException("令牌id无效", SecurityCode.TOKEN_ERROR);
        }
        if (!"jwt".equals(user.getString("mode"))) {
            Records security = rec.getEnv().get("rbac.security");
            security.call("checkCredentials", user, secret, Collections.emptyMap());
        } else if (!Utils.equals(login, DigestUtils.DES.decode(secret))) {
            throw new AccessException("令牌id或密钥无效", SecurityCode.TOKEN_ERROR);
        }
        return Jwt.create(login, 1);
    }

    @ServiceMethod(auth = Constants.ANONYMOUS)
    public String getAdminJwt(Records rec, String login, String password) {
        Criteria criteria = Criteria.equal("active", true).and(Criteria.equal("mode", "user"),
            Criteria.equal("login", login).or(Criteria.equal("mobile", login), Criteria.equal("email", login)));
        Records user = rec.find(criteria, 0, 1, null);
        if (!user.any()) {
            throw new ValidationException(rec.l10n("账号[%s]不存在", login));
        }
        Records security = rec.getEnv().get("rbac.security");
        try {
            security.call("checkCredentials", user, password, Collections.emptyMap());
        } catch (Exception e) {
            throw new ValidationException(ThrowableUtils.getCause(e).getMessage());
        }
        boolean isAdmin = (Boolean) security.call("isAdmin", user.getId());
        if (!isAdmin) {
            throw new ValidationException(rec.l10n("账号[%s]不是管理员", login));
        }
        return Jwt.create(user.getId(), 1);
    }

    /**
     * 验证JWT，返回令牌id。
     * JWT有效时长1小时，有效时长小于50分钟重新生成新的JWT.
     *
     * @param records
     * @param jwt
     * @return
     */
    public String validateJwt(Records records, String jwt) {
        Claims claims = Jwt.validate(jwt);
        String login = claims.getKey();
        Criteria criteria = Criteria.equal("active", true)
            .and(Criteria.equal("login", login));
        Records user = records.find(criteria, 0, 1, null);
        if (!user.any()) {
            throw new AccessException("令牌id无效", SecurityCode.TOKEN_ERROR);
        }
        if (claims.getExp().before(Utils.addMinutes(new Date(), 50))) {
            String newJwt = Jwt.create(login, 1);
            records.getEnv().getContext().put("token", newJwt);
        } else {
            records.getEnv().getContext().put("token", jwt);
        }
        return user.getId();
    }
}
