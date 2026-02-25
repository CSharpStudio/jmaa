package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;

import java.util.ArrayList;
import java.util.List;

@Model.Meta(name = "md.workshop", label = "车间", table = "md_enterprise_model", inherit = {"md.enterprise_model", "mixin.company"})
public class Workshop extends ValueModel {

    @Override
    public String getTableQuery(Records rec) {
        return "SELECT e.* FROM md_enterprise_model e join md_enterprise_tpl t on e.tpl_id=t.id and t.type='workshop'";
    }

    public List<Object[]> getPresent(Records records) {
        List<Object[]> result = new ArrayList<>();
        for (Records rec : records) {
            String present = String.format("%s(%s)", rec.get("name"), rec.get("code"));
            Records parent = rec.getRec("parent_id");
            if (parent.any()) {
                present = parent.get("name") + "/" + present;
            }
            result.add(new Object[]{rec.getId(), present});
        }
        return result;
    }

    @Override
    public Criteria searchPresent(Records rec, String op, Object value) {
        return Criteria.binary("code", op, value).or(Criteria.binary("name", op, value))
                .or(Criteria.binary("parent_id.code", op, value)).or(Criteria.binary("parent_id.name", op, value));
    }
}
