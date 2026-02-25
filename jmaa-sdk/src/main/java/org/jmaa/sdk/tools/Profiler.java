package org.jmaa.sdk.tools;

import org.jmaa.sdk.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 性能跟踪器
 */
public class Profiler {
    private boolean isTracing;
    private int level;

    private List<Monitor> monitors = new ArrayList<>();
    static ThreadLocal<Profiler> profiler = ThreadLocal.withInitial(() -> new Profiler());

    /**
     * 设置是否启用跟踪
     *
     * @param isTracing
     */
    public static void setTracing(boolean isTracing) {
        profiler.get().isTracing = isTracing;
    }

    /**
     * 获取是否启用跟踪
     *
     * @return
     */
    public static boolean isTracing() {
        return profiler.get().isTracing;
    }

    /**
     * 清理跟踪器数据
     */
    public static void clear() {
        profiler.remove();
    }

    /**
     * 开始监控
     *
     * @param message
     * @return
     */
    public static AutoCloseable monitor(Supplier<String> message) {
        Profiler p = profiler.get();
        if (p.isTracing) {
            Monitor m = new Monitor(message);
            p.monitors.add(m);
            return m;
        }
        return new Empty();
    }

    /**
     * 获取追踪结果
     *
     * @return
     */
    public static String getTrace() {
        return profiler.get().monitors.stream().map(m -> m.toString()).collect(Collectors.joining("\n"));
    }

    static class Empty implements AutoCloseable {
        @Override
        public void close() {
        }
    }

    static class Monitor implements AutoCloseable {
        Supplier<String> message;
        long elapsed;
        int level;

        public Monitor(Supplier<String> message) {
            this.message = message;
            elapsed = System.currentTimeMillis();
            level = profiler.get().level++;
        }

        @Override
        public void close() {
            elapsed = System.currentTimeMillis() - elapsed;
            profiler.get().level--;
        }

        @Override
        public String toString() {
            String indent = StringUtils.leftPad("", level, "-");
            return Utils.format("%s>%s ms elapsed:%s.", indent, elapsed, message.get());
        }
    }
}
