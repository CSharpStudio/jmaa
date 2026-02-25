package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

import java.util.List;

@Model.Meta(name = "wip.production_mobile", inherit = "wip.production", label = "在制品查询-移动端")
public class WipProductionMobile extends ValueModel {
    @ServiceMethod(label = "查询在制品", auth = "read")
    public Object searchByCode(Records records, String code, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.production").searchLimit(fields, Criteria.equal("bind_code_ids.code", code)
            .and("status", "not in", Utils.asList("done", "scrap")), offset, limit, order);
    }

    @ServiceMethod(label = "查询在制品工序", auth = "read")
    public Object searchProcess(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.process").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }

    @ServiceMethod(label = "查询在制品组件", auth = "read")
    public Object searchModule(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.module").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }

    @ServiceMethod(label = "查询在制品不良信息", auth = "read")
    public Object searchDefect(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.defect").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }

    @ServiceMethod(label = "查询在制品维修记录", auth = "read")
    public Object searchRepair(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.repair").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }
}
