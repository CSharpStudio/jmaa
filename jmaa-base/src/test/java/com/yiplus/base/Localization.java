package org.jmaa.base;

import org.jmaa.sdk.tools.PathUtils;
import org.jmaa.sdk.tools.StringUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Localization {
    List<File> getFiles(File dir) {
        List<File> files = new ArrayList<>();
        File[] list = dir.listFiles();
        if (list == null) {
            System.out.println("null");
        } else {
            for (File item : list) {
                if (item.isDirectory()) {
                    files.addAll(getFiles(item));
                } else {
                    files.add(item);
                }
            }
        }
        return files;
    }

    static Pattern javaPattern = Pattern.compile("label\\s*=\\s*\"(?<label1>\\S+?)\"|label\\s*\\(\\s*\"(?<label2>\\S+?)\"|help\\s*\\(\\s*\"(?<help>\\S+?)\"|l10n\\s*\\(\\s*\"(?<l10n>\\S+?)\"");
    static Pattern jsPattern = Pattern.compile("[\"'](?<text>[^\"']+?)[\"']\\.t\\s*\\(\\s*\\)");
    static Pattern xmlPattern = Pattern.compile(" label\\s*=\\s*[\"'](?<label>[^\"']+?)[\"']| placeholder\\s*=\\s*[\"'](?<placeholder>[^\"']+?)[\"']|<t>\\s*(?<txt>\\S+?)</t>");


    @Test
    public void test() {
        Pattern pattern = Pattern.compile("[\"'](?<text>[^\"']+?)[\"']\\.t\\s*\\(\\s*\\)");
        String w = "me.dom.find(\".receipt-title\").html(\"收料'单：\".t() + opt.receiptCode);";
        Matcher m = pattern.matcher(w);
        while (m.find()) {
            String txt = m.group("text");
            System.out.println(txt);
        }
        System.out.println(0);
    }

    Set<String> getJava(String content) {
        Set<String> words = new HashSet<>();
        Matcher m = javaPattern.matcher(content);
        while (m.find()) {
            String label1 = m.group("label1");
            String label2 = m.group("label2");
            String help = m.group("help");
            String l10n = m.group("l10n");
            if (StringUtils.isNotBlank(label1)) {
                words.add(label1);
            }
            if (StringUtils.isNotBlank(label2)) {
                words.add(label2);
            }
            if (StringUtils.isNotBlank(help)) {
                words.add(help);
            }
            if (StringUtils.isNotBlank(l10n)) {
                words.add(l10n);
            }
        }
        return words;
    }

    Set<String> getXml(String content) {
        Set<String> words = new HashSet<>();
        Matcher m = xmlPattern.matcher(content);
        while (m.find()) {
            String label = m.group("label");
            if (StringUtils.isNotBlank(label)) {
                words.add(label);
            }
            String placeholder = m.group("placeholder");
            if (StringUtils.isNotBlank(placeholder)) {
                words.add(placeholder);
            }
            String txt = m.group("txt");
            if (StringUtils.isNotBlank(txt)) {
                words.add(txt);
            }
        }
        return words;
    }

    Set<String> getJs(String content) {
        Set<String> words = new HashSet<>();
        Matcher m = jsPattern.matcher(content);
        while (m.find()) {
            String text = m.group("text");
            if (StringUtils.isNotBlank(text)) {
                words.add(text);
            }
        }
        return words;
    }

    @Test
    public void getText() throws IOException {
        File root = new File("").getAbsoluteFile().getParentFile();
        List<File> modules = findPackageInfoDirectories(root);
        for (File module : modules) {
            if (module.isFile()) {
                continue;
            }
            List<File> files = getFiles(module);
            File pkg = new File(module, "package-info.java");
            if (pkg.exists()) {
                Set<String> words = new HashSet<>();
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith(".java")) {
                        byte[] bytes = Files.readAllBytes(Paths.get(file.getPath()));
                        String content = new String(bytes, StandardCharsets.UTF_8);
                        words.addAll(getJava(content));
                    } else if (name.endsWith(".xml")) {
                        byte[] bytes = Files.readAllBytes(Paths.get(file.getPath()));
                        String content = new String(bytes, StandardCharsets.UTF_8);
                        words.addAll(getXml(content));
                    } else if (name.endsWith(".js")) {
                        byte[] bytes = Files.readAllBytes(Paths.get(file.getPath()));
                        String content = new String(bytes, StandardCharsets.UTF_8);
                        words.addAll(getJs(content));
                    }
                }
                String content = words.stream().collect(Collectors.joining("\r\n"));
                Files.write(Paths.get(PathUtils.combine(pkg.getParent(), "lang.txt")), content.getBytes());
                System.out.println(Paths.get(PathUtils.combine(pkg.getParent(), "lang.txt")) + " 更新成功");
            }
        }
    }

    public static List<File> findPackageInfoDirectories(File directory) {
        List<File> result = new ArrayList<>();
        // 检查当前目录是否包含package-info.java
        File packageInfoFile = new File(directory, "package-info.java");
        if (packageInfoFile.exists() && packageInfoFile.isFile()) {
            result.add(directory);
        }
        // 递归处理子目录
        File[] subFiles = directory.listFiles();
        if (subFiles != null) {
            for (File file : subFiles) {
                if (file.isDirectory()) {
                    result.addAll(findPackageInfoDirectories(file));
                }
            }
        }
        return result;
    }
}
