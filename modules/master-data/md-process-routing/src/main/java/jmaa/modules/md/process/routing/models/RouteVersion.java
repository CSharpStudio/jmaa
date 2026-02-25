package jmaa.modules.md.process.routing.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.IdWorker;
import org.jmaa.sdk.util.KvMap;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "pr.route_version", label = "工艺路线版本", present = {"route_id", "version", "code"}, presentFormat = "{code}({version})-{route_id}", order = "version desc", authModel = "pr.route")
@Model.UniqueConstraint(name = "code_unique", fields = {"route_id", "code"})
public class RouteVersion extends Model {
    static Field code = Field.Char().label("版本编码").required();
    static Field version = Field.Char().label("版本号").required();
    static Field route_id = Field.Many2one("pr.route").label("工艺路线");
    static Field family_id = Field.Many2one("md.product_family").related("route_id.family_id");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field is_default = Field.Boolean().label("是否默认版本").copy(false).defaultValue(false);
    static Field node_ids = Field.One2many("pr.route_node", "version_id").label("版本节点详情");
    static Field canvas_width = Field.Integer().label("画布宽度").defaultValue(2000).min(500).required();
    static Field canvas_height = Field.Integer().label("画布高度").defaultValue(1600).min(500).required();
    static Field start_x = Field.Integer().label("开始X坐标").defaultValue(100);
    static Field start_y = Field.Integer().label("开始Y坐标").defaultValue(60);
    static Field end_x = Field.Integer().label("结束X坐标").defaultValue(100);
    static Field end_y = Field.Integer().label("结束Y坐标").defaultValue(260);
    static Field is_publish = Field.Boolean().label("是否发布").copy(false).defaultValue(false);

    @ServiceMethod(label = "设为默认", doc = "设置默认版本")
    public Action setDefault(Records record) {
        if (!record.getBoolean("is_publish")) {
            throw new ValidationException(record.l10n("版本未发布，不能设置为默认"));
        }
        Records route = record.getRec("route_id");
        record.getEnv().getCursor().execute("update pr_route_version set is_default=%s where route_id=%s", Utils.asList(false, route.getId()));
        record.set("is_default", true);
        return Action.reload(record.l10n("操作成功"));
    }

    @OnSaved("active")
    public void onActiveSaved(Records records) {
        for (Records record : records) {
            if (record.getBoolean("is_default") && !record.getBoolean("active")) {
                throw new ValidationException(record.l10n("默认版本不能设置为无效"));
            }
        }
    }

    @ServiceMethod(auth = "update", label = "添加工艺节点")
    public Map<String, Object> createNode(Records record, Map<String, Object> values, List<String> fields) {
        values.put("version_id", record.getId());
        Records node = record.getEnv().get("pr.route_node").create(values);
        return node.read(fields).get(0);
    }

    @ServiceMethod(auth = "read", label = "加载艺路线图")
    public Map<String, Object> loadFlow(Records record, Collection<String> fields) {
        Map<String, Object> result = record.read(Utils.asList("canvas_width", "canvas_height", "is_publish")).get(0);
        List<Map<String, Object>> nodes = record.getRec("node_ids").read(fields);
        nodes.add(new KvMap() {{
            put("id", "start");
            put("present", record.l10n("开始"));
            put("x", record.get("start_x"));
            put("y", record.get("start_y"));
        }});
        nodes.add(new KvMap() {{
            put("id", "end");
            put("present", record.l10n("结束"));
            put("x", record.get("end_x"));
            put("y", record.get("end_y"));
        }});
        result.put("nodes", nodes);
        return result;
    }

    @ServiceMethod(label = "发布", doc = "发布后不能再修改")
    public void publish(Records records) {
        records.set("is_publish", true);
        for (Records record : records) {
            Records nodes = record.getEnv().get("pr.route_node")
                .find(Criteria.equal("version_id", record.getId()).and("active", "=", true)
                    .and(Criteria.equal("parent_id", null)));
            if (!nodes.any()) {
                throw new ValidationException(record.l10n("没有工序不能发布"));
            }
            String error = (String) nodes.call("check");
            if (Utils.isNotEmpty(error)) {
                throw new ValidationException(error);
            }
            record.getEnv().getCursor().execute("delete from pr_route_node where version_id=%s and active=%s",
                Utils.asList(record.getId(), false));
        }
    }

    @ServiceMethod(auth = "create", label = "复制版本")
    public Object copyVersion(Records record, String code, String version) {
        Records ver = record.create(new KvMap()
            .set("code", code)
            .set("version", version)
            .set("route_id", record.getRec("route_id").getId())
            .set("canvas_width", record.get("canvas_width"))
            .set("canvas_height", record.get("canvas_height"))
            .set("start_x", record.get("start_x"))
            .set("start_y", record.get("start_y"))
            .set("end_x", record.get("end_x"))
            .set("end_y", record.get("end_y")));
        Records nodes = record.getEnv().get("pr.route_node").find(Criteria.equal("active", true)
            .and("version_id", "=", record.getId()));
        List<Map<String, Object>> toCreate = nodes.read(nodes.getMeta().getFields().keySet().stream()
            .filter(f -> !"child_ids".equals(f)).collect(Collectors.toList()));
        Map<String, String> idMap = new HashMap<>();
        List<List<String>> idList = new ArrayList<>();
        for (Map<String, Object> row : toCreate) {
            String id = (String) row.get("id");
            String newId = IdWorker.nextId();
            idMap.put(id, newId);
            row.put("id", newId);
            row.put("version_id", ver.getId());
            idList.add(Utils.asList(newId, (String) row.get("ok_id"), (String) row.get("ng_id"), (String) row.get("parent_id")));
            row.remove("ok_id");
            row.remove("ng_id");
            row.remove("parent_id");
        }
        nodes.withContext("#autoId", false).createBatch(toCreate);
        for (List<String> row : idList) {
            String okId = idMap.get(row.get(1));
            String ngId = idMap.get(row.get(2));
            String parentId = idMap.get(row.get(3));
            nodes.browse(row.get(0)).update(new KvMap().set("ok_id", okId).set("ng_id", ngId).set("parent_id", parentId));
        }
        return Action.reload(record.l10n("操作成功"));
    }
}
