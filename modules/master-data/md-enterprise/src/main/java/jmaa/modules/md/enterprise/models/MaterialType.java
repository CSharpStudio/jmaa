package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;

import java.util.ArrayList;
import java.util.List;

@Model.Meta(name = "md.material_type", label = "物料分类", order = "code")
public class MaterialType extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field display = Field.Char().label("显示").compute("computeDisplay");
    static Field parent_id = Field.Many2one("md.material_type").label("父级分类");
    static Field child_ids = Field.One2many("md.material_type", "parent_id").label("子级分类");
    static Field complete_name = Field.Char().label("完整名称").compute("computeCompleteName");
    static Field material_ids = Field.One2many("md.material", "type_id").label("物料");

    public String computeDisplay(Records record) {
        String code = record.getString("code");
        String name = record.getString("name");
        return String.format("%s(%s)", code, name);
    }

    public String computeCompleteName(Records record) {
        Records parent = record.getRec("parent_id");
        String name = record.getString("name");
        if (parent.any()) {
            name = parent.get("complete_name") + "/" + name;
        }
        return name;
    }

    public List<Object[]> getPresent(Records records) {
        List<Object[]> result = new ArrayList<>();
        for (Records record : records) {
            result.add(new Object[]{record.getId(), record.get("complete_name")});
        }
        return result;
    }
}
