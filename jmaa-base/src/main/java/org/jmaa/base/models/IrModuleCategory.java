package org.jmaa.base.models;

import org.jmaa.sdk.*;

/**
 * 模块分类
 *
 * @author
 */
@Model.Meta(name = "ir.module.category", label = "模块分类", authModel = "ir.module")
public class IrModuleCategory extends Model {
	static Field name = Field.Char().label("名称").help("模块的名称").unique();
	static Field module_ids = Field.One2many("ir.module", "category_id").label("模块集合");
}
