package org.jmaa.jdbc.clickhouse;

import org.jmaa.sdk.data.Database;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ClickhouseOnStarted implements ApplicationListener<ApplicationStartedEvent> {
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Database.addSqlDialect("com.clickhouse.jdbc.ClickHouseDriver", new ClickhouseDialect());
    }
}
