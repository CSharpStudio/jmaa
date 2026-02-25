package jmaa.modules.cron.job.xxl;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : eric
 **/
@Configuration
public class XxlJobInitialization {
    private Logger logger = LoggerFactory.getLogger(XxlJobInitialization.class);

    @Autowired
    private XxlJobConfig xxlJobConfig;

    @Bean
    @ConditionalOnProperty(
        name = "xxl.job.admin.enable",
        havingValue = "true",
        matchIfMissing = false
    )
    public XxlJobSpringExecutor xxlJobExecutor() {
        logger.info("xxl-job executor init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(xxlJobConfig.getAddress());
        xxlJobSpringExecutor.setAppName(xxlJobConfig.getExecutor().getAppName());
        xxlJobSpringExecutor.setIp(xxlJobConfig.getExecutor().getIp());
        xxlJobSpringExecutor.setPort(xxlJobConfig.getExecutor().getPort());
        xxlJobSpringExecutor.setLogPath(xxlJobConfig.getExecutor().getLogPath());
        xxlJobSpringExecutor.setLogRetentionDays(xxlJobConfig.getExecutor().getLogRetentionDays());
        xxlJobSpringExecutor.setAccessToken(xxlJobConfig.getExecutor().getAccessToken());
        return xxlJobSpringExecutor;
    }
}
