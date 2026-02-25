package jmaa.modules.inventory.models.report;

import org.jmaa.sdk.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author 梁荣振
 */
@Model.Meta(name = "stock.onhand_report_base", label = "物料库存查询")
public class OnhandReportBase extends AbstractModel {
    static Field material_id = Field.Many2one("md.material").label("物料编码");
    static Field material_code = Field.Char().label("物料编码");
    static Field ok_qty = Field.Float().label("合格数");
    static Field ng_qty = Field.Float().label("不合格数");
    static Field allot_qty = Field.Float().label("分配数");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库");
    static Field warehouse = Field.Char().label("仓库");
    static Field total_qty = Field.Float().label("总数量");
    static Field available_qty = Field.Float().label("可用数");
    static Field frozen_qty = Field.Float().label("冻结数");
    static Field company_id = Field.Many2many("res.company", "", "", "").label("组织").lookup("searchCompany");
    static Field company = Field.Char().label("组织");
    static Field unit = Field.Char().label("单位");
    static Field category = Field.Selection().label("基本分类").related("material_id.category");
    static Field abc_type = Field.Selection().label("ABC分类").related("material_id.abc_type");
    static Field name_spec = Field.Char().label("名称规格");

    public Criteria searchCompany(Records rec) {
        return Criteria.in("id", rec.getEnv().getUser().getRec("company_ids").getIds());
    }

    /**
     * 查询当前账号有权限的仓库
     *
     * @param rec
     * @return
     */
    @Model.ServiceMethod(auth = "read")
    public Map<String, Object> searchWarehouse(Records rec,
                                               @Doc(doc = "公司") List<String> companyIds,
                                               @Doc(doc = "条件") List<Object> criteria,
                                               @Doc(doc = "行数") Integer limit,
                                               @Doc(doc = "偏移量") Integer offset) {
        Utils.requireNotNull(criteria, "参数criteria不能为空");
        Criteria filter = Criteria.parse(criteria);
        if (Utils.isEmpty(companyIds)) {
            filter.and(Criteria.in("company_id", rec.getEnv().getCompanies().getIds()));
        } else {
            filter.and(Criteria.in("company_id", companyIds));
        }
        if (limit == null) {
            limit = 10;
        }
        return rec.getEnv().get("md.warehouse").searchLimit(Arrays.asList("present"), filter, offset, limit, "");
    }
}
