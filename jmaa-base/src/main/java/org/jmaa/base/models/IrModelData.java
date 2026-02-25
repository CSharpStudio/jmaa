package org.jmaa.base.models;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jmaa.base.utils.ImportErrorHandle;
import org.jmaa.sdk.*;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.core.Loader;
import org.apache.commons.collections4.SetUtils;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型数据
 *
 * @author Eric Liang
 */
@Model.Meta(name = "ir.model.data", label = "模型数据", order = "module, model, name", authModel = "ir.model")
@Model.Service(remove = "@all")
public class IrModelData extends Model {
    private Logger logger = LoggerFactory.getLogger(IrModelData.class);

    static Field name = Field.Char().label("扩展ID").required();
    static Field model = Field.Char().label("模型名称").required();
    static Field module = Field.Char();
    static Field res_id = Field.Char();

    static Field no_update = Field.Boolean().defaultValue(false);

    /**
     * 查找引用的模型数据
     */
    public Records findRef(Records rec, String xmlId) {
        String[] parts = xmlId.split("\\.", 2);
        String sql = "SELECT id, model, res_id FROM ir_model_data WHERE module=%s AND name=%s";
        Cursor cr = rec.getEnv().getCursor();
        cr.execute(sql, Arrays.asList(parts));
        Object[] row = cr.fetchOne();
        if (row.length == 0) {
            return null;
        }
        return rec.getEnv().get((String) row[1]).browse((String) row[2]);
    }

    /**
     * 查找模型数据
     */
    public Map<String, Object> findData(Records rec, String xmlId) {
        String[] parts = xmlId.split("\\.", 2);
        String sql = "SELECT id, model, res_id, no_update FROM ir_model_data WHERE module=%s AND name=%s";
        Cursor cr = rec.getEnv().getCursor();
        cr.execute(sql, Arrays.asList(parts));
        return cr.fetchMapOne();
    }

    /**
     * 删除数据
     */
    public void removeData(Records rec, Collection<String> modules) {
        Environment env = rec.getEnv();
        Records moduleData = rec.find(Criteria.in("module", modules), 0, null, "id desc");
        Map<String, List<String>> recordsItems = new HashMap<>(16);
        List<String> modelIds = new ArrayList<>();
        List<String> fieldIds = new ArrayList<>();
        List<String> selectionIds = new ArrayList<>();
        List<String> constraintIds = new ArrayList<>();
        for (Records r : moduleData) {
            String model = r.getString("model");
            String resId = r.getString("res_id");
            if ("ir.model".equals(model)) {
                modelIds.add(resId);
            } else if ("ir.model.field".equals(model)) {
                fieldIds.add(resId);
            } else if ("ir.model.field.selection".equals(model)) {
                selectionIds.add(resId);
            } else if ("ir.model.constraint".equals(model)) {
                constraintIds.add(resId);
            } else {
                List<String> lst = recordsItems.get(model);
                if (lst == null) {
                    lst = new ArrayList<>();
                    recordsItems.put(model, lst);
                }
                lst.add(resId);
            }
        }
        List<String> undeletableIds = new ArrayList<>();

        // 先删除非模型相关的记录
        for (Entry<String, List<String>> entry : recordsItems.entrySet()) {
            if (env.getRegistry().contains(entry.getKey())) {
                deleteData(env.get(entry.getKey(), entry.getValue()), moduleData, undeletableIds);
            }
        }

        deleteData(env.get("ir.model.field", fieldIds), moduleData, undeletableIds);
        deleteData(env.get("ir.model", modelIds), moduleData, undeletableIds);

        if (undeletableIds.size() > 0) {
            logger.info("ir.model.data could not be deleted in ({})", undeletableIds);
        }

        Set<String> dataIds = SetUtils.hashSet(moduleData.getIds());
        for (Records data : rec.browse(undeletableIds).exists()) {
            try {
                Records record = env.get(data.getString("model"), data.getString("res_id"));
                if (record.exists().any()) {
                    dataIds.remove(data.getId());
                }
            } catch (Exception exc) {
                // 表可能已经不存在
                logger.warn("执行exists失败", exc);
            }
        }
        rec.browse(dataIds).withContext("uninstall", true).delete();
    }

    public void deleteData(Records rec, Records moduleData, List<String> undeletableIds) {
        Records refData = moduleData.find(Criteria.equal("model", rec.getMeta().getName()).and(Criteria.in("res_id", rec.getIds())));
        Set<String> refIds = SetUtils.hashSet(refData.getIds());
        refIds.removeAll(Arrays.asList(moduleData.getIds()));
        Set<String> externalIds = moduleData.browse(refIds).stream().map(r -> r.getString("res_id")).collect(Collectors.toSet());
        for (String id : rec.getIds()) {
            if (!externalIds.contains(id)) {
                try {
                    rec.browse(id).withContext("uninstall", true).delete();
                } catch (Exception exc) {
                    undeletableIds.addAll(Arrays.asList(refData.getIds()));
                }
            }
        }
    }

    /**
     * 保存模块数据.
     * 读取{@link  Manifest#data()} 和 {@link  Manifest#demo()} }声明的资源文件，导入保存文件中定义的数据
     */
    public void loadData(Records rec, String packageName) {
        Manifest manifest = ManifestUtils.getManifest(packageName);
        String[] files = manifest.data();
        //date数据在前面加载，demo在后面加载
        Boolean loadDemo = ObjectUtils.toBoolean(SpringUtils.getProperty("loadDemo"));
        if (loadDemo != null && loadDemo) {
            //扩容前长度
            int length = files.length;
            String[] demoFiles = manifest.demo();
            if (demoFiles != null && demoFiles.length > 0) {
                files = Arrays.copyOf(files, files.length + demoFiles.length);
                System.arraycopy(demoFiles, 0, files, length, demoFiles.length);
            }
        }
        try {
            Records xmlLoader = rec.getEnv().get("ir.model.data.xml.loader");
            Records csvLoader = rec.getEnv().get("ir.model.data.csv.loader");
            String moduleId = rec.getEnv().get("ir.module").find(Criteria.equal("name", manifest.name())).getId();
            Map<String, Object> context = new HashMap<>();
            context.put("moduleId", moduleId);
            context.put("moduleName", manifest.name());
            context.put("packageName", packageName);
            for (String file : files) {
                if (StringUtils.isNoneBlank(file)) {
                    String path = PathUtils.combine(packageName.replaceAll("\\.", "/"), file);
                    InputStream input = IoUtils.getResourceAsStream(path);
                    if (input != null) {
                        String pathLower = path.toLowerCase();
                        try {
                            if (pathLower.endsWith(".xml")) {
                                xmlLoader.call("loadData", input, context, (ImportErrorHandle) (m, e) -> {
                                    logger.warn(pathLower + ":" + m, e);
                                    String content = String.format("包：%s;文件：%s;错误：%s", packageName, pathLower, ThrowableUtils.getDebug(e));
                                    Loader.addModuleLog(rec.getEnv().getDatabase(), "加载数据失败", content, Loader.LogLevel.Error);
                                });
                            } else if (pathLower.endsWith(".csv")) {
                                String csvFileName = pathLower.substring(pathLower.lastIndexOf("/") + 1);
                                String modelName = csvFileName.substring(0, csvFileName.length() - 4);
                                csvLoader.call("loadData", input, manifest.name(), modelName, (ImportErrorHandle) (m, e) -> {
                                    logger.warn(pathLower + ":" + m, e);
                                    String content = String.format("包：%s;文件：%s;错误：%s", packageName, pathLower, ThrowableUtils.getDebug(e));
                                    Loader.addModuleLog(rec.getEnv().getDatabase(), "加载数据失败", content, Loader.LogLevel.Error);
                                });
                            }
                        } catch (Exception ex) {
                            logger.warn(pathLower + ":" + ex.getMessage(), ex);
                            String content = String.format("包：%s;文件：%s;错误：%s", packageName, pathLower, ThrowableUtils.getDebug(ex));
                            Loader.addModuleLog(rec.getEnv().getDatabase(), "加载数据失败", content, Loader.LogLevel.Error);
                        }
                    } else {
                        logger.warn(packageName + "找不到文件:" + file);
                        String content = String.format("包：%s;文件：%s", packageName, file);
                        Loader.addModuleLog(rec.getEnv().getDatabase(), "加载数据找不到文件", content, Loader.LogLevel.Error);
                    }
                }
            }
            rec.flush();
        } catch (Exception e) {
            String content = String.format("包：%s;错误：%s", packageName, ThrowableUtils.getDebug(e));
            Loader.addModuleLog(rec.getEnv().getDatabase(), "加载数据失败", content, Loader.LogLevel.Error);
        }
    }
}
