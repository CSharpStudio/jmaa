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

/**
 * @author : eric
 **/
@Model.Meta(name = "ir.ui.menu.mobile", label = "移动端菜单", inherit = "ir.ui.menu.base")
public class IrUiMenuMobile extends Model {
    static Field child_ids = Field.One2many("ir.ui.menu.mobile", "parent_id").label("子菜单");
    static Field parent_id = Field.Many2one("ir.ui.menu.mobile").label("父菜单").index().ondelete(DeleteMode.Restrict);
}
