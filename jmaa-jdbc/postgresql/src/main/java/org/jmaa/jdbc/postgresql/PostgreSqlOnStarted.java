package org.jmaa.jdbc.postgresql;

import org.jmaa.sdk.data.Database;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlOnStarted implements ApplicationListener<ApplicationStartedEvent> {
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Database.addSqlDialect("org.postgresql.Driver", new PgSqlDialect());
    }
}
