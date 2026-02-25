package org.jmaa.sdk.util;

/**
 * @author Eric Liang
 */
public class SecurityCode {
    /** 未认证 */
    public final static int UNAUTHORIZED = 7100;
    /** 账号不存在 */
    public final static int LOGIN_NOT_FOUND = 7101;
    /** 密码错误 */
    public final static int PASSWORD_ERROR = 7102;
    public final static int TOKEN_ERROR = 7103;
    /** 强制重置密码 */
    public final static int RESET_PASSWORD = 7104;
    /** 锁定 */
    public final static int LOCKED = 7105;
    /** 密码已过期 */
    public final static int PASSWORD_EXPIRED = 7106;
	/** 令牌已过期 */
	public final static int TOKEN_EXPIRED = 7107;
    /** 验证码错误 */
    public final static int CAPTCHA_ERROR = 7108;
    /** 未授权 */
    public final static int NO_PERMISSION = 7110;
    /** 其它客户端登录 */
    public final static int LOGGED = 7201;
}
