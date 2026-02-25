package org.jmaa.jdbc.oracle;

import org.jmaa.sdk.data.Database;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class OracleOnStarted implements ApplicationListener<ApplicationStartedEvent> {
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Database.addSqlDialect("oracle.jdbc.OracleDriver", new OracleDialect());
    }
}
