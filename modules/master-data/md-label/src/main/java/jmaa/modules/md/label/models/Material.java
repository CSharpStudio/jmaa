package jmaa.modules.md.label.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author eric
 */
@Model.Meta(inherit = "md.material")
public class Material extends Model {
    static Field stock_rule = Field.Selection(new Options() {{
        put("sn", "序列号管控");
        put("lot", "批次管控");
        put("num", "数量管控");
    }}).label("库存规则").defaultValue("sn").required();
    static Field sn_coding_id = Field.Many2one("code.coding").label("序列号编码").defaultValue(Default.method("getDefaultSnCoding"));
    static Field lot_coding_id = Field.Many2one("code.coding").label("批次编码").defaultValue(Default.method("getDefaultLotCoding"));
    static Field print_tpl_id = Field.Many2one("print.template").label("标签模板");
    static Field min_packages = Field.Float().label("标签数量").help("每张标签的标准数量");
    @ActionMethod
    public Action onUnitChange(Records rec) {
        return Action.attr().setAttr("min_packages", "data-decimals", rec.getRec("unit_id").getInteger("accuracy"));
    }

    /**
     * 条码规则添加默认值
     */
    public String getDefaultSnCoding(Records rec) {
        return rec.getEnv().get("code.coding").find(Criteria.equal("code", "SYS-SN")).getId();
    }

    /**
     * 批次条码规则添加默认值
     */
    public String getDefaultLotCoding(Records rec) {
        return rec.getEnv().get("code.coding").find(Criteria.equal("code", "SYS-LOT")).getId();
    }
}
