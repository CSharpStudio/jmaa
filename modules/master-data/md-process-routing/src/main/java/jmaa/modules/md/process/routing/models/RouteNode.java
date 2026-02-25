package jmaa.modules.md.process.routing.models;

import org.jmaa.sdk.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Model.Meta(name = "pr.route_node", label = "工艺节点", present = "process_id", order = "seq", authModel = "pr.route_version")
@Model.Service(remove = {"create", "createBatch", "copy", "delete"})
public class RouteNode extends Model {
    static Field type = Field.Selection(new Options() {{
        put("process", "工序");
        put("group", "工序组");
        put("gateway", "网关");
    }}).label("类型").defaultValue("process");
    static Field parent_id = Field.Many2one("pr.route_node").label("工序组").ondelete(DeleteMode.Cascade);
    static Field child_ids = Field.One2many("pr.route_node", "parent_id").label("工序");
    static Field process_id = Field.Many2one("md.work_process").label("工序");
    static Field process_type = Field.Selection().related("process_id.process_type").label("工序分类");
    static Field collection_result = Field.Selection().related("process_id.collection_result").label("采集结果");
    static Field ok_id = Field.Many2one("pr.route_node").label("OK工序").ondelete(DeleteMode.SetNull);
    static Field ng_id = Field.Many2one("pr.route_node").label("NG工序").ondelete(DeleteMode.SetNull);
    static Field seq = Field.Integer().label("顺序节点").defaultValue(1000);
    static Field version_id = Field.Many2one("pr.route_version").label("工艺路线版本").ondelete(DeleteMode.Cascade);
    static Field is_optional = Field.Boolean().label("可选");
    static Field is_output = Field.Boolean().label("计产");
    static Field create_task = Field.Boolean().label("生成任务单");
    static Field min_interval = Field.Float().label("最小间隔(分钟)").help("与前工序的最小间隔时间");
    static Field enable_move_in = Field.Boolean().label("进出站控制");
    static Field min_duration = Field.Float().label("进出站最小时长(分钟)").help("控制进站->出站的间隔时间");
    static Field is_repeatable = Field.Boolean().label("允许重复过站");
    static Field max_times = Field.Integer().label("最大过站次数");
    static Field is_deduction = Field.Boolean().label("扣料").help("根据工单BOM反推扣料");
    static Field to_fqc = Field.Boolean().label("生成FQC检验单");
    static Field x = Field.Integer().label("X坐标");
    static Field y = Field.Integer().label("Y坐标");
    static Field is_start = Field.Boolean().label("开始");
    static Field is_end = Field.Boolean().label("结束");
    static Field group_options = Field.Integer().label("可选工序数量").help("可选工序必须完成的数量").defaultValue(0);
    static Field gateway_expr = Field.Char().label("网关表达式").help("Closure表达式，如 r->r.getDouble('qty') > 10");
    static Field label = Field.Char().label("标题");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);

    @ActionMethod
    public Action processChangeAction(Records record) {
        return Action.attr().setValue("process_type", record.getRec("process_id").get("process_type"));
    }

    @Override
    public List<Object[]> getPresent(Records records) {
        List<Object[]> result = new ArrayList<>();
        for (Records rec : records) {
            String type = rec.getString("type");
            String present = "process".equals(type) ? rec.getRec("process_id").getString("present") : rec.getString("label");
            result.add(new Object[]{rec.getId(), present});
        }
        return result;
    }

    /**
     * 校验节点
     */
    public String check(Records nodes) {
        Set<String> toIds = new HashSet<>();
        int output = 0;
        for (Records node : nodes) {
            Records ok = node.getRec("ok_id");
            if (ok.any()) {
                toIds.add(ok.getId());
            } else if (!node.getBoolean("is_end")) {
                return nodes.l10n("%s没有OK连出", node.get("present"));
            }
            if ("ok/ng".equals(node.getString("collection_result"))) {
                Records ng = node.getRec("ng_id");
                if (!ng.any()) {
                    return nodes.l10n("%s没有NG连出", node.get("present"));
                }
                toIds.add(ng.getId());
            }
            if ("process".equals(node.getString("type")) && node.getBoolean("is_output")) {
                output++;
            }
            if ("group".equals(node.getString("type"))) {
                Records child = node.getRec("child_ids");
                if (child.size() < 2) {
                    return nodes.l10n("工序组[%s]至少设置两个工序", node.get("label"));
                }
                for (Records row : child) {
                    if (row.getBoolean("is_output")) {
                        output++;
                    }
                }
            }
        }
        if (output == 0) {
            return nodes.l10n("没有设置计产工序");
        }
        if (output > 1) {
            return nodes.l10n("只能设置一个计产工序，当前设置[%s]个", output);
        }
        Set<String> set = Utils.hashSet(nodes.filter(n -> !n.getBoolean("is_start")).getIds());
        set.removeAll(toIds);
        if (!set.isEmpty()) {
            String msg = nodes.browse(set).stream().map(n -> n.getString("present")).collect(Collectors.joining(","));
            return nodes.l10n("%s 没有连入", msg);
        }
        Records from = nodes.filter(n -> n.getBoolean("is_start"));
        if (!from.any()) {
            return nodes.l10n("开始没有连接");
        }
        int seq = 1;
        List<String> ids = new ArrayList<>();
        from.set("seq", seq++);
        ids.add(from.getId());
        while (true) {
            from = from.getRec("ok_id");
            if (!from.any()) {
                break;
            }
            if (ids.contains(from.getId())) {
                Records cycle = nodes.browse(ids);
                String msg = cycle.stream().map(n -> n.getString("present")).collect(Collectors.joining("->"));
                msg += "->" + from.getString("present");
                return nodes.l10n("%s 循环连接", msg);
            }
            ids.add(from.getId());
            from.set("seq", seq++);
        }
        return null;
    }
}
