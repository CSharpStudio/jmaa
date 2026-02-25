package org.jmaa.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.jmaa.sdk.exceptions.ModelException;
import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tenants.TenantService;

import org.jmaa.sdk.tools.PathUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

/**
 * web启动事件
 *
 * @author Eric Liang
 */
@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${spring.profiles.active}")
    private String activeEnv;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String env = this.activeEnv == null ? "dev" : this.activeEnv;
        String propertiesFileName = "dbcp-" + env + ".properties";
        Properties properties = find(PathUtils.combine("config", propertiesFileName));
        if(properties == null){
            properties = find(propertiesFileName);
        }
        if(properties == null){
            try {
                properties = PropertiesLoaderUtils.loadAllProperties(propertiesFileName);
            } catch (Exception e) {
                throw new ModelException("load dbcp error", e);
            }
        }
        TenantService.register(new Tenant("root", "Root", properties));
    }

    static Properties find(String propertiesFileName) {
        String absolutePath = System.getProperty("user.dir");
        String filePath = PathUtils.combine(absolutePath, propertiesFileName);
        File file = new File(filePath);
        if (file.exists()) {
            System.out.println("dbcp from file:" + filePath);
            try (InputStream in = new FileInputStream(file)) {
                Properties properties = new Properties();
                properties.load(in);
                return properties;
            } catch (Exception e) {
                throw new ModelException("load dbcp error", e);
            }
        }
        return null;
    }
}
