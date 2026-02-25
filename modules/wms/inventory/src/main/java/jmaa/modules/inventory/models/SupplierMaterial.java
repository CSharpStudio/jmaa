package jmaa.modules.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

/**
 * @Author: lix
 * @Create: 2025-12-18
 **/
@Model.Meta(name = "wms.supplier_material", label = "供应商物料", inherit = {"mixin.material"})
@Model.UniqueConstraint(name = "supplier_id_supplier_material_code_unique", fields = {"supplier_id", "supplier_material_code"}, message = "供应商物料编码已存在")
public class SupplierMaterial extends Model {
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field material_id = Field.Many2one("md.material").label("物料").required();
    static Field supplier_material_code = Field.Char().label("供应商物料编码").required();
}
