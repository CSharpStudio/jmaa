package org.jmaa.sdk.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.exceptions.UserException;

/**
 * @author Eric Liang
 */
public class ThrowableUtils {
    /**
     * 获取根本原因:首次引发异常的原因
     *
     * @param throwable
     * @return
     */
    public static Throwable getCause(Throwable throwable) {
        if (throwable instanceof UserException) {
            return throwable;
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            return getCause(cause);
        }
        return throwable;
    }

    /**
     * 获取根本原因的主要信息
     *
     * @param throwable
     * @return
     */
    public static String getMessage(Throwable throwable) {
        Throwable t = getCause(throwable);
        if (t instanceof ValidationException) {
            return t.getMessage();
        }
        String msg = t.getMessage();
        StackTraceElement[] stacks = t.getStackTrace();
        String at = ObjectUtils.isNotEmpty(stacks) ? t.getStackTrace()[0].toString() : "";
        if (StringUtils.isNotEmpty(msg)) {
            return String.format("%s:%s\r\n\tat %s", t.getClass().getName(), msg, at);
        }
        return t.getClass().getName() + " at " + at;
    }

    /**
     * 获取调试信息，堆栈信息与Throwable.printStackTrace堆栈信息相反
     *
     * @param error
     * @return
     */
    public static String getDebug(Throwable error) {
        List<String> message = new ArrayList<>();
        Throwable t = error;
        while (t != null) {
            String msg = t.getMessage();
            StackTraceElement[] stacks = t.getStackTrace();
            String at = ObjectUtils.isNotEmpty(stacks) ? stacks[0].toString() : "";
            if (StringUtils.isNotEmpty(msg)) {
                message.add(String.format("%s:%s\r\n\tat %s", t.getClass().getName(), msg, at));
            } else {
                message.add(t.getClass().getName() + " at " + at);
            }
            t = t.getCause();
        }
        Collections.reverse(message);
        return StringUtils.join(message, "\r\nCause:");
    }
}
