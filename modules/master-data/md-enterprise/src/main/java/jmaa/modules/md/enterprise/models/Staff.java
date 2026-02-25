package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "md.staff", label = "员工", present = {"name", "code"}, presentFormat = "{name}({code})")
public class Staff extends Model {
    static Field code = Field.Char().label("工号").unique().required();
    static Field name = Field.Char().label("姓名").required();
    static Field sex = Field.Selection(new Options() {{
        put("male", "男");
        put("female", "女");
    }}).label("性别").defaultValue("male").prefetch(false);
    static Field entry_date = Field.Date().label("入职日期");
    static Field birth_date = Field.Date().label("出生日期");
    static Field email = Field.Char().label("邮箱").unique();
    static Field mobile = Field.Char().label("手机号").unique();
    static Field active = Field.Boolean().label("是否在职").defaultValue(true);
    static Field account_id = Field.Many2one("rbac.user").label("系统账号");

    @ServiceMethod(label = "创建账号", doc = "给当前员工创建用户", auth = "createUserAccount")
    public Object createUserAccount(Records records) {
        for (Records record : records) {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("login", record.getString("code"));
            values.put("email", record.get("email"));
            values.put("mobile", record.get("mobile"));
            values.put("name", record.getString("name"));
            values.put("company_ids", Arrays.asList(4, records.getEnv().getCompany().getId(), 0));
            Records user = records.getEnv().get("rbac.user").create(values);
            record.set("account_id", user.getId());
        }
        return Action.reload(records.l10n("添加成功"));
    }
}
