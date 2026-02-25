package org.jmaa.base.models;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.tools.PathUtils;
import org.jmaa.sdk.tools.ThrowableUtils;
import org.jmaa.sdk.util.KvMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * 语言翻译
 *
 * @author Eric Liang
 */
@Model.Meta(name = "res.lang", label = "语系", order = "active desc,name", authModel = "res.localization", present = {"name", "code"}, presentFormat = "{name} - {code}")
public class ResLanguage extends Model {
    private Logger logger = LoggerFactory.getLogger(ResLanguage.class);
    static Field name = Field.Char().label("名称").required().unique();
    static Field code = Field.Char().label("地区代码").help("用于用户设置本地化").required().unique();
    static Field iso_code = Field.Char().label("ISO代码").help("ISO代码用于翻译").required().unique();
    static Field active = Field.Boolean().label("是否生效").defaultValue(false);
    static Field date_format = Field.Char().label("日期格式").required().defaultValue("yyyy/MM/dd");
    static Field time_format = Field.Char().label("时间格式").required().defaultValue("HH:mm:ss");
    static Field week_start = Field.Selection(new Options() {{
        put("1", "星期一");
        put("2", "星期二");
        put("3", "星期三");
        put("4", "星期四");
        put("5", "星期五");
        put("6", "星期六");
        put("7", "星期天");
    }}).label("一周的第一天").required().defaultValue("7");
    static Field version = Field.Integer().label("版本").defaultValue(0).required();

    /**
     * 从lang.txt文件中加载要翻译的文本
     */
    public void loadLang(Records rec, String packageName) {
        Set<String> words = readLang(packageName);
        doAddWords(rec, words);
    }

    int doAddWords(Records rec, Set<String> words) {
        if (words.size() > 0) {
            Environment env = rec.getEnv();
            Records lang = env.findRef("base.lang_zh_cn");
            Cursor cr = env.getCursor();
            cr.execute("SELECT name FROM res_localization WHERE lang_id=%s", Collections.singletonList(lang.getId()));
            List<String> exists = cr.fetchAll().stream().map(row -> (String) row[0]).collect(Collectors.toList());
            words.removeAll(exists);
            List<Map<String, Object>> toCreate = new ArrayList<>();
            for (String name : words) {
                try {
                    env.get("res.localization").create(new KvMap().set("name", name).set("lang_id", lang.getId()));
                } catch (Exception exception) {
                    logger.warn(String.format("添加多语言[%s]失败:", name) + ThrowableUtils.getCause(exception).getMessage());
                }
            }
        }
        return words.size();
    }

    Set<String> readLang(String packageName) {
        Set<String> result = new HashSet<>();
        String path = PathUtils.combine(packageName.replaceAll("\\.", "/"), "lang.txt");
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        if (loader != null) {
            InputStream input = loader.getResourceAsStream(path);
            if (input != null) {
                try (InputStreamReader reader = new InputStreamReader(input); BufferedReader bufferedReader = new BufferedReader(reader)) {
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        result.add(line);
                    }
                } catch (Exception ex) {
                    logger.warn(path + ":" + ex.getMessage(), ex);
                }
            }
        }
        return result;
    }

    /**
     * 获取安装的语言
     */
    @ServiceMethod(auth = Constants.ANONYMOUS, label = "获取安装的言语")
    public Map<String, String> getInstalled(Records rec) {
        return rec.find(Criteria.equal("active", true)).read(Arrays.asList("code", "name")).stream().collect(Collectors.toMap(k -> (String) k.get("code"), v -> (String) v.get("name")));
    }

    /**
     * 获取语言版本
     */
    @ServiceMethod(auth = Constants.ANONYMOUS)
    public int getVersion(Records record) {
        Criteria criteria = Criteria.equal("code", record.getEnv().getLang());
        return record.find(criteria).getInteger(version);
    }

    /**
     * 获取语言翻译
     */
    @ServiceMethod(auth = Constants.ANONYMOUS)
    public Map<String, String> getLocalization(Records record) {
        Map<String, String> result = new HashMap<>();
        String sql = "SELECT a.name,a.value FROM res_localization a JOIN res_lang b ON a.lang_id=b.id WHERE b.code=%s AND a.value is not null AND a.name != a.value";
        Cursor cr = record.getEnv().getCursor();
        cr.execute(sql, Collections.singletonList(record.getEnv().getLang()));
        for (Object[] row : cr.fetchAll()) {
            result.put((String) row[0], (String) row[1]);
        }
        return result;
    }

    /**
     * 添加要翻译的文本
     */
    @ServiceMethod(auth = Constants.ANONYMOUS)
    public int addWords(Records record, Collection<String> words) {
        return doAddWords(record, new HashSet<>(words));
    }
}
