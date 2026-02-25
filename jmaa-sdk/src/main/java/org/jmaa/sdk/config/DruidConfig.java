package org.jmaa.sdk.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.jmaa.sdk.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * druid监控控制台配置
 */
@Configuration
public class DruidConfig {

    @Value("${druid.monitor.user:}")
    private String userName;
    @Value("${druid.monitor.password:}")
    private String password;
    @Value("${druid.monitor.allow:}")
    private String allow;
    @Value("${druid.monitor.deny:}")
    private String deny;

    @Bean
    public StatFilter statFilter() {
        StatFilter statFilter = new StatFilter();
        statFilter.setMergeSql(true);
        return statFilter;
    }

    /**
     * web监控的配置
     */
    @Bean
    public ServletRegistrationBean druidServlet() {
        ServletRegistrationBean bean = new ServletRegistrationBean(
            new StatViewServlet(), "/druid/*");
        if(Utils.isNotEmpty(allow)){
            bean.addInitParameter("allow", allow);//白名单
        }
        if(Utils.isNotEmpty(deny)){
            bean.addInitParameter("deny", allow);//黑名单
        }
        if(Utils.isEmpty(userName)){
            userName = "E3D692EE";
        }
        if(Utils.isEmpty(password)){
            password = "A8271BEE89FB";
        }
        bean.addInitParameter("loginUsername", userName);
        bean.addInitParameter("loginPassword", password);
        return bean;
    }

    /**
     * 配置web监控过滤器
     */
    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new WebStatFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.addInitParameter("exclusions", "/web/*,/druid/*,/druid");
        return filterRegistrationBean;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource druidDataSource() {
        return new DruidDataSource();
    }
}
