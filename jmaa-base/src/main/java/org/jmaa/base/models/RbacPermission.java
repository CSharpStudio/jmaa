package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.*;
import org.jmaa.sdk.data.Cursor;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 权限码，包含服务、字段、自定的权限码
 *
 * @author Eric Liang
 */
@Model.Meta(name = "rbac.permission", label = "权限码")
@Model.UniqueConstraint(name = "unique_model_auth_type", fields = {"auth", "model", "type"})
public class RbacPermission extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field auth = Field.Char().label("权限码/字段").required();
    static Field model = Field.Char().label("模型").required();
    static Field role_ids = Field.Many2many("rbac.role", "rbac_role_permission", "permission_id", "role_id").ondelete(DeleteMode.Cascade);
    static Field type = Field.Selection().label("类型").selection(new Options() {{
        put("service", "服务");
        put("field", "字段");
    }}).defaultValue("service").required();
    static Field origin = Field.Selection().label("来源").selection(new Options() {{
        put("base", "内置");
        put("manual", "自建");
    }}).defaultValue("manual").readonly();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);

    /**
     * 更新所有模型的权限码清单
     *
     * @param rec
     * @return
     */
    @SuppressWarnings("AlibabaMethodTooLong")
    @ServiceMethod(label = "更新", ids = false)
    public Object refresh(Records rec) {
        return refreshByModels(rec, null);
    }

    private List<Map<String, Object>> getModelPermission(Environment env, List<String> modelList) {
        Cursor cr = env.getCursor();
        if (modelList == null) {
            String sql = "SELECT id, auth, model, type, name FROM rbac_permission WHERE origin='base'";
            cr.execute(sql);
        } else {
            String sql = "SELECT id, auth, model, type, name FROM rbac_permission WHERE origin='base' AND model IN %s";
            cr.execute(sql, Arrays.asList(modelList));
        }
        return cr.fetchMapAll();
    }

    private void refreshFieldPermission(Records rec, MetaModel meta, Map<String, Map<String, Object>> fields, Set<Map<String, Object>> toUpdate, Map<String, String> toDelete) {
        for (MetaField field : meta.getFields().values()) {
            if (field.isAuth()) {
                String name = field.getName();
                Map<String, Object> map = fields.get(name);
                String label = field.getLabel();
                if (Utils.isEmpty(label)) {
                    label = name;
                }
                Map<String, Object> values = createValues(meta.getName(), label, name, "field");
                if (map == null) {
                    values.put("origin", "base");
                    rec.create(values);
                } else {
                    updateOrDelete(toUpdate, toDelete, map, values);
                }
            }
        }
    }

    private void refreshServicePermission(Records rec, MetaModel m, Map<String, Map<String, Object>> services, Set<Map<String, Object>> toUpdate, Map<String, String> toDelete) {
        Set<String> auths = new HashSet<>();
        for (BaseService svc : m.getService().values()) {
            String auth = svc.getAuth();
            if (Utils.isEmpty(auth)) {
                auth = svc.getName();
            }
            if (auth.equals(svc.getName())) {
                //先记录没使用别名的授权码
                auths.add(auth);
            }
        }
        for (BaseService svc : m.getService().values()) {
            String auth = svc.getAuth();
            if (Constants.ANONYMOUS.equals(auth)) {
                continue;
            }
            if (Utils.isEmpty(auth)) {
                auth = svc.getName();
            }
            if (!auth.equals(svc.getName()) && auths.contains(auth)) {
                //使用别名且已存在的授权码不处理
                continue;
            }
            Map<String, Object> map = services.get(auth);
            String label = svc.getLabel();
            if (Utils.isEmpty(label)) {
                label = auth;
            }
            Map<String, Object> values = createValues(m.getName(), label, auth, "service");
            if (map == null) {
                try {
                    values.put("origin", "base");
                    rec.create(values);
                    auths.add(auth);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                updateOrDelete(toUpdate, toDelete, map, values);
            }
        }
    }

    public Object refreshByModels(Records rec, List<String> modelList) {
        Environment env = rec.getEnv();
        Collection<MetaModel> metaModelCollection = modelList == null ? env.getRegistry().getModels().values() :
            env.getRegistry().getModels().values().stream().filter(item -> modelList.contains(item.getName())).collect(Collectors.toList());
        List<Map<String, Object>> existsData = getModelPermission(env, modelList);
        Map<String, List<Map<String, Object>>> modelPermission = new HashMap<>(256);
        for (Map<String, Object> row : existsData) {
            String model = (String) row.get("model");
            List<Map<String, Object>> permission = modelPermission.get(model);
            if (permission == null) {
                permission = new ArrayList<>();
                modelPermission.put(model, permission);
            }
            permission.add(row);
        }
        //所有权限标记为删除
        Map<String, String> toDelete = new HashMap<>(existsData.size());
        for (Map<String, Object> map : existsData) {
            String key = map.get("model") + ":" + map.get("auth") + ":" + map.get("type");
            toDelete.put(key, map.get("id").toString());
        }
        Set<Map<String, Object>> toUpdate = new HashSet<>(256);
        for (MetaModel m : metaModelCollection) {
            if (m.isAbstract() || Constants.ANONYMOUS.equals(m.getAuthModel())) {
                continue;
            }
            List<Map<String, Object>> data = modelPermission.getOrDefault(m.getName(), new ArrayList<>());
            Map<String, Map<String, Object>> services = new HashMap<>();
            Map<String, Map<String, Object>> fields = new HashMap<>();
            for (Map<String, Object> map : data) {
                String auth = (String) map.get("auth");
                String type = (String) map.get("type");
                if ("service".equals(type)) {
                    services.put(auth, map);
                } else {
                    fields.put(auth, map);
                }
            }
            refreshFieldPermission(rec, m, fields, toUpdate, toDelete);
            refreshServicePermission(rec, m, services, toUpdate, toDelete);
        }
        //更新集合参数
        for (Map<String, Object> values : toUpdate) {
            String id = (String) values.remove("id");
            rec.browse(id).update(values);
        }
        //删除不存在的数据
        rec.browse(toDelete.values()).delete();
        return Action.reload(rec.l10n("更新成功"));
    }

    private void updateOrDelete(Set<Map<String, Object>> toUpdate, Map<String, String> toDelete, Map<String, Object> data, Map<String, Object> updateData) {
        String key = updateData.get("model") + ":" + updateData.get("auth") + ":" + updateData.get("type");
        toDelete.remove(key);
        for (String k : updateData.keySet()) {
            Object oldValue = data.get(k);
            Object newValue = updateData.get(k);
            if (!Utils.equals(oldValue, newValue)) {
                updateData.put("id", data.get("id"));
                toUpdate.add(updateData);
                break;
            }
        }
    }

    private Map<String, Object> createValues(String modelName, String name, String auth, String type) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", name);
        values.put("auth", auth);
        values.put("model", modelName);
        values.put("type", type);
        return values;
    }

    /**
     * 根据当前用户加载指定模型的已授权权限码
     *
     * @param rec
     * @param model
     * @return
     */
    public List<String> loadModelAuth(Records rec, String model) {
        Cursor cr = rec.getEnv().getCursor();
        if (rec.getEnv().isAdmin()) {
            String sql = "SELECT auth FROM rbac_permission p WHERE p.model=%s AND p.type='service' AND p.active=%s";
            cr.execute(sql, Arrays.asList(model, true));
        } else {
            String sql = "SELECT distinct auth FROM rbac_permission p JOIN rbac_role_permission r ON p.id = r.permission_id JOIN rbac_role_user u ON r.role_id=u.role_id WHERE p.model=%s AND u.user_id=%s AND p.type='service' AND p.active=%s";
            cr.execute(sql, Arrays.asList(model, rec.getEnv().getUserId(), true));
        }
        return cr.fetchAll().stream().map(o -> (String) o[0]).collect(Collectors.toList());
    }

    /**
     * 初始化模型需要授权的字段
     *
     * @param rec
     */
    public void initModelAuthFields(Records rec) {
        Cursor cr = rec.getEnv().getCursor();
        String sql = "SELECT auth,model FROM rbac_permission p WHERE p.active=%s AND p.type='field'";
        cr.execute(sql, Arrays.asList(true));
        Map<String, List<String>> map = new HashMap<>();
        for (Object[] row : cr.fetchAll()) {
            String model = (String) row[1];
            List<String> fields = map.get(model);
            if (fields == null) {
                fields = new ArrayList<>();
                map.put(model, fields);
            }
            fields.add((String) row[0]);
        }
        Registry reg = rec.getEnv().getRegistry();
        for (Entry<String, List<String>> row : map.entrySet()) {
            String model = row.getKey();
            if (reg.contains(model)) {
                new MetaModelWrapper(reg.get(model)).setAuthFields(row.getValue());
            }
        }
    }

    /**
     * 加载当前用户没有权限访问的模型的字段
     *
     * @param rec
     * @param model
     * @return
     */
    public Set<String> loadModelDenyFields(Records rec, String model) {
        Set<String> result = rec.getEnv().isAdmin() ? Collections.emptySet() : new HashSet<String>(rec.getEnv().getRegistry().get(model).getAuthFields());
        if (result.size() > 0) {
            Cursor cr = rec.getEnv().getCursor();
            String sql = "SELECT auth FROM rbac_permission p JOIN rbac_role_permission rp ON rp.permission_id=p.id JOIN rbac_role_user ru ON ru.role_id=rp.role_id JOIN rbac_role r ON r.id=ru.role_id WHERE p.type='field' AND ru.user_id=%s AND p.active=%s AND r.active=%s";
            cr.execute(sql, Arrays.asList(rec.getEnv().getUserId(), true, true));
            List<String> allow = cr.fetchAll().stream().map(o -> (String) o[0]).collect(Collectors.toList());
            result.removeAll(allow);
        }
        return result;
    }

    /**
     * 加载有read权限的模型
     *
     * @param rec
     * @return
     */
    public List<String> loadAllowReadModels(Records rec) {
        Cursor cr = rec.getEnv().getCursor();
        String sql = "SELECT DISTINCT model FROM rbac_permission p JOIN rbac_role_permission rp on p.id = rp.permission_id JOIN rbac_role_user u on rp.role_id=u.role_id JOIN rbac_role r on rp.role_id=r.id WHERE u.user_id=%s AND r.active=%s AND p.auth='read' AND p.active=%s";
        cr.execute(sql, Arrays.asList(rec.getEnv().getUserId(), true, true));
        return cr.fetchAll().stream().map(o -> (String) o[0]).collect(Collectors.toList());
    }

    /**
     * 从给定的模型中过滤有读权限的模型
     *
     * @param rec
     * @param models
     * @return
     */
    public Set<String> getAllowReadModels(Records rec, List<String> models) {
        if (rec.getEnv().isAdmin()) {
            return new HashSet<>(models);
        }
        Cursor cr = rec.getEnv().getCursor();
        String sql = "SELECT DISTINCT model FROM rbac_permission p JOIN rbac_role_permission rp on p.id = rp.permission_id JOIN rbac_role_user u on rp.role_id=u.role_id JOIN rbac_role r on rp.role_id=r.id WHERE u.user_id=%s AND r.active=%s AND p.auth='read' AND p.active=%s AND model IN %s";
        cr.execute(sql, Arrays.asList(rec.getEnv().getUserId(), true, true, models));
        return cr.fetchAll().stream().map(o -> (String) o[0]).collect(Collectors.toSet());
    }
}
