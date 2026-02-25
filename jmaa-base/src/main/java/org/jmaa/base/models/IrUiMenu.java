package org.jmaa.base.models;

import java.util.*;
import java.util.stream.Collectors;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.tools.StringUtils;

/**
 * 菜单
 *
 * @author Eric Liang
 */
@Model.Meta(name = "ir.ui.menu", label = "菜单", inherit = "ir.ui.menu.base")
public class IrUiMenu extends Model {
    static Field role_ids = Field.Many2many("rbac.role", "rbac_role_menu", "menu_id", "role_id");
    static Field child_ids = Field.One2many("ir.ui.menu", "parent_id").label("子菜单");
    static Field parent_id = Field.Many2one("ir.ui.menu").label("父菜单").index().ondelete(DeleteMode.Restrict);
    static Field click = Field.Char().label("点击事件");
    static Field target = Field.Selection(new Options() {{
        put("tab", "页签");
        put("blank", "新窗口");
    }}).label("目标").defaultValue("tab");
    static Field top_menu = Field.Boolean().label("顶部菜单").help("作为根目录或者根菜单显示在顶部菜单栏").defaultValue(false);

    public String getUrl(Records rec) {
        String[] v = ((String) rec.get("view")).split("\\|");
        String url = "/{tenant}/view#model=" + rec.get("model") + "&views=" + v[0];
        if (v.length > 1) {
            url += "&key=" + v[1];
        }
        return url;
    }

    public boolean isLink(Records rec) {
        String url = rec.getString("url");
        String click = rec.getString("click");
        return StringUtils.isNotEmpty(url) || StringUtils.isNotEmpty(click);
    }

    public Map<String, Object> readMenu(Records rec) {
        KvMap menu = new KvMap();
        for (String field : Arrays.asList("name", "icon", "click")) {
            Object value = rec.get(field);
            if (Utils.isNotEmpty(value)) {
                menu.put(field, value);
            }
        }
        String target = rec.getString("target");
        if (!"tab".equals(target)) {
            menu.put("target", target);
        }
        boolean top = rec.getBoolean("top_menu");
        if (top) {
            menu.put("top_menu", top);
        }
        return menu;
    }
}
