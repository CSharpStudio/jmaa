package jmaa.modules.demo.models.fields;

import org.jmaa.sdk.*;

/**
 * 示例
 *
 * @author 梁荣振
 */
@Model.Meta(name = "demo.many2many")
public class Many2Many extends Model {
    static Field name = Field.Char().label("名称").index(true).required(true);
    static Field field_ids = Field.Many2many("demo.field_type", "demo_m2m", "m2m_id", "field_id").label("字段");
}
