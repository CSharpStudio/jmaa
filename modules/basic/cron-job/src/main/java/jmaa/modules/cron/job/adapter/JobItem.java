package jmaa.modules.cron.job.adapter;

import java.util.Date;

public interface JobItem {

    Object getId();

    void setId(Object id);

    String getJobCron();

    void setJobCron(String jobCron);

    String getJobDesc();

    void setJobDesc(String jobDesc);

    Date getAddTime();

    void setAddTime(Date addTime);

    Date getUpdateTime();

    void setUpdateTime(Date updateTime);

    String getRecordId();

    void setRecordId(String id);

    String getAlarmEmail();

    void setAlarmEmail(String alarmEmail);

    String getExecutorRouteStrategy();

    void setExecutorRouteStrategy(String executorRouteStrategy);


    String getExecutorHandler();

    void setExecutorHandler(String executorHandler);

    String getExecutorParam();

    void setExecutorParam(String executorParam);

    String getExecutorBlockStrategy();

    void setExecutorBlockStrategy(String executorBlockStrategy);

    int getExecutorTimeout();

    void setExecutorTimeout(int executorTimeout);

    int getExecutorFailRetryCount();

    void setExecutorFailRetryCount(int executorFailRetryCount);

    String getGlueType();

    void setGlueType(String glueType);

    String getGlueSource();

    void setGlueSource(String glueSource);

    String getGlueRemark();

    void setGlueRemark(String glueRemark);

    Date getGlueUpdatetime();

    void setGlueUpdatetime(Date glueUpdatetime);

    String getChildJobId();

    void setChildJobId(String childJobId);

    int getTriggerStatus();

    void setTriggerStatus(int triggerStatus);

    long getTriggerLastTime();

    void setTriggerLastTime(long triggerLastTime);

    long getTriggerNextTime();

    void setTriggerNextTime(long triggerNextTime);
}
