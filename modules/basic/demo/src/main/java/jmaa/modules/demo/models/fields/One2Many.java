package jmaa.modules.demo.models.fields;

import org.jmaa.sdk.*;

/**
 * 示例
 *
 * @author 梁荣振
 */
@Model.Meta(name = "demo.one2many", authModel = "demo.field_type")
public class One2Many extends Model {
    static Field name = Field.Char().label("名称").index(true).required(true);
    static Field f_bool = Field.Boolean().label("布尔");
    static Field one_id = Field.Many2one("demo.many2one").label("多对一");
    static Field field_id = Field.Many2one("demo.field_type").label("字段");
    static Field f_m2o_grid_id = Field.Many2one("demo.many2one").label("多对一下拉表格");
    static Field f_datetime = Field.DateTime().label("日期时间").help("日期时间，数据库保存UTC");
    static Field f_date_range = Field.Char().label("日期范围");

    static Field f_m2m_tags = Field.Many2many("demo.many2many", "demo_m2m_tag1", "field_id", "tag_id")
            .label("多对多").help("多对多关联，中间表保存关系，以标签tags形式编辑");
    static Field f_multi_selection = Field.Selection(new Options() {
        {
            put("one", "星期一");
            put("two", "星期二");
            put("three", "星期三");
            put("four", "星期四");
            put("five", "星期五");
        }
    }).label("多选").help("多选字段");
    static Field o2m_ids = Field.One2many("demo.one2many_details", "o2m_id").label("一对多明细").help("一对多关联，在多的表中保存一的外键");

    @ServiceMethod(label = "测试", doc = "用于测试的服务")
    public void test(Records records){

    }
}
