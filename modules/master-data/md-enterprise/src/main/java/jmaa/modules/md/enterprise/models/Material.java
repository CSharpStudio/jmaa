package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "md.material", label = "物料", present = {"code", "name", "spec"}, presentFormat = "{code}", inherit = {"mixin.companies"})
public class Material extends Model {
    static Field name = Field.Char().label("物料名称").required();
    static Field code = Field.Char().label("物料编码").required().unique();
    static Field spec = Field.Char().label("规格");
    static Field name_spec = Field.Char().label("名称规格").compute("getNameSpec").search("searchByNameSpec");
    static Field image = Field.Image().label("图片").attachment(false);
    static Field category = Field.Selection(new Options() {{
        put("raw", "原材料");
        put("semi-finished", "半成品");
        put("finished", "成品");
        put("auxiliary", "生产辅料");
        put("spare", "备品备件");
        put("office", "办公用品");
    }}).label("存货类别").required();
    static Field type_id = Field.Many2one("md.material_type").label("物料分类");
    static Field unit_id = Field.Many2one("md.unit").label("库存单位").required();
    static Field purchase_unit_id = Field.Many2one("md.unit").label("采购单位").required();
    static Field sales_unit_id = Field.Many2one("md.unit").label("销售单位").required();
    static Field abc_type = Field.Selection(new Options() {{
        put("A", "A类");
        put("B", "B类");
        put("C", "C类");
    }}).label("ABC分类").help("将库存物品按品种和占用资金的多少分为ABC类，针对不同等级分别进行管理与控制");
    static Field shelf_life = Field.Integer().label("保质期(天)").min(0).defaultValue(0).help("保质期为0表示不会过期");
    static Field company_ids = Field.Many2many("res.company", "md_material_company", "material_id", "company_id");
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field attribute = Field.Selection(new Options() {{
        put("purchase", "外购");
        put("make", "自制");
        put("outsource", "委外");
    }}).label("物料属性");
    static Field purchasable = Field.Boolean().label("允许采购");
    static Field producible = Field.Boolean().label("允许生产");
    static Field salable = Field.Boolean().label("允许销售");
    static Field outsourcable = Field.Boolean().label("允许委外");
    static Field allow_rtv = Field.Boolean().label("允许退料");
    static Field allow_return = Field.Boolean().label("允许退货");
    static Field accuracy = Field.Integer().label("单位精度").help("单位的小数位数");
    static Field iqc = Field.Boolean().label("来料检验");
    static Field ipqc = Field.Boolean().label("产品检验");
    static Field oqc = Field.Boolean().label("出货检验");

    public String getNameSpec(Records rec) {
        String name = rec.getString("name");
        String specModel = rec.getString("spec");
        if (Utils.isNotEmpty(specModel)) {
            return name + "/" + specModel;
        }
        return name;
    }

    public Criteria searchByNameSpec(Records records, String op, Object value) {
        Criteria criteria = Criteria.binary("name", op, value).or(Criteria.binary("spec", op, value));
        if (value instanceof String) {
            String[] words = ((String) value).split("/");
            if (words.length > 0) {
                criteria.or(Criteria.binary("name", op, words[0]));
            }
        }
        return criteria;
    }

    @ActionMethod
    public Action onAttributeChange(Records record) {
        String attr = record.getString("attribute");
        return Action.attr().setValue("purchasable", "purchase".equals(attr) || "outsource".equals(attr))
            .setValue("producible", "make".equals(attr))
            .setValue("salable", true)
            .setValue("outsourcable", "outsource".equals(attr));
    }
}
