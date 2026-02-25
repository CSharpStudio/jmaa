package jmaa.modules.demo.models.fields;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

/**
 * 示例
 *
 * @author 梁荣振
 */
@Model.Meta(name = "demo.one2many_details", authModel = "demo.field_type")
public class One2ManyDetails extends Model {
    static Field name = Field.Char().label("名称").index(true).required(true);
    static Field one_id = Field.Many2one("demo.many2one").label("多对一");
    static Field o2m_id = Field.Many2one("demo.one2many").label("一对多字段");
}
