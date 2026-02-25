package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.ValueModel;

/**
 * @author eric
 */
@Model.Meta(name = "mfg.material_issue_report", label = "发料明细查询", table = "mfg_material_issue_line", inherit = "mfg.material_issue_line")
public class MaterialIssueReport extends ValueModel {
    static Field type = Field.Selection().related("issue_id.type");
    static Field related_code = Field.Char().related("issue_id.related_code");
    static Field related_id = Field.Many2oneReference("related_model").related("issue_id.related_id");
    static Field workshop_id = Field.Many2one("md.enterprise_model").related("issue_id.workshop_id");
    static Field warehouse_ids = Field.Many2many("md.warehouse", "", "", "").related("issue_id.warehouse_ids");
    static Field company_id = Field.Many2one("res.company").related("issue_id.company_id");
}
