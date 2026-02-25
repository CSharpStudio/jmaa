package org.jmaa.tenant;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.tenants.*;
import org.jmaa.sdk.tools.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupEvent {
    private final static Logger logger = LoggerFactory.getLogger(StartupEvent.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        TenantService.onStartup(() -> {
            Tenant root = TenantService.get("root");
            Registry registry = root.getRegistry();
            if (registry.contains("tenant.info")) {
                try (Cursor cr = root.getDatabase().openCursor()) {
                    Records rec = registry.get("tenant.info").browse(new Environment(registry, cr, Constants.SYSTEM_USER), ArrayUtils.EMPTY_STRING_ARRAY, () -> ArrayUtils.EMPTY_STRING_ARRAY);
                    rec.call("loadTenants");
                } catch (Exception exc) {
                    logger.error("租户服务启动失败", exc);
                }
            }
        });
    }
}
