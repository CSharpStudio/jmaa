package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.util.KvMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model.Meta(name = "ir.ui.menu.base", label = "菜单基类", order = "sequence, id")
public class IrUiMenuBase extends AbstractModel {
    static Field name = Field.Char().label("菜单").required().translate();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field sequence = Field.Integer().defaultValue(16).label("显示顺序");
    static Field icon = Field.Char().label("图标");
    static Field url = Field.Char().label("地址");
    static Field model = Field.Char().label("模型");
    static Field view = Field.Char().label("视图");
    static Field model_name = Field.Char().label("模型名称").compute("computeModelName");

    public String computeModelName(Records record) {
        String model = record.getString("model");
        if (record.getEnv().getRegistry().contains(model)) {
            return record.getEnv().getRegistry().get(model).getLabel();
        }
        return model;
    }

    @Model.ActionMethod
    public Action onModelChange(Records record) {
        AttrAction action = new AttrAction();
        action.setValue("model_name", computeModelName(record));
        return action;
    }

    /**
     * 按用户权限加载菜单
     *
     * @param rec
     * @return
     */
    @SuppressWarnings("unchecked")
    @Model.ServiceMethod(ids = false, doc = "加载菜单", auth = Constants.ANONYMOUS)
    public Map<String, Object> loadMenu(Records rec) {
        KvMap menus = new KvMap();
        List<String> root = new ArrayList<>();
        boolean isAdmin = rec.getEnv().isAdmin();
        List<String> authModels = (List<String>) rec.getEnv().get("rbac.permission").call("loadAllowReadModels");
        List<String> authMenus = loadAuthMenus(rec);
        rec = rec.find(Criteria.equal("active", true));
        for (Records r : rec) {
            Records parent = r.getRec("parent_id");
            if (!parent.any()) {
                addMenu(menus, r, isAdmin, authModels, authMenus);
                String id = r.getId();
                if (menus.containsKey(id)) {
                    root.add(r.getId());
                }
            }
        }
        menus.put("root", root);
        return menus;
    }

    /**
     * 加载已授权的菜单id列表
     *
     * @param rec
     * @return
     */
    public List<String> loadAuthMenus(Records rec) {
        Cursor cr = rec.getEnv().getCursor();
        String sql = "SELECT DISTINCT m.menu_id FROM rbac_role_menu m"
            + " JOIN rbac_role_user u on m.role_id=u.role_id"
            + " JOIN rbac_role r on m.role_id=r.id"
            + " WHERE u.user_id=%s AND r.active=%s";
        cr.execute(sql, Arrays.asList(rec.getEnv().getUserId(), true));
        return cr.fetchAll().stream().map(o -> (String) o[0]).collect(Collectors.toList());
    }

    @Model.Constrains({"model", "url"})
    public void checkModelAndUrl(Records rec) {
        for (Records r : rec) {
            String model = r.getString("model");
            String url = r.getString("url");
            if (StringUtils.isNotEmpty(model) && StringUtils.isNotEmpty(url)) {
                throw new ValidationException(rec.l10n("模型与地址不能同时赋值"));
            }
        }
    }

    public Map<String, Object> readMenu(Records rec) {
        KvMap menu = new KvMap();
        for (String field : Arrays.asList("name", "icon")) {
            Object value = rec.get(field);
            if (Utils.isNotEmpty(value)) {
                menu.put(field, value);
            }
        }
        return menu;
    }

    public boolean isLink(Records rec) {
        String url = rec.getString("url");
        return StringUtils.isNotEmpty(url);
    }

    @SuppressWarnings("unchecked")
    boolean addMenu(KvMap menus, Records rec, boolean isAdmin, List<String> authModels, List<String> authMenus) {
        boolean result = false;
        String url = rec.getString("url");
        String model = rec.getString("model");
        if (StringUtils.isNotEmpty(model)) {
            if (isAdmin || authModels.contains(model)) {
                url = getUrl(rec);
                result = true;
            } else {
                return false;
            }
        } else if (isLink(rec)) {
            if (isAdmin || authMenus.contains(rec.getId())) {
                result = true;
            } else {
                return false;
            }
        }

        Map<String, Object> menu = readMenu(rec);
        if (StringUtils.isNotEmpty(url)) {
            url = url.replace("{tenant}", rec.getEnv().getRegistry().getTenant().getKey());
            menu.put("url", url);
        }

        Records child = rec.getRec("child_ids").filter(m -> Boolean.TRUE.equals(m.get("active")));
        for (Records c : child) {
            if (addMenu(menus, c, isAdmin, authModels, authMenus)) {
                List<String> sub = (List<String>) menu.get("sub");
                if (sub == null) {
                    sub = new ArrayList<>();
                    menu.put("sub", sub);
                }
                sub.add(c.getId());
                result = true;
            }
        }
        if (result) {
            menus.put(rec.getId(), menu);
        }
        return result;
    }

    public String getUrl(Records rec) {
        String[] v = rec.getString("view").split("\\|");
        String url = "/{tenant}/m/view#model=" + rec.get("model") + "&view=" + v[0];
        if (v.length > 1) {
            url += "&key=" + v[1];
        }
        return url;
    }
}
