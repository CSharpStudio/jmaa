package org.jmaa.sdk.bus;

import org.jmaa.sdk.core.Environment;

/**
 * 订阅者，根据消息类型订阅指定消息
 *
 * @author Eric Liang
 * @param <T> 消息类
 */
public interface Subscriber<T> {
    /**
     * 消费消息
     *
     * @param env   环境
     * @param event 消息对象
     */
    void subscribe(Environment env, T event);
}
