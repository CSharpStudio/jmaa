package org.jmaa.sdk.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 回调
 */
public class Callbacks {
    List<Runnable> func = new ArrayList<>();
    KvMap data = new KvMap();

    public void add(Runnable fn) {
        func.add(fn);
    }

    public KvMap getData() {
        return data;
    }

    public void run() {
        for (Runnable f : func) {
            f.run();
        }
        clear();
    }

    public void clear() {
        func.clear();
        data.clear();
    }
}
