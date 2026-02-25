package org.jmaa.base.models;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.apache.commons.collections4.SetUtils;

import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.tools.StringUtils;

/**
 * 角色
 *
 * @author Eric Liang
 */
@Model.Meta(name = "rbac.role", label = "角色")
public class RbacRole extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field user_ids = Field.Many2many("rbac.user", "rbac_role_user", "role_id", "user_id");
    static Field permission_ids = Field.Many2many("rbac.permission", "rbac_role_permission", "role_id", "permission_id");
    static Field menu_ids = Field.Many2many("ir.ui.menu", "rbac_role_menu", "role_id", "menu_id");
    static Field is_admin = Field.Boolean().label("是否管理员").readonly().defaultValue(false);
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);

    /**
     * 更新：非管理员不能修改is_admin
     */
    @Override
    public void update(Records records, Map<String, Object> values) {
        if (values.containsKey("is_admin")) {
            if (records.getEnv().getRegistry().isLoaded() && !records.getEnv().isAdmin()) {
                values.remove("is_admin");
            }
        }
        records.callSuper(RbacRole.class, "update", values);
    }

    /**
     * 创建: 非管理员不能设置is_admin
     */
    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valueList) {
        for (Map<String, Object> values : valueList) {
            if (values.containsKey("is_admin")) {
                if (rec.getEnv().getRegistry().isLoaded() && !rec.getEnv().isAdmin()) {
                    values.put("is_admin", false);
                }
            }
        }
        return (Records) rec.callSuper(RbacRole.class, "createBatch", valueList);
    }

    /**
     * 设置是否管理员
     */
    @Model.ServiceMethod(label = "设置管理员")
    public Object setAdmin(Records rec, boolean isAdmin) {
        if (!rec.getEnv().isAdmin()) {
            throw new ValidationException(rec.l10n("当前用户非管理员角色，不能修改是否管理员"));
        }
        if ("admin".equals(rec.get("name")) && !isAdmin) {
            throw new ValidationException(rec.l10n("用户admin无法取消管理员"));
        }
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("UPDATE rbac_role SET is_admin=%s WHERE id in %s",
            Arrays.asList(isAdmin, Arrays.asList(rec.getIds())));
        return Action.reload(rec.l10n("设置成功"));
    }

    /**
     * 保存权限：模块权限、菜单权限
     */
    @ServiceMethod(label = "保存权限")
    public void savePermission(Records rec, List<String> permissions, List<String> menus) {
        Records p = rec.getEnv().get("rbac.permission", permissions);
        rec.set("permission_ids", p);

        Records menu = rec.getEnv().get("ir.ui.menu", menus);
        rec.set("menu_ids", menu);
    }

    Map<String, KvMap> toAppMap(Records menus, boolean isAdmin, Set<String> roleIds) {
        Map<String, KvMap> apps = new LinkedHashMap<>();
        for (Records menu : menus) {
            if (!menu.getRec("parent_id").any()) {
                KvMap data = new KvMap();
                data.put("name", menu.get("name"));
                data.put("models", new ArrayList<>());
                apps.put(menu.getId(), data);
                KvMap links = new KvMap();
                if (addMenu(links, menu, isAdmin, roleIds)) {
                    data.put("menus", links);
                }
            }
        }
        return apps;
    }

    public void linkMenusToApp(Records records, Records menus, Set<String> models, Map<String, KvMap> apps, Records permissions,
                               boolean isAdmin, Set<String> roleIds, Map<String, KvMap> loaded) {
        for (Records menu : menus) {
            String model = menu.getString("model");
            if (StringUtils.isNotEmpty(model) && models.remove(model)) {
                KvMap kv = getModelPermission(menus, menu.getString("name"), model, permissions, isAdmin, roleIds);
                if (kv != null) {
                    Records root = getRoot(menu);
                    KvMap data = apps.get(root.getId());
                    List<KvMap> appModels = (List<KvMap>) data.get("models");
                    appModels.add(kv);
                    loaded.put(model, kv);
                }
            }
        }
        //处理PDA
        Records pdaMenus = menus.getEnv().get("ir.ui.menu.mobile").find(new Criteria());
        for (Records menu : pdaMenus) {
            String model = menu.getString("model");
            if (StringUtils.isNotEmpty(model) && models.remove(model)) {
                KvMap kv = getModelPermission(menus, menu.getString("name"), model, permissions, isAdmin, roleIds);
                if (kv != null) {
                    Records root = getRoot(menu);
                    KvMap pdaRoot = apps.get("@pda");
                    if (pdaRoot == null) {
                        pdaRoot = new KvMap();
                        pdaRoot.put("name", "PDA");
                        pdaRoot.put("groups", new KvMap());
                        apps.put("@pda", pdaRoot);
                    }
                    KvMap groups = (KvMap) pdaRoot.get("groups");
                    KvMap group = (KvMap) groups.get(root.getId());
                    if (group == null) {
                        group = new KvMap();
                        group.put("name", root.get("name"));
                        group.put("models", new ArrayList<>());
                        groups.put(root.getId(), group);
                    }
                    List<KvMap> appModels = (List<KvMap>) group.get("models");
                    appModels.add(kv);
                    loaded.put(model, kv);
                }
            }
        }
    }

    /**
     * 加载所有模型的权限码清单。
     * 1.绑定菜单的模型将按根菜单分组；
     * 2.没绑定模型，但有url或者click的菜单，按树型菜单结构加载；
     * 3.模型指定authModel的，会生成到authModel的related列表；
     */
    @ServiceMethod(label = "加载模型权限清单", ids = false, auth = "savePermission")
    @SuppressWarnings({"unchecked", "AlibabaMethodTooLong"})
    public List<Object> loadPermissionList(Records rec) {
        Environment env = rec.getEnv();
        Records menus = env.get("ir.ui.menu").find(new Criteria());
        Records permissions = env.get("rbac.permission").find(Criteria.equal("active", true));
        boolean isAdmin = rec.getEnv().isAdmin();
        Set<String> roleIds = SetUtils.hashSet(rec.getEnv().getUser().getRec("role_ids").getIds());
        Set<String> models = new HashSet<>();
        for (Records p : permissions) {
            models.add(p.getString("model"));
        }
        //计算顶层app
        Map<String, KvMap> apps = toAppMap(menus, isAdmin, roleIds);
        //记录已处理的功能
        Map<String, KvMap> loaded = new HashMap<>();
        // 关联菜单的模型
        linkMenusToApp(rec, menus, models, apps, permissions, isAdmin, roleIds, loaded);

        List<KvMap> nomenu = new ArrayList<>();
        Records ms = env.get("ir.model").find(Criteria.in("model", models));
        Registry reg = env.getRegistry();
        // 没关联菜单的模型且没指定authModel
        for (String model : new ArrayList<String>(models)) {
            if (!reg.contains(model) || StringUtils.isEmpty(reg.get(model).getAuthModel())) {
                String label = (String) ms.filter(p -> model.equals(p.get("model"))).get("name");
                if (StringUtils.isEmpty(label)) {
                    label = model;
                }
                KvMap kv = getModelPermission(rec, label, model, permissions, isAdmin, roleIds);
                if (kv != null) {
                    nomenu.add(kv);
                    loaded.put(model, kv);
                    models.remove(model);
                }
            }
        }
        // 指定authModel的模型
        for (String model : models) {
            String auth = reg.get(model).getAuthModel();
            if (Constants.ANONYMOUS.equals(auth)) {
                continue;
            }
            String label = (String) ms.filter(p -> model.equals(p.get("model"))).get("name");
            KvMap kv = getModelPermission(rec, label, model, permissions, isAdmin, roleIds);
            if (kv != null) {
                String authModel = getAuthModel(env.getRegistry(), model);
                KvMap root = loaded.get(authModel);
                if (root != null) {
                    List<KvMap> related = (List<KvMap>) root.get("related");
                    if (related == null) {
                        related = new ArrayList<>();
                        root.put("related", related);
                    }
                    related.add(kv);
                } else {
                    nomenu.add(kv);
                }
            }
        }
        //转换成返回格式
        List<Object> result = new ArrayList<>();
        for (Entry<String, KvMap> app : apps.entrySet()) {
            KvMap data = app.getValue();
            List<Object> appModels = (List<Object>) data.get("models");
            KvMap groupModels = (KvMap) data.get("groups");
            KvMap map = new KvMap().set("app", data.get("name"));
            if (Utils.isNotEmpty(appModels)) {
                map.set("models", appModels);
            }
            if (Utils.isNotEmpty(groupModels)) {
                map.set("groups", groupModels.values());
            }
            if (data.containsKey("menus")) {
                map.put("menus", data.get("menus"));
            }
            if (Utils.isNotEmpty(appModels) || Utils.isNotEmpty(groupModels)) {
                result.add(map);
            }
        }
        if (nomenu.size() > 0) {
            result.add(new KvMap().set("app", "-").set("models", nomenu));
        }

        return result;
    }

    boolean has(Set<String> source, String[] target) {
        for (String t : target) {
            if (source.contains(t)) {
                return true;
            }
        }
        return false;
    }

    public KvMap getModelPermission(Records records, String menu, String model, Records permissions, boolean isAdmin, Set<String> roleIds) {
        KvMap kv = new KvMap();
        List<KvMap> serviceList = new ArrayList<>();
        List<Object> fieldList = new ArrayList<>();
        for (Records r : permissions.filter(p -> model.equals(p.get("model")))) {
            String[] rs = ((Records) r.get("role_ids")).getIds();
            if (isAdmin || has(roleIds, rs)) {
                KvMap p = new KvMap();
                p.set("name", r.get("name"));
                p.set("auth", r.get("auth"));
                p.set("id", r.getId());
                p.set("role_ids", rs);
                if ("service".equals(r.get("type"))) {
                    serviceList.add(p);
                } else {
                    fieldList.add(p);
                }
            }
        }
        String ordered = "read,create,update,delete";
        Comparator<KvMap> comparator = (x, y) -> {
            int px = ordered.indexOf((String) x.get("auth"));
            int py = ordered.indexOf((String) y.get("auth"));
            if (px > -1 && py > -1) {
                return px - py;
            }
            if (px > -1) {
                return -1;
            }
            if (py > -1) {
                return 1;
            }
            return StringUtils.compare((String) x.get("auth"), (String) y.get("auth"));
        };
        if (isAdmin || serviceList.size() > 0 || fieldList.size() > 0) {
            kv.set("services", serviceList.stream().sorted(comparator).collect(Collectors.toList()));
            kv.set("fields", fieldList);
            kv.set("menu", menu);
            kv.set("model", model);
            return kv;
        }
        return null;
    }

    String getAuthModel(Registry reg, String model) {
        String auth = reg.get(model).getAuthModel();
        if (StringUtils.isEmpty(auth) || Utils.equals(model, auth)) {
            return model;
        }
        return getAuthModel(reg, auth);
    }

    @SuppressWarnings("unchecked")
    boolean addMenu(KvMap menus, Records rec, boolean isAdmin, Set<String> roleIds) {
        boolean result = false;
        String url = (String) rec.get("url");
        String click = (String) rec.get("click");
        String model = (String) rec.get("model");
        KvMap menu = new KvMap().set("name", rec.get("name"));
        boolean customMenu = StringUtils.isEmpty(model) && (StringUtils.isNotEmpty(url) || StringUtils.isNotEmpty(click));
        if (customMenu) {
            String[] rs = ((Records) rec.get("role_ids")).getIds();
            if (isAdmin || has(roleIds, rs)) {
                menu.put("role_ids", rs);
                result = true;
            }
        }
        Records child = (Records) rec.get("child_ids");
        for (Records c : child) {
            if (addMenu(menus, c, isAdmin, roleIds)) {
                List<String> sub = (List<String>) menu.get("sub");
                if (sub == null) {
                    sub = new ArrayList<String>();
                    menu.put("sub", sub);
                }
                sub.add(c.getId());
                result = true;
            }
        }
        if (result) {
            if (!((Records) rec.get("parent_id")).any()) {
                menus.put("sub", menu.get("sub"));
            } else {
                menus.put(rec.getId(), menu);
            }
        }
        return result;
    }

    public Records getRoot(Records menu) {
        Records parent = (Records) menu.get("parent_id");
        if (parent.any()) {
            return getRoot(parent);
        }
        return menu;
    }
}
