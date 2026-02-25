package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

import java.util.List;

@Model.Meta(name = "wip.trace_production_mobile", inherit = "wip.trace_production", label = "产品履历-移动端")
public class TraceProductionMobile extends ValueModel {
    @ServiceMethod(label = "查询产品履历", auth = "read")
    public Object searchByCode(Records records, String code, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.trace_production").searchLimit(fields, Criteria.equal("code_ids.code", code)
            .and("status", "in", Utils.asList("done", "scrap")), offset, limit, order);
    }

    @ServiceMethod(label = "查询产品履历工序", auth = "read")
    public Object searchProcess(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.trace_process").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }

    @ServiceMethod(label = "查询产品履历组件", auth = "read")
    public Object searchModule(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.trace_module").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }

    @ServiceMethod(label = "查询产品履历不良信息", auth = "read")
    public Object searchDefect(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.trace_defect").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }

    @ServiceMethod(label = "查询产品履历维修记录", auth = "read")
    public Object searchRepair(Records records, String wipId, List<String> fields, int offset, int limit, String order) {
        return records.getEnv().get("wip.trace_repair").searchLimit(fields, Criteria.equal("product_id", wipId),
            offset, limit, order);
    }
}
