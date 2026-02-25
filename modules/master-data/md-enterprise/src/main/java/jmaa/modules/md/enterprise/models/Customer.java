package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.regex.Pattern;

/**
 * 客户信息模型
 * @author eric
 */
@Model.Meta(name = "md.customer", label = "客户", present = {"code", "name"}, presentFormat = "{code}({name})")
public class Customer extends Model {
    static Field name = Field.Char().label("客户名称").required();
    static Field code = Field.Char().label("客户编码").required().unique();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
    static Field contact = Field.Char().label("联系人");
    static Field phone = Field.Char().label("联系电话");
    static Field address = Field.Char().label("地址");
    static Field zip = Field.Char().label("邮编");
    static Field email = Field.Char().label("E-MAIL");
    static Field name_en = Field.Char().label("客户英文名称");
    static Field chars = Field.Char().label("客户代码").help("只能是数字、字母（区分大小写）");
    static Field remark = Field.Char().label("备注");

    public static final String REGEX = "^[a-zA-Z0-9]+$";

    @Constrains("chars")
    public void checkChars(Records records) {
        for (Records rec : records) {
            String chars = rec.getString("chars");
            if (Utils.isNotEmpty(chars)) {
                boolean matches = Pattern.compile(REGEX).matcher(chars).matches();
                if (!matches) {
                    throw new ValidationException(rec.l10n("客户代码只能是数字和英文字母"));
                }
            }
        }
    }
}
