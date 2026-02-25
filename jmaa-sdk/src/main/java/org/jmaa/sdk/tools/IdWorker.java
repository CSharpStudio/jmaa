package org.jmaa.sdk.tools;

/**
 * id生成器
 *
 * @author Eric Liang
 */
public class IdWorker {
    /** todo 参数从网卡地址获取 */
    static SnowFlake snowFlake = new SnowFlake(1, 1);

    public static String nextId() {
        long id = snowFlake.nextId();
        String str = Long.toString(id, 36);
        return StringUtils.leftPad(str, 13, '0');
    }
}
