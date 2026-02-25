package jmaa.modules.purchase.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "purchase.supplier_material", label = "供应商物料", inherit = "mixin.material")
public class SupplierMaterial extends Model {
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field material_supplier_code = Field.Char().label("供应商物料编码");
    static Field material_supplier_name = Field.Char().label("供应商物料名称");
}
