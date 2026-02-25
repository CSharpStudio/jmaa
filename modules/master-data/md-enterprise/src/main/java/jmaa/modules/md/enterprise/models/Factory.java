package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.factory", label = "工厂", table = "md_enterprise_model", inherit = {"md.enterprise_model", "mixin.company"})
public class Factory extends ValueModel {

    static Field craft_section = Field.Selection(new Options() {{
        put("assembly", "组装");
        put("molding", "注塑");
        put("machining", "机加");
        put("die-casting", "压铸");
    }}).label("工段").useCatalog().store(false);

    @Override
    public String getTableQuery(Records rec) {
        return "SELECT e.* FROM md_enterprise_model e join md_enterprise_tpl t on e.tpl_id=t.id and t.type='factory'";
    }

    public Records findWorkshops(Records records) {
        Criteria criteria = new Criteria();
        for (Records row : records) {
            criteria.or("parent_path", "like", row.getId());
        }
        return records.getEnv().get("md.workshop").find(criteria);
    }
}
