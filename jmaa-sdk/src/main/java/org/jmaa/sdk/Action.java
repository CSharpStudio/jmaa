package org.jmaa.sdk;

import org.jmaa.sdk.action.*;
import org.jmaa.sdk.core.Environment;

/**
 * 动作，从服务端返回给前端的动作
 *
 * @author Eric Liang
 */
public interface Action {

    String getAction();

    static ReloadAction reload(String message) {
        return new ReloadAction(message);
    }

    static ReloadAction success() {
        Environment env = Environment.envs.get();
        return new ReloadAction(env.l10n("操作成功"));
    }

    static DialogAction dialog(String model, String type) {
        return new DialogAction(model, type);
    }

    static JsAction js(String script) {
        return new JsAction(script);
    }

    static ViewAction view(String model, String type) {
        return new ViewAction(model, type);
    }

    static ServiceAction service(String model, String service) {
        return new ServiceAction(model, service);
    }

    static AttrAction attr() {
        return new AttrAction();
    }
}
