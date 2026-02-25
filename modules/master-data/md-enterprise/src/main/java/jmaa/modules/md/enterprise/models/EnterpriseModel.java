package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.tools.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author eric
 */
@Model.UniqueConstraint(name = "unique_name_parent", fields = {"name", "parent_id"}, message = "同一层级名称不能重复")
@Model.UniqueConstraint(name = "unique_code_parent", fields = {"code", "parent_id"}, message = "同一层级编码不能重复")
@Model.Meta(name = "md.enterprise_model", label = "企业模型", order = "seq,id", present = {"name", "code"}, presentFormat = "{name}({code})")
public class EnterpriseModel extends Model {
    static Field name = Field.Char().label("名称").required(true);
    static Field code = Field.Char().label("编码").required(true);
    static Field parent_id = Field.Many2one("md.enterprise_model").label("父模型");
    static Field parent_path = Field.Char().label("父模型路径").length(2000);
    static Field child_ids = Field.One2many("md.enterprise_model", "parent_id").label("子模型");
    static Field tpl_id = Field.Many2one("md.enterprise_tpl").label("企业层级").required(true);
    static Field seq = Field.Integer().label("显示顺序").defaultValue(16);
    static Field company_id = Field.Many2one("res.company").label("组织");

    @Model.ServiceMethod(auth = "read", doc = "根据父模型获取企业层级")
    public Map<String, String> getTemplate(Records rec, String parentId) {
        if (StringUtils.isNotEmpty(parentId)) {
            Records parent = rec.browse(parentId);
            return parent.getRec("tpl_id").getRec("child_ids")
                    .stream().collect(Collectors.toMap(r -> r.getId(), r -> (String) r.get("present")));
        }
        return rec.getEnv().get("md.enterprise_tpl").find(Criteria.equal("type", "group").or(Criteria.equal("type", "company")))
                .filter(r -> Utils.isEmpty(r.getRec("parent_id").getId()))
                .stream().collect(Collectors.toMap(r -> r.getId(), r -> (String) r.get("present")));
    }

    @OnSaved("company_id")
    public void onCompanyIdSave(Records records) {
        for (Records record : records) {
            Records tpl = record.getRec("tpl_id");
            if (tpl.any() && "company".equals(tpl.getString("type"))) {
                List<String> ids = getChildrenId(record);
                record.browse(ids).set("company_id", record.getRec("company_id"));
            }
        }
    }

    @OnSaved("parent_id")
    public void onParentIdSave(Records records) {
        for (Records record : records) {
            Records parent = record.getRec("parent_id");
            if (parent.any()) {
                String path = parent.getString("parent_path") + "/" + parent.getId();
                record.set("parent_path", path);
            }
        }
    }

    List<String> getChildrenId(Records records) {
        List<String> ids = new ArrayList<>();
        for (Records record : records) {
            Records children = record.find(Criteria.equal("parent_id", record.getId()));
            if (children.any()) {
                ids.addAll(Arrays.asList(children.getIds()));
                ids.addAll(getChildrenId(children));
            }
        }
        return ids;
    }

    @ActionMethod
    public Action onTplChange(Records record) {
        AttrAction action = new AttrAction();
        Records tpl = record.getRec("tpl_id");
        if (tpl.any() && "group".equals(tpl.getString("type"))) {
            action.setVisible("company_id", false);
        } else {
            action.setVisible("company_id", true);
        }
        if (tpl.any() && "company".equals(tpl.getString("type"))) {
            action.setReadonly("company_id", false);
        } else {
            action.setReadonly("company_id", true);
        }
        Records parent = record.getRec("parent_id");
        if (parent.any() && parent.getRec("company_id").any()) {
            action.setValue("company_id", parent.getRec("company_id"));
        }
        return action;
    }
}
