package jmaa.modules.demo.models.fields;

import java.util.HashMap;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

/**
 * 示例
 *
 * @author 梁荣振
 */
@Model.Meta(name = "demo.field_type", authModel = "demo.field_type")
public class FieldType extends Model {
    static Field f_char = Field.Char().label("字符").help("字符串类型字段，默认长度240").index(true).required(true);
    static Field f_bool = Field.Boolean().label("布尔").help("布尔字段").required(true).auth();
    static Field f_bool_nullable = Field.Boolean().label("布尔(可空)").help("可空布尔字段：空值/真/假");
    static Field f_date = Field.Date().label("日期").help("日期字段").required(true).auth();
    static Field f_datetime = Field.DateTime().label("日期时间").help("日期时间，数据库保存UTC");
    static Field f_time = Field.Char().label("时间(时分秒)");
    static Field f_time_mm = Field.Char().label("时间(时分)");
    static Field f_date_range = Field.Char().label("日期范围");
    static Field f_time_range = Field.Char().label("时间范围");
    static Field f_count_down = Field.Char().label("倒计时");
    static Field f_calendar = Field.Char().label("日历");
    static Field f_float = Field.Float().label("小数").help("小数字段，默认两位小数").auth();
    static Field f_html = Field.Html().label("HTML").help("HTML字段").auth();
    static Field f_int = Field.Integer().label("整数").help("整数字段");
    static Field f_text = Field.Text().label("大文本").help("大文本字段");
    static Field f_image = Field.Image().label("图片").help("以附件形式保存，通过URL下载").attachment(true);
    static Field f_image_db = Field.Image().label("图片(DB)").help("以二进制格式保存于数据库，通过Base64加载，只可上传单张图片").attachment(false);
    static Field f_file = Field.Binary().label("附件").help("可配置上传单个或多个文件，以附件形式保存").attachment(true);
    static Field f_file_db = Field.Binary().label("附件(DB)").help("以二进制格式保存于数据库，通过Base64加载，只可上传单个文件，不建议用这种方式保存文件").attachment(false);
    static Field f_big_file = Field.Binary().label("大文件");

    static Field f_radio = Field.Selection(new Options() {
        {
            put("1", "星期一");
            put("2", "星期二");
            put("3", "星期三");
            put("4", "星期四");
            put("5", "星期五");
        }
    }).label("单选").help("单选字段").useCatalog();

    static Field f_priority = Field.Selection(new Options() {
        {
            put("1", "不推荐");
            put("2", "一般");
            put("3", "不错");
            put("4", "很棒");
            put("5", "极力推荐");
        }
    }).label("评分").help("评分字段");

    static Field f_selection = Field.Selection(new Options() {
        {
            put("1", "星期一");
            put("2", "星期二");
            put("3", "星期三");
            put("4", "星期四");
            put("5", "星期五");
        }
    }).label("单选").help("单选字段");
    static Field f_multi_selection = Field.Selection(new Options() {
        {
            put("one", "星期一");
            put("two", "星期二");
            put("three", "星期三");
            put("four", "星期四");
            put("five", "星期五");
        }
    }).label("多选").help("多选字段");
    static Field f_check_list = Field.Selection(new Options() {
        {
            put("one", "星期一");
            put("two", "星期二");
            put("three", "星期三");
            put("four", "星期四");
            put("five", "星期五");
        }
    }).label("多选").help("多选字段");

    static Field f_m2o_id = Field.Many2one("demo.many2one").label("多对一").help("多对一关联");
    static Field f_m2o_grid_id = Field.Many2one("demo.many2one").label("多对一下拉表格");
    static Field f_o2m_ids = Field.One2many("demo.one2many", "field_id").label("一对多").help("一对多关联，在多的表中保存一的外键");
    static Field f_m2m_tags = Field.Many2many("demo.many2many", "demo_m2m_tag", "field_id", "tag_id")
            .label("多对多").help("多对多关联，中间表保存关系，以标签tags形式编辑");
    static Field f_m2m_ids = Field.Many2many("demo.many2many", "demo_m2m", "field_id", "m2m_id")
            .label("多对多").help("多对多关联，中间表保存关系，以列表形式编辑");
    static Field f_compute = Field.Char().label("计算字段")
            .compute(Callable.script("r->r.get('f_char')+'('+r.get('f_bool')+')'"));

    static Field parent_id = Field.Many2one("demo.field_type").label("父模型");
    static Field child_ids = Field.One2many("demo.field_type", "parent_id").label("子模型");

    @ActionMethod
    public Object getDataAction(Records records) {
        boolean checked = records.getBoolean(f_bool);
        AttrAction attrs = Action.attr();
        attrs.setValue("f_selection", checked ? "2" : "3")
                .setReadonly("f_selection", checked)
                .setVisible("f_priority", checked)
                .setAttr("f_text", "required", checked);
        return attrs;
    }

    @ActionMethod
    public Action getDecimals(Records record) {
        int decimals = record.getInteger(f_int);
        AttrAction attrs = Action.attr();
        attrs.setAttr("f_float", "data-decimals", decimals);
        attrs.setAttr("f_float", "min", 2);
        attrs.setAttr("f_selection", "options", new HashMap<String, String>(16) {
            {
                put("1", "星期1");
                put("2", "星期2");
                put("3", "星期3");
            }
        });
        return attrs;
    }
}
