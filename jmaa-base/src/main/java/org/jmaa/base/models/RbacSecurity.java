package org.jmaa.base.models;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.SecurityCode;
import org.jmaa.sdk.util.ServerDate;

import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.springframework.util.DigestUtils;


/**
 * 安全工具类
 *
 * @author Eric Liang
 */
@Model.Meta(name = "rbac.security", label = "用户安全工具类", authModel = "rbac.user")

public class RbacSecurity extends AbstractModel {
    final static String READ = "read";

    /**
     * 登录失败，记录失败日志
     */
    public void loginFailed(Records rec, String uid, String login, Map<String, Object> userAgent) {
        rec.getEnv().get("rbac.user.log").create(
            new KvMap().set("ip", userAgent.get("HTTP_HOST"))
                .set("user_id", uid)
                .set("login", login)
                .set("result", "0")
                .set("log_time", new ServerDate())
                .set("user_agent", userAgent.get("USER-AGENT")));
    }

    /**
     * 登陆成功，记录成功日志
     */
    public void loginSuccess(Records rec, Records user, String login, Map<String, Object> userAgent) {
        rec.getEnv().get("rbac.user.log").create(
            new KvMap().set("ip", userAgent.get("HTTP_HOST"))
                .set("user_id", user.getId())
                .set("result", "1")
                .set("log_time", new ServerDate())
                .set("user_agent", userAgent.get("USER-AGENT")));
    }

    /**
     * 记录登出日志
     */
    public void logout(Records rec, Records user, Map<String, Object> userAgent) {
        rec.getEnv().get("rbac.user.log").create(
            new KvMap().set("ip", userAgent.get("HTTP_HOST"))
                .set("user_id", user.getId())
                .set("result", "2")
                .set("log_time", new ServerDate())
                .set("user_agent", userAgent.get("USER-AGENT")));
    }

    /**
     * 校验账号锁定
     */
    public void checkAccountLock(Records rec, Records user) {
        Records config = rec.getEnv().getConfig();
        //密码错误次数
        int errorPasswordCount = user.getInteger("error_password_count");
        //允许错误次数
        int loginTryTimes = config.getInteger("login_try_times");

        //如果密码错误次数超出设定的次数
        if (errorPasswordCount >= loginTryTimes) {
            //密码最后错误时间
            Timestamp errorPasswordTime = user.getDateTime("error_password_time");
            if (Utils.isEmpty(errorPasswordTime)) {
                errorPasswordTime = new Timestamp(System.currentTimeMillis());
            }
            //锁定时长(秒)
            Double loginLockTime = config.getDouble("login_lock_time");
            long lockTime = Utils.addSeconds(errorPasswordTime, loginLockTime.intValue()).getTime() - System.currentTimeMillis();
            if (lockTime > 0) {
                throw new AccessException(rec.l10n("密码错误%s次数,请在%s秒后再试", errorPasswordCount, lockTime / 1000, 0), SecurityCode.LOCKED);
            } else {
                Cursor cursor = rec.getEnv().getCursor();
                cursor.execute("UPDATE rbac_user SET error_password_count=0 WHERE id = %s", Arrays.asList(user.getId()));
                cursor.commit();
            }
        }
    }

    public boolean isAdmin(Records rec, String uid) {
        Cursor cr = rec.getEnv().getCursor();
        String sql = "SELECT count(1) FROM rbac_role r JOIN rbac_role_user ru on r.id=ru.role_id WHERE ru.user_id=%s AND r.is_admin=%s AND r.active=%s";
        cr.execute(sql, Arrays.asList(uid, true, true));
        long count = Utils.toLong(cr.fetchOne()[0]);
        return count > 0;
    }

    /**
     * 判断用户是否有指定模型的指定权限码
     */
    public boolean hasPermission(Records rec, String uid, String model, String auth) {
        // TODO load from cache
        Environment env = rec.getEnv();
        if (Utils.isEmpty(uid)) {
            return false;
        }
        Boolean isAdmin = (uid.equals(env.getUserId()) && env.isAdmin()) || isAdmin(rec, uid);
        if (isAdmin) {
            return true;
        }
        if (READ.equals(auth)) {
            model = getAuthModel(env.getRegistry(), model);
        }
        Cursor cr = env.getCursor();
        String sql = "SELECT count(1) FROM rbac_permission p JOIN rbac_role_permission rp on p.id=rp.permission_id JOIN rbac_role_user ru on rp.role_id=ru.role_id JOIN rbac_role r on r.id=ru.role_id WHERE ru.user_id=%s AND p.model=%s AND p.auth=%s AND r.active=%s";
        cr.execute(sql, Arrays.asList(uid, model, auth, true));
        long count = (Long) cr.fetchOne()[0];
        return count > 0;
    }

    String getAuthModel(Registry reg, String model) {
        String auth = reg.get(model).getAuthModel();
        if (StringUtils.isEmpty(auth) || Utils.equals(model, auth)) {
            return model;
        }
        return getAuthModel(reg, auth);
    }

    /**
     * 是否有模型的创建权限
     */
    @Model.ServiceMethod(auth = Constants.ANONYMOUS, label = "是否有模型创建权限")
    public boolean canCreate(Records rec, String model) {
        return hasPermission(rec, rec.getEnv().getUserId(), model, "create");
    }

    /**
     * 权限用户获取指定模型已分配的所有权限码
     */
    public List<String> getPermissions(Records rec, String uid, String model) {
        // TODO load from cache
        Environment env = rec.getEnv();
        Cursor cr = env.getCursor();
        Boolean isAdmin = (uid.equals(env.getUserId()) && env.isAdmin()) || isAdmin(rec, uid);
        if (isAdmin) {
            String sql = "SELECT p.auth FROM rbac_permission p WHERE p.model=%s AND p.active=%s";
            cr.execute(sql, Arrays.asList(model, true));
            return cr.fetchAll().stream().map(row -> (String) row[0]).collect(Collectors.toList());
        }
        String sql = "SELECT p.auth FROM rbac_permission p JOIN rbac_role_permission rp on p.id=rp.permission_id JOIN rbac_role_user ru on rp.role_id=ru.role_id JOIN rbac_role r on r.id=ru.role_id WHERE ru.user_id=%s' AND p.model=%s AND p.active=%s AND r.active=%s";
        cr.execute(sql, Arrays.asList(uid, model, true, true));
        return cr.fetchAll().stream().map(row -> (String) row[0]).collect(Collectors.toList());
    }

    /**
     * 校验密码安全规则
     */
    public void checkPasswordRule(Records rec, String password) {
        Records config = rec.getEnv().getConfig();
        boolean pwdComplexity = Utils.toBoolean(config.get("pwd_complexity"), false);
        int pwdMinLength = Utils.toInt(config.get("pwd_min_length"), 6);
        if (pwdMinLength != 0 && password.length() < pwdMinLength) {
            throw new ValidationException(rec.l10n("不符合安全设置:密码长度最小值（%s个字符）", pwdMinLength));
        }

        //密码规则 大小写 数字 特殊字符
        int count = 0;
        if (pwdComplexity) {
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                if (Character.isLowerCase(c)) {
                    count++;
                    break;
                }
            }
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                if (Character.isUpperCase(c)) {
                    count++;
                    break;
                }
            }
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                if (Character.isDigit(c)) {
                    count++;
                    break;
                }
            }
            if (count == 2) {
                //* ? ! & ￥ $ % ^ # , . / @ " ; : > < ] [ } { - = + _ \ | 》 《 。 ， 、 ？ ’ ‘ “ ” ~ `）
                String pattern = ".*[*?!&￥$%^#,./@\";:><\\]\\[}{\\-=+_\\\\|》《。，、？’‘“”~`）].*$";
                if (Pattern.matches(pattern, password)) {
                    count++;
                }
            }
            if (count < 3) {
                throw new ValidationException(rec.l10n("不符合安全设置:密码必须符合复杂性要求: 至少包含大写字母、小写字母、数字、特殊字符四类中的三类"));
            }
        }
    }

    /**
     * 检查是否需要修改密码
     */
    public void checkPasswordExpiration(Records rec, Records user) {
        //是否需要更改密码
        boolean mustUpdatePassword = Utils.toBoolean(user.get("must_update_password"), false);
        if (mustUpdatePassword) {
            throw new AccessException(user.l10n("请重新设置密码"), SecurityCode.RESET_PASSWORD);
        }
        Records config = user.getEnv().getConfig();
        //密码有效期(天)
        int pwdValidity = Utils.toInt(config.get("pwd_validity"), Integer.MAX_VALUE);
        //上次更新密码时间
        Timestamp updatePasswordTime = user.getDateTime("update_password_time");
        if (updatePasswordTime != null) {
            long nowTime = System.currentTimeMillis();
            if (nowTime > Utils.addDays(updatePasswordTime, pwdValidity).getTime()) {
                throw new AccessException(user.l10n("密码已过期,请修改密码"), SecurityCode.PASSWORD_EXPIRED);
            }
        }
    }

    /**
     * 检查凭据
     */
    public void checkCredentials(Records rec, Records user, String password, Map<String, Object> userAgent) {
        checkAccountLock(rec, user);
        Object hashed = user.get("password");
        byte[] bytes = Base64.getDecoder().decode(password);
        String pwd = DigestUtils.md5DigestAsHex(bytes);
        if (!hashed.equals(pwd)) {
            if (!hashed.equals(new String(bytes, StandardCharsets.UTF_8))) {
                int errorPasswordCount = user.getInteger("error_password_count") + 1;
                loginFailed(rec, user.getId(), null, userAgent);
                Cursor cursor = user.getEnv().getCursor();
                cursor.execute("UPDATE rbac_user SET error_password_count=%s,error_password_time="
                    + cursor.getSqlDialect().getNowUtc() + " WHERE id = %s", Arrays.asList(errorPasswordCount, user.getId()));
                cursor.commit();
                throw new AccessException(user.l10n("密码错误%s次", errorPasswordCount), SecurityCode.PASSWORD_ERROR);
            } else {
                //加密明文的密码
                String encoded = DigestUtils.md5DigestAsHex(bytes);
                Cursor cr = user.getEnv().getCursor();
                cr.execute("UPDATE rbac_user SET password=%s WHERE id=%s", Arrays.asList(encoded, user.getId()));
                user.getEnv().getCache().remove(user, user.getMeta().getField("password"));
            }
        }
    }
}
