package org.jmaa.sdk.bus;

import org.jmaa.sdk.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 事件总线，进程内的消息订阅+消费，用于模块解耦
 * 1.不需要队列，消息都是实时接收的
 * 2.在同一请求中完成，不需要分布式事务
 * @author Eric Liang
 */
public class EventBus {
    private static Logger logger = LoggerFactory.getLogger(EventBus.class);

    Map<Class, List<Subscriber>> subscribers = new HashMap<>();

    /**
     * 关闭，清空订阅者列表
     */
    public void close() {
        subscribers.clear();
    }

    /**
     * 注册订阅者
     *
     * @param clazz      消息类型
     * @param subscriber 订阅者
     * @param <T>
     */
    public <T> void subscribe(Class<T> clazz, Subscriber<T> subscriber) {
        List<Subscriber> list = subscribers.get(clazz);
        if (list == null) {
            list = new ArrayList<>();
            subscribers.put(clazz, list);
        }
        list.add(subscriber);
    }

    /**
     * 取消订阅者
     *
     * @param clazz      消息类型
     * @param subscriber 订阅者
     * @param <T>
     * @return
     */
    public <T> boolean unsubscribe(Class<T> clazz, Subscriber<T> subscriber) {
        List<Subscriber> list = subscribers.get(clazz);
        if (list != null) {
            return list.remove(subscriber);
        }
        return false;
    }

    /**
     * 发布消息
     *
     * @param env
     * @param event
     */
    public void publish(Environment env, Object event) {
        Objects.requireNonNull(event, "event参数不能为空");
        Class clazz = event.getClass();
        List<Subscriber> list = subscribers.get(clazz);
        if (list != null) {
            try {
                for (Subscriber subscriber : list) {
                    try {
                        subscriber.subscribe(env, event);
                    } catch (Exception exception) {
                        logger.error(String.format("订阅者[%s]消费失败", subscriber.getClass()), exception);
                    }
                }
            } catch (Exception e) {
                logger.error("订阅者消费失败", e);
                e.printStackTrace();
            }
        }
    }
}
