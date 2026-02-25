package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(inherit = "print.template")
public class PrintTemplate extends Model {
    static Field category = Field.Selection().addSelection(new Options() {{
        put("material_label", "物料标签");
        put("product_label", "成品标签");
        put("batch_label", "批次标签");
        put("package_label", "包装标签");
    }});
}
