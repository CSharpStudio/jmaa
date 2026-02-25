package jmaa.modules.cron.job.adapter;

import java.util.Map;

/**
 * 调度任务服务
 */
public interface JobService {

    JobItem getJobItem(String jobId);

    default boolean isEnable() {
        return false;
    }

    JobItem createJobItem();

    Object addJobItem(JobItem job);

    void updateJobItem(JobItem job);

    void startJob(Object jobId);

    void trigger(Object jobId, Map<String, Object> param);

    void stopJob(Object jobId);

    void deleteJob(Object jobId);
}
