package org.jmaa.base.models;

import org.jmaa.base.utils.Claims;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.SecurityCode;
import org.jmaa.sdk.util.ServerDate;
import org.apache.commons.collections4.SetUtils;
import org.springframework.util.DigestUtils;

import java.sql.Timestamp;
import java.util.*;

/**
 * 用户信息。
 * 同一账号不能同时登录多台PC或PDA
 *
 * @author Eric Liang
 */
@Model.Meta(name = "rbac.user", label = "用户", present = {"name", "login"}, presentFormat = "{name}({login})")
public class RbacUser extends Model {
    static Field name = Field.Char().label("姓名").required();
    static Field login = Field.Char().label("账号").help("登录账号").required().unique();
    static Field password = Field.Char().label("密码").help("登录密码").prefetch(false);
    static Field login_time = Field.DateTime().label("最后登录时间").related("log_ids.log_time");
    static Field email = Field.Char().label("邮箱").unique(Constants.UNIQUE_MODE.Unique);
    static Field mobile = Field.Char().label("手机号").unique(Constants.UNIQUE_MODE.Unique);
    static Field lang = Field.Selection(Selection.method("getLanguages")).label("语言")
        .defaultValue(Default.method("getLanguageDefault")).help("修改后需要重新登录才生效");
    static Field tz = Field.Selection(Selection.method("getTimezone")).label("时区")
        .defaultValue(Default.method("getTimezoneDefault"));
    static Field log_ids = Field.One2many("rbac.user.log", "user_id");
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field role_ids = Field.Many2many("rbac.role", "rbac_role_user", "user_id", "role_id").label("角色");
    static Field company_id = Field.Many2one("res.company").label("主公司").ondelete(DeleteMode.Restrict);
    static Field company_ids = Field.Many2many("res.company", "rbac_user_company", "user_id", "company_id").label("公司")
        .help("有权访问的所有公司").required().defaultValue(Default.method("defaultCompanies"));
    static Field image = Field.Image().label("头像");
    static Field theme = Field.Char().label("主题");
    static Field ip = Field.Char().label("登录IP");
    static Field pda_ip = Field.Char().label("登录IP");
    static Field token_expiration = Field.DateTime().label("令牌过期时间");
    static Field pda_token_expiration = Field.DateTime().label("PDA令牌过期时间");
    static Field online = Field.Boolean().compute("isOnline").label("是否在线");
    static Field update_password_time = Field.DateTime().label("修改密码时间");
    static Field must_update_password = Field.Boolean().label("是否需要修改密码").defaultValue(true);
    static Field error_password_count = Field.Integer().label("密码错误次数").defaultValue(0);
    static Field error_password_time = Field.DateTime().label("密码错误时间");
    static Field mode = Field.Selection(new Options() {{
        put("user", "用户");
        put("jwt", "令牌");
    }}).label("类型").defaultValue("user").required();
    static Field shared = Field.Boolean().label("是否公用账号").help("公用账号允许多台电脑同时登录");

    public Object defaultCompanies(Records record) {
        return record.getEnv().getCompany();
    }

    public boolean isOnline(Records rec) {
        Timestamp pc = rec.getDateTime(token_expiration);
        Timestamp pda = rec.getDateTime(pda_token_expiration);
        return (pc != null && pc.getTime() > System.currentTimeMillis()) || (pda != null && pda.getTime() > System.currentTimeMillis());
    }

    public Map<String, String> getLanguages(Records rec) {
        return (Map<String, String>) rec.getEnv().get("res.lang").call("getInstalled");
    }

    public String getLanguageDefault(Records rec) {
        return rec.getEnv().getLang();
    }

    public String getTimezoneDefault(Records rec) {
        return rec.getEnv().getTimezone();
    }

    @Constrains("company_ids")
    public void checkCompany(Records records) {
        for (Records record : records) {
            List<String> companyIds = Arrays.asList(record.getRec(company_ids).getIds());
            if (companyIds.isEmpty()) {
                record.set(company_id, null);
            } else {
                Records company = record.getRec(company_id);
                if (company.any()) {
                    if (!companyIds.contains(company.getId())) {
                        record.set(company_id, companyIds.get(0));
                    }
                } else {
                    record.set(company_id, companyIds.get(0));
                }
            }
        }
    }

    /**
     * 获取所有时区
     */
    public Map<String, String> getTimezone(Records res) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String tz : TimeZone.getAvailableIDs()) {
            map.put(tz, String.format("%s - (%s)", TimeZone.getTimeZone(tz).getDisplayName(), tz));
        }
        return map;
    }

    /**
     * 账号验证
     */
    @Model.Constrains("login")
    public void checkLogin(Records records) {
        for (Records record : records) {
            String login = record.getString("login");
            if (login.contains("/")) {
                throw new ValidationException(record.l10n("账号不能包含/"));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> read(Records rec, Collection<String> fields) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) rec.callSuper(RbacUser.class, "read", fields);
        for (Map<String, Object> data : list) {
            if (data.containsKey("password")) {
                data.put("password", "********");
            }
        }
        return list;
    }

    /**
     * 密码登录
     */
    public Map<String, Object> login(Records rec, String login, String password, boolean remember, boolean force,
                                     Map<String, Object> userAgent) {
        Criteria criteria = Criteria.equal("active", true).and(Criteria.equal("mode", "user"),
            Criteria.equal("login", login).or(Criteria.equal("mobile", login), Criteria.equal("email", login)));
        Records user = rec.find(criteria, 0, 1, null);
        Records security = rec.getEnv().get("rbac.security");
        if (!user.any()) {
            security.call("loginFailed", null, login, userAgent);
            throw new AccessException(rec.l10n("账号[%s]不存在", login), SecurityCode.LOGIN_NOT_FOUND);
        }
        security.call("checkCredentials", user, password, userAgent);
        security.call("checkPasswordExpiration", user);
        boolean isPda = isPda(userAgent);
        updateOnline(user, force, userAgent, isPda);
        security.call("loginSuccess", user, login, userAgent);
        user.set(error_password_count, 0);
        user.set(must_update_password, false);
        Claims claims = (Claims) rec.getEnv().get("rbac.token")
            .call("updateToken", user, isPda ? "pda" : "pc", remember ? 168 : 8);
        user.set(isPda ? "pda_token_expiration" : "token_expiration", claims.getExp());
        List<Map<String, Object>> images = (List<Map<String, Object>>) user.get("image");
        return new KvMap(8)
            .set("login", user.get("login"))
            .set("name", user.get("name"))
            .set("lang", Utils.asList(user.get("lang"), user.getSelection("lang")))
            .set("tz", user.get("tz"))
            .set("theme", user.get("theme"))
            .set("token", claims.getKey())
            .set("image", Utils.isNotEmpty(images) ? images.get(0).get("id") : null)
            .set("company", user.getRec("company_id").getPresent())
            .set("id", user.getId());
    }

    boolean isPda(Map<String, Object> userAgent) {
        String agent = (String) userAgent.get("USER-AGENT");
        if (Utils.isNotEmpty(agent)) {
            agent = agent.toLowerCase();
            return agent.contains("iphone") || agent.contains("android");
        }
        return false;
    }

    /**
     * 更新在线ip
     */
    public void updateOnline(Records user, boolean force, Map<String, Object> userAgent, boolean isPda) {
        if (user.getBoolean("shared")) {
            return;
        }
        String loginIp = (String) userAgent.get("REMOTE_ADDR");
        if (!force) {
            Timestamp expiration = user.getDateTime(isPda ? pda_token_expiration : token_expiration);
            if (expiration != null && expiration.getTime() > System.currentTimeMillis()) {
                String ip = user.getString(isPda ? "pda_ip" : "ip");
                if (!Utils.equals(loginIp, ip)) {
                    throw new ValidationException(user.l10n(String.format("该账号已经在IP为%s的设备登录", ip)), SecurityCode.LOGGED);
                }
            }
        }
        user.set(isPda ? "pda_ip" : "ip", loginIp);
    }

    /**
     * 登出，清理令牌
     */
    @Model.ServiceMethod(label = "退出登录", auth = Constants.ANONYMOUS)
    public Object logout(Records rec) {
        Records user = rec.getEnv().getUser();
        user.set(token_expiration, null);
        user.set(ip, null);
        //TODO判断网页还是PDA
        user.getEnv().get("rbac.token").call("removeToken", rec.getEnv().getUserId());
        Records security = rec.getEnv().get("rbac.security");
        security.call("logout", user, new HashMap<String, Object>());
        return true;
    }

    /**
     * 读取个人账号信息
     */
    @Model.ServiceMethod(label = "读取个人账号信息", auth = Constants.ANONYMOUS, ids = false)
    public Map<String, Object> getPersonal(Records rec, Collection<String> fields) {
        return rec.getEnv().getUser().read(fields).get(0);
    }

    /**
     * 更新个人账号信息
     */
    @Model.ServiceMethod(label = "更新个人账号信息", auth = Constants.ANONYMOUS, ids = false)
    public void updatePersonal(Records rec, Map<String, Object> values) {
        //TODO控制能修改的信息
        rec.getEnv().getUser().update(values);
    }

    /**
     * 修改密码
     */
    @Model.ServiceMethod(label = "修改密码", ids = false, auth = Constants.ANONYMOUS)
    public Object changePassword(Records rec, @Doc(doc = "旧密码") String oldPassword,
                                 @Doc(doc = "新密码") String newPassword) {
        if (StringUtils.isBlank(newPassword)) {
            throw new ValidationException(rec.l10n("设置空密码不符合安全要求"));
        }
        Records user = rec.getEnv().getUser();
        Records security = rec.getEnv().get("rbac.security");
        security.call("checkCredentials", user, oldPassword, Collections.emptyMap());
        security.call("checkPasswordRule", new String(Base64.getDecoder().decode(newPassword)));
        String pwd = DigestUtils.md5DigestAsHex(Base64.getDecoder().decode(newPassword));
        user.set(password, pwd);
        user.set(error_password_count, 0);
        user.set(update_password_time, new ServerDate());
        return Action.reload(rec.l10n("保存成功"));
    }

    @Override
    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        if (!values.containsKey("password")) {
            values.put("password", rec.getEnv().getConfig().getString("default_pwd"));
        }
        return (Map<String, Object>) rec.callSuper(RbacUser.class, "addMissingDefaultValues", values);
    }

    /**
     * 重置密码
     */
    @Model.ServiceMethod(label = "重置密码")
    public Object resetPassword(Records rec) {
        if (!rec.getEnv().isAdmin()) {
            throw new ValidationException(rec.l10n("非管理员角色不能重置密码"));
        }
        rec.set("password", rec.getEnv().getConfig().getString("default_pwd"));
        rec.set("update_password_time", new ServerDate());
        rec.set("must_update_password", true);
        rec.set("error_password_count", 0);
        return Action.reload(rec.l10n("保存成功"));
    }

    /**
     * 用户允许匿名查询自己账号相关数据
     */
    @Override
    @SuppressWarnings("unchecked")
    @Model.ServiceMethod(auth = Constants.ANONYMOUS, label = "获取关联模型的数据", doc = "读取关联模型的记录")
    public Map<String, Object> searchByField(Records rec,
                                             @Doc(doc = "关联的字段") String relatedField,
                                             @Doc(doc = "查询条件") Criteria criteria,
                                             @Doc(doc = "偏移量") Integer offset,
                                             @Doc(doc = "行数") Integer limit,
                                             @Doc(doc = "读取的字段") Collection<String> fields,
                                             @Doc(doc = "排序") String order) {
        return (Map<String, Object>) rec.callSuper(RbacUser.class, "searchByField", relatedField, criteria, offset, limit, fields, order);
    }

    @Model.ServiceMethod(auth = Constants.ANONYMOUS, label = "获取用户的公司", doc = "获取当前登录用户的公司")
    public List<Map<String, Object>> getUserCompanies(Records rec) {
        Records user = rec.getEnv().getUser();
        Records companies = user.getRec(company_ids);
        if (companies.any()) {
            List<Map<String, Object>> result = companies.read(Collections.singletonList("present"));
            Records main = user.getRec(company_id);
            if (!main.any()) {
                user.set(company_id, companies.getIds()[0]);
            }
            String mainId = user.getRec(company_id).getId();
            for (Map<String, Object> row : result) {
                if (mainId.equals(row.get("id"))) {
                    row.put("main", true);
                    break;
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Model.ServiceMethod(auth = Constants.ANONYMOUS, label = "更新用户的主公司", doc = "更新当前登录用户的主公司")
    public void updateUserCompany(Records rec, String companyId) {
        Records user = rec.getEnv().getUser();
        Set<String> ids = SetUtils.hashSet(user.getRec(company_ids).getIds());
        if (!ids.contains(companyId)) {
            throw new ValidationException(rec.l10n("用户没有该公司的权限"));
        }
        user.set(company_id, companyId);
    }

    @ServiceMethod(label = "设置密码")
    public Object setPassword(Records rec, @Doc(doc = "密码") String password) {
        if (StringUtils.isBlank(password)) {
            throw new ValidationException(rec.l10n("设置空密码不符合安全要求"));
        }
        byte[] bytes = Base64.getDecoder().decode(password);
        rec.getEnv().get("rbac.security").call("checkPasswordRule", new String(bytes));
        rec.set("password", DigestUtils.md5DigestAsHex(bytes));
        rec.set("update_password_time", new ServerDate());
        rec.set("error_password_count", 0);
        rec.set("must_update_password", true);
        return Action.reload(rec.l10n("保存成功"));
    }

    public void loginChangePassword(Records rec,
                                    @Doc(doc = "原密码") String oldPassword,
                                    @Doc(doc = "新密码") String newPassword,
                                    @Doc(doc = "账号") String login) {
        if (StringUtils.isBlank(newPassword)) {
            throw new ValidationException(rec.l10n("设置空密码不符合安全要求"));
        }

        Criteria criteria = Criteria.equal("login", login).or(Criteria.equal("mobile", login), Criteria.equal("email", login));
        Records user = rec.getEnv().get("rbac.user").find(criteria);
        if (!user.any()) {
            throw new AccessException(rec.l10n("账号[%s]不存在,请重新输入", login));
        }

        byte[] bytes = Base64.getDecoder().decode(newPassword);

        Records security = rec.getEnv().get("rbac.security");
        security.call("checkCredentials", user, oldPassword, Collections.emptyMap());
        security.call("checkPasswordRule", new String(bytes));

        user.set(password, DigestUtils.md5DigestAsHex(bytes));
        user.set(update_password_time, new ServerDate());
        user.set(must_update_password, false);
        user.set(error_password_count, 0);
    }
}
