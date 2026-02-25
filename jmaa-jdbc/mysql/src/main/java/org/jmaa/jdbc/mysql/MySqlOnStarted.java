package org.jmaa.jdbc.mysql;

import org.jmaa.sdk.data.Database;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MySqlOnStarted implements ApplicationListener<ApplicationStartedEvent> {
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Database.addSqlDialect("com.mysql.cj.jdbc.Driver", new MySqlDialect());
        Database.addSqlDialect("com.mysql.jdbc.Driver", new MySqlDialect());
    }
}
