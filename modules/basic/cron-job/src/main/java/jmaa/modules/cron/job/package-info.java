@Manifest(
    name = "cron-job",
    label = "调度任务",
    category = "基础模块",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        JobTask.class,
        JobHandler.class,
        JobLog.class,
        IrModule.class,
    },
    data = {
        "views/task_job.xml",
        "views/task_job_handler.xml",
        "views/menus.xml"
    },
    beans = {
        XxlJobConfig.class,
        XxlJobInitialization.class,
    },
    jobs = {
        TestJob.class,
    })
package jmaa.modules.cron.job;

import jmaa.modules.cron.job.models.IrModule;
import jmaa.modules.cron.job.models.JobHandler;
import jmaa.modules.cron.job.models.JobLog;
import jmaa.modules.cron.job.models.JobTask;
import jmaa.modules.cron.job.xxl.XxlJobConfig;
import jmaa.modules.cron.job.xxl.XxlJobInitialization;
import org.jmaa.sdk.Manifest;
import jmaa.modules.cron.job.jobs.TestJob;
