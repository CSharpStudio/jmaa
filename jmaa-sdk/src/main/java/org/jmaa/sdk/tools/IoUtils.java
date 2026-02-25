package org.jmaa.sdk.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.exceptions.ValueException;
import org.springframework.util.ClassUtils;

/**
 * @author Eric Liang
 */
public class IoUtils {
    /**
     * 输入流转换成字节数组
     */
    public static byte[] toByteArray(InputStream input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
        } catch (IOException e) {
            throw new PlatformException("InputStream转byte[]失败", e);
        }
        return output.toByteArray();
    }

    /**
     * 读取文件字节
     */
    public static byte[] getFileBytes(File file) {
        try {
            return toByteArray(new FileInputStream(file));
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    /**
     * 字节数组转换成文件
     */
    public static File bytesToFile(byte[] bytes, String outPath, String fileName) {
        File dir = new File(outPath);
        //判断文件目录是否存在
        if (!dir.exists() && dir.isDirectory()) {
            dir.mkdirs();
        }
        File file = new File(outPath + File.separator + fileName);
        try (FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(bytes);
        } catch (Exception e) {
            throw new PlatformException("生成文件失败", e);
        }
        return file;
    }

    /**
     * 找资源返回输入流
     * 1. 如果properties文件有配置rootPath，尝试读取磁盘文件返回
     * 2. 如果第一步没找到文件，通过ClassLoader读取资源
     */
    public static InputStream getResourceAsStream(String path) {
        String rootPath = (String) SpringUtils.getProperty("rootPath");
        if (StringUtils.isNotEmpty(rootPath)) {
            for (String root : rootPath.split(",")) {
                if (path.indexOf("modules") > 0) {
                    File modules = new File(PathUtils.combine(root, "modules"));
                    if (!modules.exists()) {
                        modules = new File(root);
                    }
                    if (modules.exists()) {
                        File[] dirs = modules.listFiles();
                        if (dirs != null) {
                            for (File dir : dirs) {
                                InputStream is = find(dir, path);
                                if (is != null) {
                                    return is;
                                }
                            }
                        }
                    }
                } else {
                    InputStream is = find(new File(PathUtils.combine(root, "jmaa-web")), path);
                    if (is != null) {
                        return is;
                    }
                    is = find(new File(PathUtils.combine(root, "jmaa-base")), path);
                    if (is != null) {
                        return is;
                    }
                }
            }
        }
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        if (loader != null) {
            return loader.getResourceAsStream(path);
        }
        return null;
    }

    static InputStream find(File folder, String path) {
        if (folder.exists()) {
            String fileName = PathUtils.combine(folder.getPath(), "src/main/java", path);
            File file = new File(fileName);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            File[] files = folder.listFiles();
            if (files != null) {
                for (File dir : files) {
                    fileName = PathUtils.combine(dir.getPath(), "src/main/java", path);
                    file = new File(fileName);
                    if (file.exists()) {
                        try {
                            return new FileInputStream(file);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 使用MD5算法计算checksum
     */
    public static String computeChecksum(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(bytes);
            // 将处理后的字节转成 16 进制，得到最终 32 个字符
            StringBuilder sb = new StringBuilder();
            for (byte b : md5) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从输入写到输出
     */
    public static long stream(InputStream input, OutputStream output) throws IOException {
        try (
            ReadableByteChannel inputChannel = Channels.newChannel(input);
            WritableByteChannel outputChannel = Channels.newChannel(output)
        ) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(10240);
            long size = 0;

            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                size += outputChannel.write(buffer);
                buffer.clear();
            }

            return size;
        }
    }

    public static String toString(InputStream inputStream) {
        return toString(inputStream, StandardCharsets.UTF_8);
    }

    public static String toString(InputStream inputStream, Charset charset) {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            int result = bis.read();
            while (result != -1) {
                buf.write((byte) result);
                result = bis.read();
            }
            return buf.toString(charset.name());
        } catch (Exception e) {
            throw new ValueException("读取字符串失败", e);
        }
    }

    public static String toString(BufferedReader reader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
