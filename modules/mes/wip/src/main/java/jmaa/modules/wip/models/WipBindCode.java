package jmaa.modules.wip.models;

import org.jmaa.sdk.BoolState;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "wip.bind_code", label = "采集条码关联", description = "生产过程中与产品关联的条码", logAccess = BoolState.False)
@Model.Service(remove = "@all")
public class WipBindCode extends Model {
    static Field product_id = Field.Many2one("wip.production").label("产品");
    static Field code = Field.Char().label("条码").index();
    static Field code_type = Field.Selection(Selection.related("md.work_process_step", "code_type")).label("条码类型");
}
