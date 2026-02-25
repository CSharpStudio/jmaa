package org.jmaa.base.models;
import org.jmaa.sdk.*;

/**
 * 模块依赖
 *
 * @author
*/
@Model.Meta(name="ir.module.dependency", label="模块依赖", authModel = "ir.module")

public class IrModuleDependency extends Model{
	static Field name = Field.Char().label("名称").help("模块的名称");
	static Field module_id = Field.Many2one("ir.module").ondelete(DeleteMode.Cascade);
}
