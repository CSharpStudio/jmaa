package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.services.CreateService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Model.Meta(inherit = "mfg.material_return_mobile")
@Model.Service(remove = "@edit")
@Model.Service(name = "create", label = "创建", auth = "read", description = "为模型创建新记录", type = CreateService.class)
public class MaterialReturnMobile extends ValueModel {
    @Override
    public List<Map<String, Object>> search(Records rec, Collection<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        return rec.getEnv().get("mfg.material_return").search(fields, criteria, offset, limit, order);
    }
}
