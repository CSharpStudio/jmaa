package jmaa.modules.code.rule.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.MetaModel;

import java.util.List;

/**
 * @author eric
 */
@Model.Meta(name = "code.part", label = "编码明细", order = "seq", authModel = "code.coding")

public class CodePart extends Model {
    static Field coding_id = Field.Many2one("code.coding").ondelete(DeleteMode.Cascade).label("编码规则");
    static Field seq = Field.Integer().label("序号").required(true);
    static Field content = Field.Char().label("配置").length(1000);
    static Field code = Field.Char().label("编码");
    static Field name = Field.Char().label("名称");
    static Field part_code = Field.Object().label("编码段算法").required(true).compute("getPart", "setPart");
    static Field description = Field.Char().label("描述").compute("computeDescription");

    public List<String> getPart(Records rec) {
        return Utils.asList(rec.getString("code"), rec.getString("name"));
    }

    public void setPart(Records rec) {
        Object part = rec.get("part_code");
        if (part instanceof String) {
            String code = (String) part;
            rec.set("code", code);
            if (!code.startsWith("$")) {
                rec.set("name", rec.getEnv().getRegistry().get(code).getLabel());
            }
        } else {
            List<String> list = (List<String>) rec.get("part_code");
            if (Utils.isNotEmpty(list)) {
                rec.set("code", list.get(0));
                rec.set("name", list.get(1));
            }
        }
    }

    public String computeDescription(Records record) {
        String code = record.getString("code");
        if (Utils.isNotEmpty(code) && !code.startsWith("$")) {
            MetaModel meta = record.getEnv().getRegistry().get(code);
            String description = meta.getDescription();
            if (Utils.isEmpty(description)) {
                return meta.getLabel();
            }
            return description;
        }
        return null;
    }

    @ActionMethod
    public Action onPartChange(Records rec) {
        AttrAction action = new AttrAction();
        String description = "";
        List<String> array = (List<String>) rec.get("part_code");
        if (Utils.isNotEmpty(array)) {
            String code = array.get(0);
            if (!code.startsWith("$")) {
                MetaModel meta = rec.getEnv().getRegistry().get(code);
                description = meta.getDescription();
                if (Utils.isEmpty(description)) {
                    description = meta.getLabel();
                }
            }
        }
        action.setValue("description", description);
        return action;
    }
}
