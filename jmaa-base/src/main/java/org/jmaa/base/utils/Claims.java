package org.jmaa.base.utils;

import java.util.Date;

/**
 * 声明
 */
public class Claims {
    private String key;
    private Date exp;

    /**
     * 键
     *
     * @return
     */
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 有效期
     *
     * @return
     */
    public Date getExp() {
        return exp;
    }

    public void setExp(Date exp) {
        this.exp = exp;
    }
}
