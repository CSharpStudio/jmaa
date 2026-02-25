package jmaa.modules.md.account.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "md.currency", label = "币别")
public class Currency extends Model {
    static Field code = Field.Char().label("代码").required();
    static Field name = Field.Char().label("名称").required();
    static Field symbol = Field.Char().label("符号");
    static Field accuracy = Field.Integer().label("金额精度").defaultValue(2).min(0);
    static Field price_accuracy = Field.Integer().label("单价精度").defaultValue(2).min(0);
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
}
