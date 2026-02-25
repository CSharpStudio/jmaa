package org.jmaa.sdk.util;

import java.util.Date;

/**
 * @author Eric Liang
 */
public interface License {

    void update(String company, String license);

    /**
     * 0 未注册，1 使用期限制， 2 登录用户数限制， 3 已授权
     */
    int getStyle();

    /**
     * 获取用户数量限制
     */
    int getUserLimit();

    /**
     * 获取产线数量限制
     */
    int getLineLimit();

    /**
     * 获取有效期
     */
    Date getDueDate();

    /**
     * 获取授权公司
     */
    String getCompany();

    /**
     * 获取授权信息
     */
    String getInfo();
}
