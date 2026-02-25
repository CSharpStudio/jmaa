package org.jmaa.jdbc.h2;

import org.jmaa.sdk.data.Database;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class H2OnStarted implements ApplicationListener<ApplicationStartedEvent> {
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Database.addSqlDialect("org.h2.Driver", new H2Dialect());
    }
}
