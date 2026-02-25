package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.IdWorker;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ServerDate;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(inherit = "mfg.work_order")
public class WorkOrder extends Model {
    static Field work_order_type = Field.Selection(new Options() {{
        put("volume", "量产");
        put("trial", "试产");
        put("rework", "返工");
        put("outsource", "委外");
    }}).label("工单类型").required().tracking();
    static Field bom_version_id = Field.Many2one("md.bom_version").label("BOM编码").tracking();
    static Field bom_ids = Field.One2many("mfg.work_order_bom", "work_order_id").label("BOM");
    static Field process_bom_ids = Field.One2many("mfg.work_order_process_bom", "work_order_id").label("工序BOM");
    static Field issue_qty = Field.Float().label("备料套数").defaultValue(0);
    static Field route_version_id = Field.Many2one("pr.route_version").label("工艺路线").tracking();
    static Field work_order_route_id = Field.Many2one("mfg.work_order_route").label("工单工艺路线").ondelete(DeleteMode.SetNull);

    @ActionMethod
    public Object onProductOrderChange(Records record) {
        AttrAction action = new AttrAction();
        Records productOrder = record.getRec("product_order_id");
        if (productOrder.any()) {
            action.setValue("material_id", productOrder.getRec("material_id"));
            action.setValue("plan_qty", productOrder.getDouble("plan_qty"));
            action.setValue("bom_version_id", productOrder.getRec("bom_version_id"));
        }
        return action;
    }

    @OnSaved("bom_version_id")
    public void onBomVersionSave(Records records) {
        for (Records record : records) {
            Records bom = record.getRec("bom_ids");
            if (!bom.any()) {
                Records ver = record.getRec("bom_version_id");
                if (ver.any()) {
                    Records details = record.getEnv().get("md.bom_details").find(Criteria.equal("version_id", ver.getId()));
                    List<Map<String, Object>> data = new ArrayList<>();
                    double planQty = record.getDouble("plan_qty");
                    for (Records detail : details) {
                        boolean isAlternative = Utils.toBoolean(detail.getBoolean("is_alternative"), false);
                        double qty = detail.getDouble("qty");
                        Map<String, Object> row = new HashMap<>();
                        row.put("material_id", detail.getRec("material_id").getId());
                        row.put("qty", isAlternative ? 0d : qty);
                        row.put("require_qty", isAlternative ? 0d : Utils.round(qty * planQty));
                        row.put("is_alternative", detail.get("is_alternative"));
                        row.put("main_material_id", detail.getRec("main_material_id").getId());
                        row.put("work_order_id", record.getId());
                        data.add(row);
                    }
                    bom.createBatch(data);
                    //TODO计算工序BOM需要与校验BOM版本物料
                    Records processBom = record.getRec("process_bom_ids");
                    if (!processBom.any()) {
                        Records list = record.getEnv().get("md.bom_process").find(Criteria.equal("bom_id", ver.getRec("bom_id").getId()));
                        data = new ArrayList<>();
                        for (Records detail : list) {
                            double qty = detail.getDouble("qty");
                            Map<String, Object> row = new HashMap<>();
                            row.put("process_id", detail.getRec("process_id").getId());
                            row.put("material_id", detail.getRec("material_id").getId());
                            row.put("qty", qty);
                            row.put("key_module", detail.get("key_module"));
                            row.put("require_qty", Utils.round(qty * planQty));
                            row.put("is_alternative", detail.get("is_alternative"));
                            row.put("main_material_id", detail.getRec("main_material_id").getId());
                            row.put("work_order_id", record.getId());
                            data.add(row);
                        }
                        processBom.createBatch(data);
                    }
                }
            }
        }
    }

    @Constrains("bom_ids")
    public void onBomSave(Records records) {
        for (Records record : records) {
            Records bomIds = record.getRec("bom_ids");
            if (bomIds.any()) {
                // 先获取替代料对应主料的集合
                Set<String> mainMaterialCodeSet = bomIds.stream().filter(e -> Utils.toBoolean(e.getBoolean("is_alternative"), false))
                    .map(e -> e.getRec("main_material_id").getString("code")).collect(Collectors.toSet());
                Set<String> baseMaterialCodeSet = bomIds.stream().filter(e -> !Utils.toBoolean(e.getBoolean("is_alternative"), false))
                    .map(e -> e.getRec("material_id").getString("code")).collect(Collectors.toSet());
                mainMaterialCodeSet.removeAll(baseMaterialCodeSet);
                if (!mainMaterialCodeSet.isEmpty()) {
                    throw new ValidationException(records.l10n("替代料对应的主料不存在于当前bom列表,异常主料编码为: {%s}", String.join(",", mainMaterialCodeSet)));
                }
            }
        }
    }

    @ServiceMethod(label = "发放")
    public Object release(Records records, Map<String, Object> values, String comment) {
        if (values != null) {
            records.update(values);
        }
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"draft".equals(orderStatus) && !"suspend".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以发放", record.getSelection("status")));
            }
            if (!record.getRec("resource_id").any()) {
                throw new ValidationException(records.l10n("工单[%s]制造资源不能为空", record.get("code")));
            }
            if (!record.getRec("bom_ids").any()) {
                throw new ValidationException(records.l10n("工单[%s]BOM不能为空", record.get("code")));
            }
            if (!record.getRec("route_version_id").any()) {
                throw new ValidationException(records.l10n("工单[%s]工艺路线版本不能为空", record.get("code")));
            }
            checkRoute(record);
        }
        String body = records.l10n("发放") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "release");
        return Action.success();
    }

    @ServiceMethod(label = "暂停")
    public Object suspend(Records records, String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"release".equals(orderStatus) && !"producing".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以暂停", record.getSelection("status")));
            }
        }
        String body = records.l10n("暂停") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "suspend");
        return Action.success();
    }

    @ServiceMethod(label = "备料")
    public Object requestMaterials(Records record, @Doc("套数") Double qty) {
        Records workOrderBom = record.getRec("bom_ids");
        if (!workOrderBom.any()) {
            throw new ValidationException(record.l10n("工单[%s]没有BOM数据,备料失败", record.get("code")));
        }
        Records warehouse = record.getEnv().get("md.warehouse").find(Criteria.equal("workshop_ids", record.getRec("workshop_id").getId()));
        if (!warehouse.any()) {
            throw new ValidationException(record.l10n("请在仓库中配置车间[%s]的发料仓库", record.getRec("workshop_id").get("present")));
        }
        List<Object> warehouseIds = new ArrayList<>();
        for (Records row : warehouse) {
            warehouseIds.add(Utils.asList(4, row.getId()));
        }
        Map<String, Object> issueMap = new HashMap<>();
        issueMap.put("type", "mfg.work_order");
        issueMap.put("related_code", record.getString("code"));
        issueMap.put("workshop_id", record.getRec("workshop_id").getId());
        issueMap.put("related_id", record.getId());
        issueMap.put("warehouse_ids", warehouseIds);
        issueMap.put("status", "commit");
        Records issue = record.getEnv().get("mfg.material_issue").create(issueMap);
        List<Map<String, Object>> issueLineList = new ArrayList<>();
        for (Records bom : workOrderBom) {
            //单位用量为0则不需要发料
            double bomQty = bom.getDouble("qty");
            if (Utils.large(bomQty, 0)) {
                Map<String, Object> lineMap = new HashMap<>();
                Records materialId = bom.getRec("material_id");
                String issueId = issue.getId();
                lineMap.put("issue_id", issueId);
                lineMap.put("material_id", materialId.getId());
                lineMap.put("request_qty", Utils.round(bomQty * qty));
                lineMap.put("status", "new");
                issueLineList.add(lineMap);
            }
        }
        record.getEnv().get("mfg.material_issue_line").createBatch(issueLineList);
        // 生成对应发料单以后,修改当前工单的备料数
        record.set("issue_qty", record.getDouble("issue_qty") + qty);
        return Action.reload(record.l10n("操作成功"));
    }

    @OnSaved("route_version_id")
    public void onRouteVersionSave(Records records) {
        for (Records record : records) {
            Records route = record.getRec("work_order_route_id");
            route.delete();
            Records version = record.getRec("route_version_id");
            if (version.any()) {
                route = route.create(new KvMap()
                    .set("work_order_id", record.getId())
                    .set("canvas_width", version.get("canvas_width"))
                    .set("canvas_height", version.get("canvas_height"))
                    .set("start_x", version.get("start_x"))
                    .set("start_y", version.get("start_y"))
                    .set("end_x", version.get("end_x"))
                    .set("end_y", version.get("end_y")));
                record.set("work_order_route_id", route.getId());
                Records nodes = record.getEnv().get("pr.route_node").find(Criteria.equal("version_id", version.getId()));
                List<Map<String, Object>> toCreate = nodes.read(nodes.getMeta().getFields().keySet().stream()
                    .filter(f -> !"child_ids".equals(f)).collect(Collectors.toList()));
                Map<String, String> idMap = new HashMap<>();
                List<List<String>> idList = new ArrayList<>();
                for (Map<String, Object> row : toCreate) {
                    String id = (String) row.get("id");
                    String newId = IdWorker.nextId();
                    idMap.put(id, newId);
                    row.put("id", newId);
                    row.put("route_id", route.getId());
                    idList.add(Utils.asList(newId, (String) row.get("ok_id"), (String) row.get("ng_id"), (String) row.get("parent_id")));
                    row.remove("ok_id");
                    row.remove("ng_id");
                    row.remove("parent_id");
                }
                Records node = record.getEnv().get("mfg.work_order_route_node").withContext("#autoId", false).createBatch(toCreate);
                for (List<String> row : idList) {
                    String okId = idMap.get(row.get(1));
                    String ngId = idMap.get(row.get(2));
                    String parentId = idMap.get(row.get(3));
                    node.browse(row.get(0)).update(new KvMap().set("ok_id", okId).set("ng_id", ngId).set("parent_id", parentId));
                }
            }
        }
    }

    @OnSaved({"material_id", "resource_id"})
    public void onMaterialSave(Records records) {
        for (Records record : records) {
            Records material = record.getRec("material_id");
            Records resource = record.getRec("resource_id");
            Records route = record.getRec("route_version_id");
            Records bom = record.getRec("bom_version_id");
            if (material.any() && resource.any() && !route.any()) {
                //计算工艺路线
                Records productRoute = record.getEnv().get("mfg.product_route").find(Criteria.equal("material_id", material.getId()).and("active", "=", true)
                    .and("begin_date", "<=", new ServerDate()), 0, 1, "begin_date desc");
                if (productRoute.any()) {
                    record.set("route_version_id", productRoute.getRec("route_version_id"));
                    continue;
                }
                Records resourceRoute = record.getEnv().get("mfg.resource_route").find(Criteria.equal("resource_id", resource.getId()).and("active", "=", true)
                    .and("begin_date", "<=", new ServerDate()), 0, 1, "begin_date desc");
                if (resourceRoute.any()) {
                    record.set("route_version_id", resourceRoute.getRec("route_version_id"));
                    continue;
                }
                Records routeVersion = record.getEnv().get("pr.route_version").find(Criteria.equal("route_id.family_id", material.getRec("family_id").getId())
                    .and("active", "=", true).and("is_default", "=", true), 0, 1, "");
                if (routeVersion.any()) {
                    record.set("route_version_id", routeVersion.getId());
                }
            }
            if (material.any() && !bom.any()) {
                //计算BOM
                Records bomVersion = record.getEnv().get("md.bom_version").find(Criteria.equal("bom_id.material_id", material.getId())
                    .and("active", "=", true).and("is_default", "=", true), 0, 1, "");
                if (bomVersion.any()) {
                    record.set("bom_version_id", bomVersion.getId());
                }
            }
        }
    }

    public void checkRoute(Records record) {
        Records route = record.getRec("work_order_route_id");
        Records nodes = record.getEnv().get("mfg.work_order_route_node")
            .find(Criteria.equal("route_id", route.getId()).and("active", "=", true)
                .and(Criteria.equal("parent_id", null)));
        if (!nodes.any()) {
            throw new ValidationException(record.l10n("工艺路线没有工序"));
        }
        String error = (String) nodes.call("check");
        if (Utils.isNotEmpty(error)) {
            throw new ValidationException(record.l10n("工艺路线：") + error);
        }
        record.getEnv().getCursor().execute("delete from mfg_work_order_route_node where route_id=%s and active=%s",
            Utils.asList(route.getId(), false));
    }

    @ServiceMethod(auth = "read", label = "加载艺路线图")
    public Object loadFlow(Records record, List<String> fields) {
        Records route = record.getRec("work_order_route_id");
        Map<String, Object> result = route.read(Utils.asList("canvas_width", "canvas_height")).get(0);
        List<Map<String, Object>> nodes = route.getRec("node_ids").read(fields);
        String status = record.getString("status");
        result.put("editable", "draft".equals(status) || "suspend".equals(status));
        result.put("family_id", record.getRec("material_id").getRec("family_id").getId());
        nodes.add(new KvMap() {{
            put("id", "start");
            put("present", route.l10n("开始"));
            put("x", route.get("start_x"));
            put("y", route.get("start_y"));
        }});
        nodes.add(new KvMap() {{
            put("id", "end");
            put("present", route.l10n("结束"));
            put("x", route.get("end_x"));
            put("y", route.get("end_y"));
        }});
        result.put("nodes", nodes);
        return result;
    }
}
