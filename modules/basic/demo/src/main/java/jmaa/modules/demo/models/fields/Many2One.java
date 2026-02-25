package jmaa.modules.demo.models.fields;

import org.jmaa.sdk.*;

/**
 * 示例
 *
 * @author 梁荣振
 */
@Model.Meta(name = "demo.many2one")
public class Many2One extends Model {
    static Field name = Field.Char().label("名称").index(true).required(true);
    static Field code = Field.Char().label("编码").index(true).required(true);
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
}
