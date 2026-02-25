package jmaa.modules.cron.job.jobs;

import org.jmaa.sdk.job.CronJob;
import org.jmaa.sdk.job.CronJobBase;

public class TestJob extends CronJobBase {
    @CronJob("testJob")
    public Object test(String params) {
        //测试用
        return true;
    }
}
