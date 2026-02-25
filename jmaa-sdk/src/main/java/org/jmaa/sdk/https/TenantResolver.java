package org.jmaa.sdk.https;

import javax.servlet.http.HttpServletRequest;

import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tenants.TenantService;
import org.jmaa.sdk.tools.StringUtils;

import org.springframework.stereotype.Component;

/**
 * @author Eric Liang
 */
@Component
public class TenantResolver {
    public Tenant resolve(HttpServletRequest request) {
        String key = resolveTenantKey(request);
        return TenantService.find(key);
    }

    public String resolveTenantKey(HttpServletRequest request) {
        String[] parts = request.getRequestURI().split("/");
        String key = null;
        for (String part : parts) {
            if (StringUtils.isNoneBlank(part)) {
                key = part;
                break;
            }
        }
        return key;
    }
}
