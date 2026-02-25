package jmaa.modules.cron.job.models;

import jmaa.modules.cron.job.adapter.JobItem;
import jmaa.modules.cron.job.adapter.JobService;
import jmaa.modules.cron.job.adapter.JobServiceFactory;
import jmaa.modules.cron.job.utils.CronUtils;
import org.jmaa.sdk.*;
import org.jmaa.sdk.tools.ObjectUtils;
import jmaa.modules.cron.job.xxl.XxlJobInfo;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

import jmaa.modules.cron.job.xxl.XxlJobService;

@Model.Meta(name = "job.task", label = "定时任务")
public class JobTask extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field module_id = Field.Many2one("ir.module").label("所属应用");
    static Field module_name = Field.Char().label("应用").store(false).compute("computeModuleName", "inverseModuleName");
    static Field cron = Field.Char().label("Cron").required().length(500);
    static Field next_exec_time = Field.Char().label("下次执行时间").compute("computeNextExecTime");
    static Field job_handler_id = Field.Many2one("job.handler").label("执行方法").required().ondelete(DeleteMode.Cascade);
    static Field xxl_job_id = Field.Integer();
    static Field status = Field.Selection().label("运行状态").selection(new Options() {{
        put("0", "停止");
        put("1", "执行中");
    }}).defaultValue("0");
    static Field timeout = Field.Integer().defaultValue("15").label("任务超时时间(分钟)").required().greaterThen(0);
    static Field retry_count = Field.Integer().defaultValue("3").label("重试次数").required();
    static Field description = Field.Char().label("描述");
    static Field execute_user_id = Field.Many2one("rbac.user").label("任务执行用户").required();
    static Field company_id = Field.Many2one("res.company").label("执行任务的公司").required();
    static Field params = Field.Char().label("参数").help("JSON格式参数");
    static Field job_log_ids = Field.One2many("job.log", "job_id").label("执行日志");

    public String computeModuleName(Records r) {
        return r.getRec("module_id").getString("present");
    }

    public void inverseModuleName(Records r) {
        String moduleName = r.getString("module_name");
        Records moduleRec = r.getEnv().get("ir.module").find(Criteria.equal("name", moduleName));
        r.set("module_id", moduleRec.getId());
    }

    public String computeNextExecTime(Records r) {
        return CronUtils.getNextExecTime(r.getString("cron"), 5).stream().collect(Collectors.joining("\r\n"));
    }

    @ServiceMethod(label = "测试Cron", doc = "获取最近5次任务执行时间")
    public Object testJobByCron(Records rec, String cron) {
        return CronUtils.getNextExecTime(cron, 5);
    }

    @OnChange("execute_user_id")
    public Object onChangeUser(Records records) {
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("company_id", null);
        return resMap;
    }

    @ServiceMethod(auth = "read", doc = "根据用户id查询组织", label = "查询组织")
    public Object searchCompany(Records records, String executeUser) {
        Records recordsUser = records.getEnv().get("rbac.user").find(Criteria.equal("id", executeUser));
        Records companyIds = recordsUser.getRec("company_ids");
        Map<String, Object> resMap = new HashMap<>();
        if (companyIds.any()) {
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Records company : companyIds) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", company.getId());
                data.put("present", company.getString("present"));
                dataList.add(data);
            }
            resMap.put("values", dataList);
        } else {
            resMap.put("values", new ArrayList<>());
        }
        return resMap;
    }

    @Override
    public void update(Records records, Map<String, Object> values) {
        callSuper(records, values);
        if (records.getEnv().getRegistry().isLoaded()) {
            JobService service = JobServiceFactory.getService();
            if(service.isEnable()){
                for (Records record : records) {
                    JobItem jobItem = service.getJobItem(record.getId());
                    if (jobItem == null) {
                        jobItem = service.createJobItem();
                        toJobItem(record, jobItem);
                        jobItem.setRecordId(record.getId());
                        service.addJobItem(jobItem);
                    } else {
                        toJobItem(record, jobItem);
                        service.updateJobItem(jobItem);
                    }
                }
            }
        }
    }

    @Override
    public Records createBatch(Records records, List<Map<String, Object>> valuesList) {
        records = (Records) callSuper(records, valuesList);
        JobService service = JobServiceFactory.getService();
        if(service.isEnable()){
            for (Records record : records) {
                JobItem jobItem = service.createJobItem();
                toJobItem(record, jobItem);
                jobItem.setRecordId(record.getId());
                Object jobId = service.addJobItem(jobItem);
                if (record.getBoolean("status")) {
                    service.startJob(jobId);
                }
            }
        }
        return records;
    }

    public void toJobItem(Records record, JobItem jobItem) {
        jobItem.setJobDesc(record.getString("name"));
        jobItem.setJobCron(record.getString("cron"));
        jobItem.setExecutorHandler(record.getRec("job_handler_id").getString("method"));
        jobItem.setExecutorTimeout(record.getInteger("timeout") * 60);
        jobItem.setExecutorFailRetryCount(record.getInteger("retry_count"));
        Map<String, Object> params = new HashMap<>();
        String p = record.getString("params");
        if (Utils.isNotEmpty(p)) {
            params.put("params", ObjectUtils.fromJsonString(p));
        }
        params.put("job_id", record.getId());
        params.put("user_id", record.getRec("execute_user_id").getId());
        params.put("company_id", record.getRec("company_id").getId());
        params.put("tenant", record.getEnv().getRegistry().getTenant().getKey());
        jobItem.setExecutorParam(com.alibaba.fastjson.JSON.toJSONString(params));
    }

    @ServiceMethod(doc = "立即触发执行一次任务", label = "执行一次")
    public Object executeOnce(Records records) {
        for (Records record : records) {
            // 检查应用是否安装
            Records module = record.getRec("module_id");
            String status = module.getString("state");
            if (!"installed".equals(status)) {
                throw new ValidationException(record.l10n("请先安装应用[%s]", module.get("name")));
            }
        }
        JobService service = JobServiceFactory.getService();
        for (Records record : records) {
            JobItem jobItem = service.getJobItem(record.getId());
            if (jobItem != null) {
                Map<String, Object> params = new HashMap<>();
                String p = record.getString("params");
                if (Utils.isNotEmpty(p)) {
                    params.put("params", ObjectUtils.fromJsonString(p));
                }
                params.put("job_id", record.getId());
                params.put("user_id", record.getRec("execute_user_id").getId());
                params.put("company_id", record.getRec("company_id").getId());
                params.put("tenant", record.getEnv().getRegistry().getTenant().getKey());
                service.trigger(jobItem.getId(), params);
            }
        }
        return Action.success();
    }

    /**
     * 开始执行任务
     */
    @ServiceMethod(label = "启动")
    public Object startTask(Records records) {
        JobService service = JobServiceFactory.getService();
        for (Records record : records) {
            // 检查应用是否安装
            Records module = record.getRec("module_id");
            String status = module.getString("state");
            if (!"installed".equals(status)) {
                throw new ValidationException(record.l10n("请先安装应用[%s]", module.get("name")));
            }
        }
        for (Records record : records) {
            JobItem jobItem = service.getJobItem(record.getId());
            if (jobItem != null) {
                service.startJob(jobItem.getId());
            }
        }
        records.set("status", "1");
        return Action.success();
    }

    /**
     * 停止执行任务
     */
    @ServiceMethod(label = "停止")
    public Object stopTask(Records records) {
        JobService service = JobServiceFactory.getService();
        for (Records record : records) {
            JobItem jobItem = service.getJobItem(record.getId());
            if (jobItem != null) {
                service.stopJob(jobItem.getId());
            }
        }
        records.set("status", "0");
        return Action.success();
    }

    @OnDelete
    public void onDeleted(Records records) {
        JobService service = JobServiceFactory.getService();
        if (service.isEnable()) {
            for (Records record : records) {
                JobItem jobItem = service.getJobItem(record.getId());
                if (jobItem != null) {
                    service.deleteJob(jobItem.getId());
                }
            }
        }
    }

    @ServiceMethod(label = "清理日志", doc = "清理7天前日志")
    public Object clearLog(Records records) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date dt = calendar.getTime();
        records.getEnv().getCursor().execute("delete from job_log where start_time < %s", Arrays.asList(dt));
        return Action.success();
    }
}
