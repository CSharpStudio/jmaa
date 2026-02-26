package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.ValueModel;

@Model.Meta(name = "mfg.product_storage_notice_dialog", label = "成品入库通知对话框", authModel = "mfg.product_storage_notice")
public class ProductStorageNoticeDialog extends ValueModel {
    static Field material_id = Field.Many2one("md.material").store(false).label("物料编码").readonly();
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field auto_confirm = Field.Boolean().label("自动确认");
    static Field abc_type = Field.Selection().label("ABC分类").related("material_id.abc_type");
}
