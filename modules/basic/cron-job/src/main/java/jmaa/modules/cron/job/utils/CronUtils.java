package jmaa.modules.cron.job.utils;

import org.jmaa.sdk.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Cron表达式工具
 */
public class CronUtils {
    /**
     * 返回一个布尔值代表一个给定的Cron表达式的有效性
     *
     * @param cronExpression Cron表达式
     * @return boolean 表达式是否有效
     */
    public static boolean isValid(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }

    /**
     * 列出近numTimes次的执行时间
     *
     * @param cronExpression cron表达式
     * @param numTimes       下几次运行时间
     * @return
     */
    public static List<String> getNextExecTime(String cronExpression, int numTimes) {
        List<String> list = new ArrayList<>();
        try {
            CronExpression cron = new CronExpression(cronExpression);
            Date dt = new Date();
            for (int i = 0; i < numTimes; i++) {
                dt = cron.getTimeAfter(dt);
                list.add(Utils.format(dt, "yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            //不处理
        }
        return list;
    }
}


