package jmaa.modules.md.account.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "md.bank", label = "银行")
public class Bank extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field full_name = Field.Char().label("全称");
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
}
