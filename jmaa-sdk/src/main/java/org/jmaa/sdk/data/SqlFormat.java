package org.jmaa.sdk.data;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * sql 格式化
 *
 * @author Eric Liang
 */
public class SqlFormat {
    String sql;
    List<Object> params;
    static Pattern patten = Pattern.compile("?", Pattern.LITERAL);

    /**
     * 创建实例
     *
     * @param sql
     * @param params
     */
    public SqlFormat(String sql, List<Object> params) {
        this.sql = sql;
        this.params = params;
    }

    /**
     * 获取sql字符串
     *
     * @return
     */
    public String getSql() {
        return sql;
    }

    /**
     * 获取参数
     *
     * @return
     */
    public List<Object> getParmas() {
        return params;
    }

    /**
     * 生成拼接参数的sql字符串
     */
    @Override
    public String toString() {
        String result = sql;
        for (Object p : params) {
            if (p instanceof String) {
                result = patten.matcher(result).replaceFirst(Matcher.quoteReplacement(String.format("'%s'", p)));
            } else {
                result = patten.matcher(result).replaceFirst(Matcher.quoteReplacement(String.format("%s", p)));
            }
        }
        return result;
    }

    /**
     * 生成拼接参数的sql字符串
     */
    public String toString(SqlDialect sqlDialect) {
        String result = sql;
        for (Object p : params) {
            if (p instanceof String) {
                result = patten.matcher(result).replaceFirst(Matcher.quoteReplacement(String.format("'%s'", sqlDialect.prepareObject(p))));
            } else {
                result = patten.matcher(result).replaceFirst(Matcher.quoteReplacement(String.format("%s", sqlDialect.prepareObject(p))));
            }
        }
        return result;
    }
}
