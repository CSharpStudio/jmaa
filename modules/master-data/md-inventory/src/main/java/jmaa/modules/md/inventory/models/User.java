package jmaa.modules.md.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

/**
 * @author eric
 */
@Model.Meta(inherit = { "rbac.user" })
public class User extends Model{
    static Field warehouse_ids = Field.Many2many("md.warehouse", "md_warehouse_user", "user_id", "wh_id").label("仓库");
}
