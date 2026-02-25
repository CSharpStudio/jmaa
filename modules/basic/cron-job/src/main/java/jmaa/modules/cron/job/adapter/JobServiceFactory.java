package jmaa.modules.cron.job.adapter;

public class JobServiceFactory {
    static JobService instance;

    public static JobService getService() {
        return instance;
    }

    public static void registerService(JobService service) {
        instance = service;
    }
}
