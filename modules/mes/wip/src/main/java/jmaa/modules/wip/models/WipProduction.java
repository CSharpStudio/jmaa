package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.Table;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "wip.production", label = "在制品查询", logAccess = BoolState.False, inherit = {"mixin.company"})
public class WipProduction extends Model {
    static Field route_id = Field.Many2one("wip.route").label("工艺流程").ondelete(DeleteMode.SetNull);
    static Field process_ids = Field.One2many("wip.process", "product_id").label("生产工序");
    static Field code_ids = Field.One2many("wip.code", "product_id").label("条码");
    static Field bind_code_ids = Field.One2many("wip.bind_code", "product_id").label("绑定条码");
    static Field product_code = Field.Char().label("产品条码").index();
    static Field status = Field.Selection(new Options() {{
        put("producing", "生产中");
        put("done", "完工");
        put("scrap", "报废");
        put("pause", "暂停");
    }}).label("状态");
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");
    static Field qty = Field.Float().label("合格数量");
    static Field ng_qty = Field.Float().label("不良数量").defaultValue(0);
    static Field material_id = Field.Many2one("md.material").label("物料");
    static Field material_name_spec = Field.Char().label("产品名称规格").related("material_id.name_spec");
    static Field defect_ids = Field.One2many("wip.defect", "product_id").label("缺陷");
    static Field repair_ids = Field.One2many("wip.repair", "product_id").label("维修记录");
    static Field module_ids = Field.One2many("wip.module", "product_id").label("装配件");
    static Field current_process_id = Field.Many2one("md.work_process").related("route_id.current_node_id.process_id").label("当前工序");
    static Field next_process_name = Field.Char().label("后工序").compute("getNextProcess");

    public String getNextProcess(Records record) {
        Records nodes = record.getRec("route_id").getRec("next_node_ids");
        return nodes.stream().map(r -> r.getRec("process_id").getString("present")).collect(Collectors.joining(","));
    }

    public void bindCode(Records records, String codeType, String code) {
        Environment env = records.getEnv();
        for (Records wip : records) {
            if ("container".equals(codeType)) {
                Cursor cr = env.getCursor();
                cr.execute("update wip_container set status='in-use',work_order_id=%s,qty=%s where code=%s and status='idle'",
                    Arrays.asList(wip.getRec("work_order_id").getId(), wip.get("qty"), code));
                if (cr.getRowCount() < 1) {
                    Records container = env.get("wip.container").find(Criteria.equal("code", code));
                    if (!"idle".equals(container.getString("status"))) {
                        throw new ValidationException(env.l10n("容器条码[%s]状态[%s]，不能绑定", code, container.getSelection("status")));
                    }
                }
            } else {
                if ("product".equals(codeType) || "batch".equals(codeType)) {
                    wip.set("product_code", code);
                }
                env.get("wip.code").create(new KvMap()
                    .set("code", code)
                    .set("code_type", codeType)
                    .set("product_id", wip.getId()));
            }
            env.get("wip.bind_code").create(new KvMap()
                .set("product_id", wip.getId())
                .set("code", code)
                .set("code_type", codeType));
        }
    }

    public void updateNextProcess(Records records, String result) {
        for (Records record : records) {
            Records route = record.getRec("route_id");
            Records current = route.getRec("current_node_id");
            if ("group".equals(current.get("type"))) {

            } else {
                if (current.getBoolean("is_end")) {
                    record.set("status", "done");
                    record.call("archive");
                    return;
                }
                Set<String> nextNode = new HashSet<>();
                if (current.getBoolean("is_repeatable")) {
                    nextNode.add(current.getId());
                }
                addNextNode(record, nextNode, current, result);
                route.set("next_node_ids", Utils.asList(Utils.asList(6, 0, new ArrayList<>(nextNode))));
            }
        }
    }

    public void addNextNode(Records record, Set<String> nextProcess, Records current, String result) {
        Records next = current.getRec("ok".equals(result) ? "ok_id" : "ng_id");
        if (!next.any()) {
            throw new ValidationException(record.l10n("工序[%s]未配置后续工序", current.getRec("process_id").get("present")));
        }
        nextProcess.add(next.getId());
        if (next.getBoolean("is_optional")) {
            addNextNode(record, nextProcess, next, "ok");
        }
    }

    public void updateCurrentProcess(Records records, String processId, boolean finish) {
        for (Records record : records) {
            Records route = record.getRec("route_id");
            Records next = route.getRec("next_node_ids");
            Records node = route.getRec("current_node_id");
            Records currentProcess = node.getRec("process_id");
            if (Utils.equals(processId, currentProcess.getId())) {
                int times = route.getInteger("current_process_times") + 1;
                route.set("current_process_times", times);
            } else {
                Records current = findNextNode(records, next, processId);
                route.set("current_node_id", current.getId());
                route.set("current_process_times", finish ? 1 : 0);
                route.set("current_ok_qty", 0);
                route.set("current_ng_qty", 0);
            }
        }
    }

    public Records findNextNode(Records records, Records next, String processId) {
        for (Records node : next) {
            if ("group".equals(node.get("type"))) {
                for (Records child : node.getRec("child_ids")) {
                    if (Utils.equals(processId, child.getRec("process_id").getId())) {
                        return node;
                    }
                }
            } else if (Utils.equals(processId, node.getRec("process_id").getId())) {
                return node;
            }
        }
        throw new ValidationException("找到不当前工序的路由");
    }

    public void checkProcess(Records records, String processId) {
        for (Records record : records) {
            Records route = record.getRec("route_id");
            Records nodes = route.getRec("next_node_ids");
            if (!nodes.any()) {
                throw new ValidationException(record.l10n("没有后续工序"));
            }
            List<String> nextProcess = new ArrayList<>();
            for (Records node : nodes) {
                if ("group".equals(node.get("type"))) {
                    nextProcess.addAll(node.getRec("child_ids").stream().map(r -> r.getRec("process_id").getId()).collect(Collectors.toList()));
                } else {
                    nextProcess.add(node.getRec("process_id").getId());
                }
            }
            if (!nextProcess.contains(processId)) {
                Records process = records.getEnv().get("md.work_process").browse(nextProcess);
                String processPresent = process.stream().map(p -> p.getString("present")).collect(Collectors.joining(","));
                throw new ValidationException(records.l10n("采集工序不正确，应为[%s]", processPresent));
            }
        }
    }

    public Records createWipProduction(Records record, String codeType, String code, String workOrderId, String processId, double qty) {
        Environment env = record.getEnv();
        Records workOrder = env.get("mfg.work_order", workOrderId);
        Records route = workOrder.getRec("work_order_route_id");
        Records nodes = route.getRec("node_ids");
        Records start = nodes.filter(n -> n.getBoolean("is_start"));
        List<String> processIds;
        if ("group".equals(start.get("type"))) {
            processIds = start.getRec("child_ids").stream().map(r -> r.getRec("process_id").getId()).collect(Collectors.toList());
        } else {
            processIds = Utils.asList(start.getRec("process_id").getId());
        }
        if (!processIds.contains(processId)) {
            throw new ValidationException(record.l10n("产品[%s]未上线，上线工序为[%s]", code, start.getRec("process_id").get("present")));
        }
        if ("block".equals(codeType)) {
            Records blockCode = env.get("wip.block_code").find(Criteria.equal("code", code));
            List<Map<String, Object>> toCreate = new ArrayList<>();
            for (Records block : blockCode) {
                Records label = block.getRec("label_id");
                Records wipRoute = env.get("wip.route").create(new KvMap()
                    .set("route_id", route.getId())
                    .set("next_node_ids", Utils.asList(Utils.asList(4, start.getId()))));
                toCreate.add(new KvMap()
                    .set("status", "producing")
                    .set("route_id", wipRoute.getId())
                    .set("material_id", workOrder.getRec("material_id").getId())
                    .set("work_order_id", workOrderId)
                    .set("company_id", workOrder.getRec("company_id").getId())
                    .set("qty", label.get("qty"))
                    .set("product_code", label.get("sn")));
            }
            return env.get("wip.production").createBatch(toCreate);
        }
        if ("product".equals(codeType) || "batch".equals(codeType)) {
            qty = getQtyByCode(record, codeType, code);
        }
        Records wipRoute = env.get("wip.route").create(new KvMap()
            .set("route_id", route.getId())
            .set("next_node_ids", Utils.asList(Utils.asList(4, start.getId()))));
        if ("container".equals(codeType)) {
            code = (String) env.get("code.coding").find(Criteria.equal("code", "SYS-BATCH")).call("createCode", Collections.emptyMap());
        }
        Records wip = env.get("wip.production").create(new KvMap()
            .set("status", "producing")
            .set("route_id", wipRoute.getId())
            .set("material_id", workOrder.getRec("material_id").getId())
            .set("work_order_id", workOrderId)
            .set("company_id", workOrder.getRec("company_id").getId())
            .set("qty", qty)
            .set("product_code", code));
        if ("container".equals(codeType)) {
            bindCode(wip, "batch", code);
        }
        return wip;
    }

    public Records createWipProcess(Records records, List<Map<String, Object>> codeTypes, Records station, double okQty, double ngQty) {
        List<Map<String, Object>> values = new ArrayList<>();
        Records duty = records.getEnv().get("mfg.work_station_on_duty").find(Criteria.equal("station_id", station.getId()));
        List<Object> opIds = duty.stream().map(r -> Utils.asList(4, r.getRec("staff_id").getId())).collect(Collectors.toList());
        List<String> codes = new ArrayList<>();
        String moveCodeType = null;
        for (Map<String, Object> row : codeTypes) {
            boolean isMove = Utils.toBoolean(row.get("is_move_code"));
            if (isMove) {
                moveCodeType = (String) row.get("code_type");
            }
            codes.add((String) row.get("code"));
        }
        for (Records wip : records) {
            KvMap map = new KvMap();
            map.set("product_id", wip.getId())
                .set("process_id", station.getRec("process_id").getId())
                .set("station_id", station.getId())
                .set("resource_id", station.getRec("resource_id").getId())
                .set("ok_qty", okQty)
                .set("ng_qty", ngQty)
                .set("status", "done")
                .set("code", codes.stream().collect(Collectors.joining(",")))
                .set("code_type", moveCodeType)
                .set("op_ids", opIds);
            values.add(map);
        }
        return records.getEnv().get("wip.process").createBatch(values);
    }

    public void updateBindCode(Records records, List<Map<String, Object>> codeTypes) {
        Cursor cr = records.getEnv().getCursor();
        Table toBind = new Table();
        for (Map<String, Object> row : codeTypes) {
            boolean unbind = "unbind".equals(row.get("bind_mode"));
            String code = (String) row.get("code");
            String codeType = (String) row.get("code_type");
            if (unbind) {
                cr.execute("DELETE FROM wip_bind_code WHERE code=%s AND code_type=%s", Arrays.asList(code, codeType));
                if ("container".equals(codeType)) {
                    cr.execute("update wip_container set status='idle',work_order_id=null,qty=0 where code=%s", Arrays.asList(code));
                }
            } else {
                toBind.add(code, codeType);
            }
        }
        for (Records wipProduction : records) {
            Records bindCode = wipProduction.getEnv().get("wip.bind_code")
                .find(Criteria.in("code", toBind.getColumn(0)).and("product_id", "=", wipProduction.getId()));
            for (List<Object> row : toBind) {
                Records exists = bindCode.filter(r -> Utils.equals(r.getString("code"), row.get(0)));
                if (!exists.any()) {
                    bindCode(wipProduction, (String) row.get(1), (String) row.get(0));
                }
            }
        }
    }

    public Map<String, Object> findMoveCode(Records records, List<Map<String, Object>> codeTypes) {
        List<Map<String, Object>> moveCode = codeTypes.stream().filter(m -> Utils.toBoolean(m.get("is_move_code"))).collect(Collectors.toList());
        if (!moveCode.isEmpty()) {
            return moveCode.get(0);
        }
        return codeTypes.get(0);
    }

    public Records findByCode(Records record, String codeType, String code) {
        Records binds = record.getEnv().get("wip.bind_code").find(Criteria.equal("code", code).and("code_type", "=", codeType));
        List<String> ids = new ArrayList<>();
        for (Records bind : binds) {
            ids.add(bind.getRec("product_id").getId());
        }
        return record.browse(ids);
    }

    public double getQtyByCode(Records records, String codeType, String code) {
        if ("product".equals(codeType)) {
            Records product = records.getEnv().get("lbl.product_label").find(Criteria.equal("sn", code));
            return product.getDouble("qty");
        } else if ("batch".equals(codeType)) {
            Records batch = records.getEnv().get("wip.batch_code").find(Criteria.equal("code", code));
            return batch.getDouble("qty");
        }
        throw new ValidationException(records.l10n("不支持条码[%s]类型[%s]", code, codeType));
    }

    public void checkRepeatable(Records records, String code) {
        for (Records wip : records) {
            Records route = wip.getRec("route_id");
            Records node = route.getRec("current_node_id");
            boolean repeatable = node.getBoolean("is_repeatable");
            if (!repeatable) {
                int times = route.getInteger("current_process_times");
                if (times > 1) {
                    throw new ValidationException(records.l10n("条码[%s]不允许工序重复过站!", code));
                }
            }
        }
    }

    public Records move(Records records, List<Map<String, Object>> codeTypes, Records station, Records workOrder, boolean finish, String result, double qty) {
        Records process = station.getRec("process_id");
        Map<String, Object> moveCode = findMoveCode(records, codeTypes);
        String codeType = (String) moveCode.get("code_type");
        String code = (String) moveCode.get("code");
        Records wipProduction = findByCode(records, codeType, code);
        if (wipProduction.any()) {
            for (Records wip : wipProduction) {
                String status = wip.getString("status");
                if (!"producing".equals(status)) {
                    throw new ValidationException(records.l10n("条码[%s]状态[%s]，不能生产", code, wip.getSelection("status")));
                }
            }
            //校验工序
            checkProcess(wipProduction, process.getId());
        } else {
            //根据标签数量创建在制品记录，如果标签是容器，则使用报工数量
            wipProduction = createWipProduction(wipProduction, codeType, code, workOrder.getId(), process.getId(), qty);
        }
        updateCurrentProcess(wipProduction, process.getId(), finish);
        checkRepeatable(wipProduction, code);
        double okQty = "ok".equals(result) ? qty : 0;
        double ngQty = "ng".equals(result) ? qty : 0;
        Records wipProcess = createWipProcess(wipProduction, codeTypes, station, okQty, ngQty);
        updateBindCode(wipProduction, codeTypes);
        if (finish) {
            updateNextProcess(wipProduction, result);
        } else {
            for (Records wip : wipProduction) {
                Records route = wip.getRec("route_id");
                Records current = route.getRec("current_node_id");
                route.set("next_node_ids", Utils.asList(Utils.asList(6, 0, Utils.asList(current.getId()))));
            }
        }
        return wipProcess;
    }

    public Records report(Records records, List<Map<String, Object>> codeTypes, Records station, Records workOrder, double okQty, double ngQty) {
        Records process = station.getRec("process_id");
        Map<String, Object> moveCode = findMoveCode(records, codeTypes);
        String codeType = (String) moveCode.get("code_type");
        String code = (String) moveCode.get("code");
        Records wipProduction = findByCode(records, codeType, code);
        if (wipProduction.any()) {
            for (Records wip : wipProduction) {
                String status = wip.getString("status");
                if (!"producing".equals(status)) {
                    throw new ValidationException(records.l10n("条码[%s]状态[%s]，不能生产", code, wip.getSelection("status")));
                }
            }
            //校验工序
            checkProcess(wipProduction, process.getId());
        } else {
            //根据标签数量创建在制品记录，如果标签是容器，则使用报工数量
            wipProduction = createWipProduction(wipProduction, codeType, code, workOrder.getId(), process.getId(), Utils.round(okQty + ngQty));
        }
        updateCurrentProcess(wipProduction, process.getId(), true);
        for (Records wip : wipProduction) {
            Records route = wip.getRec("route_id");
            boolean repeatable = route.getRec("current_node_id").getBoolean("is_repeatable");
            double qty = wip.getDouble("qty");
            if (repeatable) {
                //可重复报工时，总报工数量不能超过可报工数量，报工不校验最大过站次数限制
                double currentOkQty = route.getDouble("current_ok_qty");
                double currentNgQty = route.getDouble("current_ng_qty");
                double totalQty = Utils.round(currentOkQty + currentNgQty + okQty + ngQty);
                if (Utils.large(totalQty, qty)) {
                    throw new ValidationException(records.l10n("条码[%s]总报工数量[%s]超过可报工数量[%s]", code, totalQty, qty));
                }
                if (Utils.equals(qty, totalQty)) {
                    wip.set("qty", currentOkQty + okQty);
                    wip.set("ng_qty", wip.getDouble("ng_qty") + currentNgQty + ngQty);
                }
                route.set("current_ok_qty", currentOkQty + okQty);
                route.set("current_ng_qty", currentNgQty + ngQty);
            } else {
                if (!Utils.equals(qty, okQty + ngQty)) {
                    throw new ValidationException(records.l10n("条码[%s]应报工数量[%s]，当前报工数量[%s]", code, qty, Utils.round(okQty + ngQty)));
                }
                wip.set("qty", okQty);
                wip.set("ng_qty", wip.getDouble("ng_qty") + ngQty);
            }
        }
        Records wipProcess = createWipProcess(wipProduction, codeTypes, station, okQty, ngQty);
        updateBindCode(wipProduction, codeTypes);
        updateNextProcess(wipProduction, "ok");
        return wipProcess;
    }

    public void archive(Records records) {
        for (Records record : records) {
            record.getRec("route_id").delete();
        }
        List<String> ids = Utils.asList(records.getIds());
        Environment env = records.getEnv();
        Cursor cr = env.getCursor();
        cr.execute("delete from wip_bind_code where product_id in %s", Utils.asList(ids));
        //wip.code
        Records code = env.get("wip.code");
        String fields = code.getMeta().getFields().values().stream().filter(f -> f.isStore())
            .map(f -> f.getName()).collect(Collectors.joining(","));
        String sql = String.format("insert into wip_trace_code(%s) select %s from wip_code where product_id in %%s", fields, fields);
        cr.execute(sql, Utils.asList(ids));
        cr.execute("delete from wip_code where product_id in %s", Utils.asList(ids));

        //wip.process
        Records process = env.get("wip.process");
        fields = process.getMeta().getFields().values().stream().filter(f -> f.isStore() && !"op_ids".equals(f.getName()))
            .map(f -> f.getName()).collect(Collectors.joining(","));
        sql = String.format("insert into wip_trace_process(%s) select %s from wip_process where product_id in %%s", fields, fields);
        cr.execute(sql, Utils.asList(ids));
        cr.execute("insert into wip_trace_process_staff(process_id,staff_id) select process_id,staff_id from wip_process_staff where process_id in(select id from wip_process where product_id in %s)", Utils.asList(ids));
        cr.execute("delete from wip_process where product_id in %s", Utils.asList(ids));
        //wip.module
        Records module = env.get("wip.module");
        fields = module.getMeta().getFields().values().stream().filter(f -> f.isStore())
            .map(f -> f.getName()).collect(Collectors.joining(","));
        sql = String.format("insert into wip_trace_module(%s) select %s from wip_module where product_id in %%s", fields, fields);
        cr.execute(sql, Utils.asList(ids));
        cr.execute("delete from wip_module where product_id in %s", Utils.asList(ids));
        //wip.defect
        Records defect = env.get("wip.defect");
        fields = defect.getMeta().getFields().values().stream().filter(f -> f.isStore() && !"repair_ids".equals(f.getName()))
            .map(f -> f.getName()).collect(Collectors.joining(","));
        sql = String.format("insert into wip_trace_defect(%s) select %s from wip_defect where product_id in %%s", fields, fields);
        cr.execute(sql, Utils.asList(ids));
        cr.execute("delete from wip_defect where product_id in %s", Utils.asList(ids));
        //wip.repair
        Records repair = env.get("wip.repair");
        fields = repair.getMeta().getFields().values().stream().filter(f -> f.isStore())
            .map(f -> f.getName()).collect(Collectors.joining(","));
        sql = String.format("insert into wip_trace_repair(%s) select %s from wip_repair where product_id in %%s", fields, fields);
        cr.execute(sql, Utils.asList(ids));
        cr.execute("delete from wip_repair where product_id in %s", Utils.asList(ids));
    }
}
