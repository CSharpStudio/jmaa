package org.jmaa.sdk.tenants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.MissingException;
import org.jmaa.sdk.tools.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 租户服务
 *
 * @author Eric Liang
 */
public class TenantService {
    private final static Logger logger = LoggerFactory.getLogger(TenantService.class);
    HashMap<String, Tenant> tenants = new HashMap<>();
    List<Runnable> onStartup = new ArrayList<>();
    static String startTime = Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
    static TenantService instance;

    public static String getStartTime() {
        return startTime;
    }

    public static TenantService getInstance() {
        if (instance == null) {
            instance = new TenantService();
        }
        return instance;
    }

    public static void setInstance(TenantService svc) {
        instance = svc;
    }

    public static void register(Tenant tenant) {
        getInstance().tenants.put(tenant.getKey(), tenant);
    }

    public static void onStartup(Runnable runnable) {
        getInstance().onStartup.add(runnable);
    }

    public static void startup() {
        for (Runnable run : getInstance().onStartup) {
            try {
                run.run();
            } catch (Exception ex) {
                logger.error("onStartup执行失败", ex);
            }
        }
        get("root").getRegistry();
    }

    public static Tenant get(String tenant) {
        Tenant result = getInstance().tenants.get(tenant);
        if (result == null) {
            throw new MissingException(String.format("租户[%s]不存在", tenant));
        }
        return result;
    }

    public static Tenant find(String tenant) {
        return getInstance().tenants.get(tenant);
    }
}
