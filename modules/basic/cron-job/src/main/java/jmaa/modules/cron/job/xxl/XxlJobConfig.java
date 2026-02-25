package jmaa.modules.cron.job.xxl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "xxl.job.admin")
public class XxlJobConfig {
    /**
     * 是否开启
     */
    private Boolean enable;

    /**
     * xxlJob服务
     */
    private String address;

    /**
     * 用户名
     */
    private String user_name;

    /**
     * 密码
     */
    private String password;

    /**
     * 执行器id
     */
    private Integer job_group;

    /**
     * 执行配置
     */
    private ExecutorConfig executor;

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserName() {
        return user_name;
    }

    public void setUserName(String userName) {
        this.user_name = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getJobGroup() {
        return job_group;
    }

    public void setJobGroup(Integer jobGroup) {
        this.job_group = jobGroup;
    }

    public ExecutorConfig getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorConfig executor) {
        this.executor = executor;
    }

    public static class ExecutorConfig {
        private String app_name;
        private String title;
        private String ip;
        private Integer port;
        private String log_path;
        private Integer log_retention_days;
        private String routing_policy;
        private String access_token;

        public String getAppName() {
            return app_name;
        }

        public void setAppName(String appName) {
            app_name = appName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getLogPath() {
            return log_path;
        }

        public void setLogPath(String logPath) {
            log_path = logPath;
        }

        public Integer getLogRetentionDays() {
            return log_retention_days;
        }

        public void setLogRetentionDays(Integer days) {
            log_retention_days = days;
        }

        public String getRoutingPolicy() {
            return routing_policy;
        }

        public void setRoutingPolicy(String policy) {
            routing_policy = policy;
        }

        public String getAccessToken() {
            return access_token;
        }

        public void setAccessToken(String token) {
            access_token = token;
        }
    }
}
