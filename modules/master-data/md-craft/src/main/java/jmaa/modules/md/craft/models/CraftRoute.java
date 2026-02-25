package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "md.craft_route", label = "制程路线")
public class CraftRoute extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field active = Field.Boolean().label("是否有效");
    static Field node_ids = Field.One2many("md.craft_route_node", "route_id").label("路线节点");
    static Field canvas_width = Field.Integer().label("画布宽度").defaultValue(2000).min(500).required();
    static Field canvas_height = Field.Integer().label("画布高度").defaultValue(1600).min(500).required();
    static Field start_x = Field.Integer().label("开始X坐标").defaultValue(100);
    static Field start_y = Field.Integer().label("开始Y坐标").defaultValue(60);
    static Field end_x = Field.Integer().label("结束X坐标").defaultValue(100);
    static Field end_y = Field.Integer().label("结束Y坐标").defaultValue(260);
    static Field material_ids = Field.One2many("md.material", "craft_route_id").label("产品");

    @ServiceMethod(label = "启用")
    public Object setActive(Records records) {
        records.set("active", true);
        for (Records record : records) {
            Records nodes = record.getEnv().get("md.craft_route_node")
                .find(Criteria.equal("route_id", record.getId()).and("active", "=", true));
            if (!nodes.any()) {
                throw new ValidationException(record.l10n("没有工序不能启用"));
            }
            Set<String> toIds = new HashSet<>();
            for (Records node : nodes) {
                Records ok = node.getRec("next_id");
                if (ok.any()) {
                    toIds.add(ok.getId());
                } else if (!node.getBoolean("is_end")) {
                    throw new ValidationException(record.l10n("%s没有OK连出", node.get("present")));
                }
            }
            Set<String> set = Utils.hashSet(nodes.filter(n -> !n.getBoolean("is_start")).getIds());
            set.removeAll(toIds);
            if (!set.isEmpty()) {
                String msg = nodes.browse(set).stream().map(n -> n.getString("present")).collect(Collectors.joining(","));
                throw new ValidationException(record.l10n("%s 没有连入", msg));
            }
            Records from = nodes.filter(n -> n.getBoolean("is_start"));
            if (!from.any()) {
                throw new ValidationException(record.l10n("开始没有连接"));
            }
            List<String> ids = new ArrayList<>();
            ids.add(from.getId());
            while (true) {
                from = from.getRec("next_id");
                if (!from.any()) {
                    break;
                }
                if (ids.contains(from.getId())) {
                    Records cycle = nodes.browse(ids);
                    String msg = cycle.stream().map(n -> n.getString("present")).collect(Collectors.joining("->"));
                    msg += "->" + from.getString("present");
                    throw new ValidationException(record.l10n("%s 循环连接", msg));
                }
                ids.add(from.getId());
            }
            record.getEnv().getCursor().execute("delete from md_craft_route_node where route_id=%s and active=%s",
                Utils.asList(record.getId(), false));
        }
        return Action.success();
    }

    @ServiceMethod(label = "禁用")
    public Object setInactive(Records records) {
        records.set("active", false);
        return Action.success();
    }

    @ServiceMethod(auth = "update", label = "添加工艺节点")
    public Map<String, Object> createNode(Records record, Map<String, Object> values, List<String> fields) {
        values.put("route_id", record.getId());
        Records node = record.getEnv().get("md.craft_route_node").create(values);
        return node.read(fields).get(0);
    }

    @ServiceMethod(auth = "read", label = "加载艺路线图")
    public Map<String, Object> loadFlow(Records record, Collection<String> fields) {
        Map<String, Object> result = record.read(Utils.asList("canvas_width", "canvas_height", "active")).get(0);
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
}
