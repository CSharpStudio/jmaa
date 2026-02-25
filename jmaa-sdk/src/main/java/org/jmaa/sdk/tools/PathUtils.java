package org.jmaa.sdk.tools;

import org.jmaa.sdk.Utils;

/**
 * @author Eric Liang
 */
public class PathUtils {
    /**
     * 路径合并处理
     */
    public static String combine(String... paths) {
        String path = "";
        for (String p : paths) {
            String f = p.replaceAll("\\\\", "/");
            if (path.length() == 0) {
                path = f;
            } else {
                boolean end = path.endsWith("/");
                boolean start = f.startsWith("/");
                if (end) {
                    if (!start) {
                        path += f;
                    } else {
                        path += f.substring(1);
                    }
                } else {
                    if (start) {
                        path += f;
                    } else {
                        path += "/" + f;
                    }
                }
            }
        }
        return path;
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String path) {
        if (Utils.isNotEmpty(path)) {
            int lastIndex = path.lastIndexOf('.');
            if (lastIndex > 0) {
                return path.substring(lastIndex + 1);
            }
        }
        return null;
    }



    /**
     * 获取文件名
     */
    public static String getFileName(String filename) {
        if (Utils.isEmpty(filename)) {
            return filename;
        }
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex == 0 && dotIndex == filename.length() - 1) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }
}
