package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.tools.DateUtils;
import org.jmaa.sdk.tools.IdWorker;
import org.jmaa.sdk.util.KvMap;
import org.apache.commons.collections4.SetUtils;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "as.to_plan_order", label = "待排订单", authModel = "as.task_scheduling")
public class ToPlanOrder extends ValueModel {
    static Field code = Field.Char().label("生产订单号");
    static Field material_id = Field.Many2one("md.material").label("物料");
    static Field material_name_spec = Field.Char().related("material_id.name_spec").label("规格名称");
    static Field plan_qty = Field.Float().label("计划数量");
    static Field factory_id = Field.Many2one("md.factory").label("工厂");
    static Field customer_due_date = Field.Date().label("客户交期");
    static Field factory_due_date = Field.Date().label("工厂交期");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field sales_order_id = Field.Many2one("sales.order").label("销售订单");
    static Field priority = Field.Integer().label("优先级").defaultValue(100);

    @ServiceMethod(label = "加载制程订单", auth = "read")
    public Object searchOrder(Records record, List<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        criteria.and(Criteria.greater("craft_order_ids.to_plan_qty", 0));
        return record.getEnv().get("mfg.product_order").searchLimit(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(label = "统计制程订单数量", auth = "read")
    public Object countOrder(Records record, Criteria criteria) {
        criteria.and(Criteria.greater("craft_order_ids.to_plan_qty", 0));
        return record.getEnv().get("mfg.product_order").count(criteria);
    }

    @ServiceMethod(label = "加载制程订单", auth = "read")
    public Object loadCraftOrder(Records record, List<String> fields) {
        return record.getEnv().get("as.craft_order").find(Criteria.equal("product_order_id", record.getId())
            .and(Criteria.greater("to_plan_qty", 0))).read(fields);
    }

    @ServiceMethod(label = "生成计划单", auth = "read")
    public Object generateTask(Records records) {
        Map<String, Object> result = new KvMap();
        Map<String, List<String>> errors = new HashMap<>();
        result.put("errors", errors);
        List<Map<String, Object>> tasks = new ArrayList<>();
        result.put("tasks", tasks);
        Records orders = records.getEnv().get("as.craft_order").find(Criteria.in("product_order_id", records.getIds())
            .and(Criteria.greater("to_plan_qty", 0)));
        orders.set("generate_status", "new");
        Records settings = records.getEnv().get("as.user_settings").find(Criteria.equal("user_id", records.getEnv().getUserId()));
        Records permission = records.getEnv().get("as.scheduler_permission").find(Criteria.equal("scheduler_id.user_id", records.getEnv().getUserId())
            .and("scheduler_id.active", "=", true));
        Set<String> hideResource = SetUtils.hashSet(settings.getRec("hide_resource_ids").getIds());
        List<String> resourceIds = new ArrayList<>();
        for (Records row : permission) {
            String resourceId = row.getRec("resource_id").getId();
            if ("edit".equals(row.getString("permission")) && !hideResource.contains(resourceId)) {
                resourceIds.add(resourceId);
            }
        }
        // 模具任务
        Records moldings = orders.filter(o -> "molding".equals(o.getRec("craft_process_id")
            .getRec("craft_type_id").getString("algorithm")));
        generateMoldingTask(records, moldings, settings, resourceIds, tasks, errors);
        // 非模具任务
        Map<String, Records> crafts = new HashMap<>();
        for (Records row : orders) {
            if ("molding".equals(row.getRec("craft_process_id").getRec("craft_type_id").getString("algorithm"))) {
                continue;
            }
            String craftId = row.getRec("craft_process_id").getId();
            Records group = crafts.get(craftId);
            if (group == null) {
                group = row;
                crafts.put(craftId, group);
            } else {
                group.union(row);
            }
        }
        for (Records row : crafts.values()) {
            checkQuota(records, row, resourceIds, errors);
            tasks.addAll(splitOrder(records, settings, row.filter(r -> "new".equals(r.get("generate_status")))));
            tasks.addAll(margeOrder(records, settings, row.filter(r -> "new".equals(r.get("generate_status")))));
            Records toPlan = row.filter(r -> "new".equals(r.get("generate_status")));
            for (Records order : toPlan) {
                List<Map<String, Object>> details = new ArrayList<>();
                KvMap detail = createTaskDetail(records, order);
                details.add(detail);
                String code = order.getString("code");
                Records craftType = order.getRec("craft_process_id").getRec("craft_type_id");
                double qty = order.getDouble("to_plan_qty");
                KvMap task = createTask(records, code, craftType, qty, details);
                tasks.add(task);
            }
        }
        return result;
    }

    /**
     * 生成按模具算法的任务
     */
    public void generateMoldingTask(Records records, Records orders, Records settings, List<String> resourceIds,
                                    List<Map<String, Object>> tasks, Map<String, List<String>> errors) {
        Set<String> materialIds = new HashSet();
        for (Records row : orders) {
            Records material = row.getRec("product_id");
            materialIds.add(material.getId());
        }
        Records mainProducts = records.getEnv().get("md.sub_resource_product").find(Criteria.in("sub_ids.material_id", materialIds));
        Records molds = records.getEnv().get("md.sub_resource").find(Criteria.equal("usable", true).and(Criteria.in("product_ids.material_id", materialIds)
            .or(Criteria.in("product_ids", mainProducts.getIds()))));
        Set<String> moldModelIds = molds.stream().map(m -> m.getRec("model_id").getId()).collect(Collectors.toSet());
        Records moldModels = records.getEnv().get("md.sub_resource_model", moldModelIds);
        List<Map<String, Object>> moldModelResources = (List<Map<String, Object>>) moldModels.call("getModelResources");
        List<Map<String, Object>> moldProducts = new ArrayList<>();
        List<Map<String, Object>> multiProducts = new ArrayList<>();
        List<Map<String, Object>> singleProducts = new ArrayList<>();
        Map<String, Set<String>> craftMoldModels = new HashMap<>();
        for (Map<String, Object> row : moldModelResources) {
            String craftTypeId = (String) row.get("craft_type_id");
            Set<String> modelIds = craftMoldModels.get(craftTypeId);
            if (modelIds == null) {
                modelIds = new HashSet<>();
                craftMoldModels.put(craftTypeId, modelIds);
            }
            modelIds.add((String) row.get("model_id"));
        }
        for (Records mold : molds) {
            Records products = mold.getRec("product_ids");
            for (Records product : products) {
                Records subProducts = product.getRec("sub_ids");
                boolean multiProduct = product.getBoolean("multi_product");
                KvMap data = new KvMap()
                    .set("mold_id", mold.getId())
                    .set("mold_model_id", mold.getRec("model_id").getId())
                    .set("product_id", product.getRec("material_id").getId())
                    .set("qty", product.get("qty"))
                    .set("sub_ids", subProducts)
                    .set("count", subProducts.size());
                moldProducts.add(data);
                if (multiProduct) {
                    multiProducts.add(data);
                } else {
                    singleProducts.add(data);
                }
            }
        }

        for (Records order : orders) {
            Records productOrder = order.getRec("product_order_id");
            if (!moldModels.any()) {
                addError(productOrder.getId() + "|" + order.getId(), records.l10n("无可用的模具，请检查模具型号产品适配"), errors);
                order.set("generate_status", "error");
            } else {
                //一模多品
                if ("new".equals(order.getString("generate_status"))) {
                    generateMoldMultiProductTask(records, order, orders, craftMoldModels, moldModelResources, multiProducts,
                        settings, resourceIds, tasks, errors);
                }
                //专模
                if ("new".equals(order.getString("generate_status"))) {
                    generateMoldSingleProductTask(records, order, orders, craftMoldModels, moldModelResources, singleProducts,
                        settings, resourceIds, tasks, errors);
                }
            }
        }
    }

    /**
     * 生成共享模任务
     */
    public void generateMoldMultiProductTask(Records records, Records order, Records orders,
                                             Map<String, Set<String>> craftMoldModels,
                                             List<Map<String, Object>> moldModelResources,
                                             List<Map<String, Object>> multiProducts,
                                             Records settings, List<String> resourceIds,
                                             List<Map<String, Object>> tasks,
                                             Map<String, List<String>> errors) {
        String craftTypeId = order.getRec("craft_process_id").getRec("craft_type_id").getId();
        Set<String> moldIds = craftMoldModels.get(craftTypeId);
        if (Utils.isEmpty(moldIds)) {
            return;
        }
        List<Map<String, Object>> moldProducts = multiProducts;
        Records productOrder = order.getRec("product_order_id");
        Records factory = productOrder.getRec("factory_id");
        if (factory.any()) {
            //
        }
        Records product = order.getRec("product_id");
        Records newOrder = orders.filter(o -> "new".equals(o.getString("generate_status")) &&
            DateUtils.isSameDay(o.getRec("product_order_id").getDate("customer_due_date"), productOrder.getDate("customer_due_date")));
        List<Map<String, Object>> list = multiProducts.stream().filter(p -> moldIds.contains(p.get("mold_id"))
            && Utils.equals(product.getId(), p.get("product_id"))).collect(Collectors.toList());
        for (Map<String, Object> item : list) {

        }
    }

    /**
     * 生成专模任务
     */
    public void generateMoldSingleProductTask(Records records, Records order, Records orders,
                                              Map<String, Set<String>> craftMoldModels,
                                              List<Map<String, Object>> moldModelResources,
                                              List<Map<String, Object>> singleProducts,
                                              Records settings, List<String> resourceIds,
                                              List<Map<String, Object>> tasks,
                                              Map<String, List<String>> errors) {
        String craftTypeId = order.getRec("craft_process_id").getRec("craft_type_id").getId();
        Set<String> craftMoldModelIds = craftMoldModels.get(craftTypeId);
        if (Utils.isEmpty(craftMoldModelIds)) {
            return;
        }
        List<Map<String, Object>> molds = null;
        Records productOrder = order.getRec("product_order_id");
        Records product = order.getRec("product_id");
        Records factory = productOrder.getRec("factory_id");
        if (factory.any()) {
            // 指定工厂
            Records workshops = (Records) factory.call("findWorkshops");
            List<String> workshopIds = Utils.asList(workshops.getIds());
            Records resource = records.getEnv().get("md.work_resource", resourceIds)
                .filter(r -> workshopIds.contains(r.getRec("workshop_id").getId()));
            List<String> resourceIdList = Utils.asList(resource.getIds());
            List<String> moldIds = moldModelResources.stream().filter(r -> Utils.equals(r.get("craft_type_id"), craftTypeId)
                && resourceIdList.contains(r.get("work_resource_id"))).map(r -> (String) r.get("model_id")).collect(Collectors.toList());
            molds = singleProducts.stream().filter(r -> Utils.equals(product.getId(), r.get("product_id"))
                && moldIds.contains(r.get("mold_model_id"))).collect(Collectors.toList());
        } else {
            molds = singleProducts.stream().filter(r -> Utils.equals(product.getId(), r.get("product_id"))
                && craftMoldModelIds.contains(r.get("mold_model_id"))).collect(Collectors.toList());
        }
        if (Utils.isEmpty(molds)) {
            order.set("generate_status", "error");
            addError(productOrder.getId() + "|" + order.getId(), records.l10n("无可用的辅助资源(模具)，请检查辅助资源产品适配"), errors);
            return;
        }
        Set<String> moldIds = molds.stream().map(r -> (String) r.get("mold_id")).collect(Collectors.toSet());
        List<String> materialIds = molds.stream().map(m -> (String) m.get("product_id")).collect(Collectors.toList());
        Records toOrder = orders.filter(o -> "new".equals(o.getString("generate_status"))
            && materialIds.contains(o.getRec("product_id").getId()));
        if (factory.any()) {
            toOrder = toOrder.filter(o -> factory.equals(o.getRec("product_order_id").getRec("factory_id")));
        }
        boolean customer = settings.getBoolean("merge_customer");
        if (customer) {
            toOrder = toOrder.filter(o -> productOrder.getRec("customer_id")
                .equals(o.getRec("product_order_id").getRec("customer_id")));
        }
        boolean dueDate = settings.getBoolean("merge_factory_due_date");
        if (dueDate) {
            toOrder = toOrder.filter(o -> DateUtils.isSameDay(productOrder.getDate("factory_due_date"),
                o.getRec("product_order_id").getDate("factory_due_date")));
        }
        boolean materialSpec = settings.getBoolean("merge_material_spec");
        if (materialSpec) {
            toOrder = toOrder.filter(o -> Utils.equals(product.get("spec"), o.getRec("product_id").get("spec")));
        }
        List<Records> toOrderList = toOrder.stream().sorted(Comparator.comparing(x -> x.getRec("product_id").getId()))
            .sorted(Comparator.comparingDouble(x -> x.getDouble("to_plan_qty"))).collect(Collectors.toList());
        for (Records row : toOrderList) {
            //check quota
            tasks.addAll(margeMoldOrder(records, settings, toOrderList, molds));
            tasks.addAll(splitMoldOrder(records, settings, toOrderList, molds));
            if ("new".equals(row.get("generate_status"))) {
                Records craftType = row.getRec("craft_process_id").getRec("craft_type_id");
                List<Map<String, Object>> details = new ArrayList<>();
                KvMap detail = createTaskDetail(records, row);
                detail.put("is_main", false);
                //detail.put("multi_qty", molds.get(0).get("qty"));//TODO 检查是否正确
                detail.put("plan_qty", row.getDouble("to_plan_qty"));
                details.add(detail);
                KvMap task = createTask(records, row.getString("code"), craftType, row.getDouble("to_plan_qty"), details);
                Records mold = getMoldRandom(records, new ArrayList<>(moldIds));
                Records moldModel = mold.getRec("model_id");
                task.put("mold_model_id", moldModel.getId());
                task.put("mold_model", moldModel.get("present"));
                task.put("mold_id", mold.getId());
                task.put("mold_code", mold.get("code"));
                tasks.add(task);
                row.set("generate_status", "success");
            }
        }
    }

    public List<Map<String, Object>> splitMoldOrder(Records records, Records settings, List<Records> toPlan, List<Map<String, Object>> moldList) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        if (settings.getBoolean("split_craft_order")) {
            int maxQty = settings.getInteger("split_max_size");
            boolean merge = settings.getBoolean("merge_craft_order");
            int minQty = settings.getInteger("merge_min_size");
            toPlan = merge ? toPlan.stream().filter(o -> "new".equals(o.get("generate_status"))
                && Utils.large(o.getDouble("to_plan_qty"), minQty + maxQty)).collect(Collectors.toList()) :
                toPlan.stream().filter(o -> "new".equals(o.get("generate_status"))
                    && Utils.large(o.getDouble("to_plan_qty"), maxQty)).collect(Collectors.toList());
            List<String> code = new ArrayList<>();
            for (Records order : toPlan) {
                Records craftType = order.getRec("craft_process_id").getRec("craft_type_id");
                code.add(order.getString("code"));
                List<Map<String, Object>> molds = moldList.stream().filter(r -> Utils.equals(r.get("product_id"),
                    order.getRec("product_id").getId())).collect(Collectors.toList());
                double qty = order.getDouble("to_plan_qty");
                while (Utils.large(qty, 0d)) {
                    double planQty = Utils.largeOrEqual(qty, maxQty) ? maxQty : qty;
                    if (merge && Utils.lessOrEqual(qty, minQty + maxQty)) {
                        planQty = qty;
                    }
                    List<Map<String, Object>> details = new ArrayList<>();
                    KvMap detail = createTaskDetail(records, order);
                    detail.put("is_main", false);
                    //detail.put("multi_qty", molds.get(0).get("qty"));
                    detail.put("plan_qty", planQty);
                    details.add(detail);
                    KvMap task = createTask(records, Utils.join(code, ","), craftType, planQty, details);
                    Records mold = getMoldRandom(records, molds.stream().map(r -> (String) r.get("mold_id")).collect(Collectors.toList()));
                    Records moldModel = mold.getRec("model_id");
                    task.put("mold_model_id", moldModel.getId());
                    task.put("mold_model", moldModel.get("present"));
                    task.put("mold_id", mold.getId());
                    task.put("mold_code", mold.get("code"));
                    tasks.add(task);
                    qty = Utils.round(qty - planQty);
                }
                order.set("generate_status", "success");
            }
        }
        return tasks;
    }

    public List<Map<String, Object>> margeMoldOrder(Records records, Records settings, List<Records> toPlan, List<Map<String, Object>> moldList) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        if (settings.getBoolean("merge_craft_order")) {
            int minQty = settings.getInteger("merge_min_size");
            boolean split = settings.getBoolean("split_craft_order");
            int maxQty = settings.getInteger("split_max_size");
            toPlan = toPlan.stream().filter(o -> "new".equals(o.getString("generate_status"))
                && Utils.lessOrEqual(o.getDouble("to_plan_qty"), minQty)).collect(Collectors.toList());
            double qty = 0d;
            List<Map<String, Object>> details = new ArrayList<>();
            List<String> code = new ArrayList<>();
            Records craftType = null;
            for (Records order : toPlan) {
                if (craftType == null) {
                    craftType = order.getRec("craft_process_id").getRec("craft_type_id");
                }
                code.add(order.getString("code"));
                List<Map<String, Object>> molds = moldList.stream().filter(r -> Utils.equals(r.get("product_id"),
                    order.getRec("product_id").getId())).collect(Collectors.toList());
                KvMap detail = createTaskDetail(records, order);
                detail.put("is_main", false);
                //detail.put("multi_qty", molds.get(0).get("qty"));
                details.add(detail);
                qty = Utils.round(qty + order.getDouble("to_plan_qty"));
                if (split && Utils.large(qty, maxQty)) {
                    KvMap task = createTask(records, Utils.join(code, ","), craftType, qty, details);
                    Records mold = getMoldRandom(records, molds.stream().map(r -> (String) r.get("mold_id")).collect(Collectors.toList()));
                    Records moldModel = mold.getRec("model_id");
                    task.put("mold_model_id", moldModel.getId());
                    task.put("mold_model", moldModel.get("present"));
                    task.put("mold_id", mold.getId());
                    task.put("mold_code", mold.get("code"));
                    tasks.add(task);
                    details = new ArrayList<>();
                    qty = 0d;
                    code = new ArrayList<>();
                }
                order.set("generate_status", "success");
            }
            if (details.size() > 0) {
                KvMap task = createTask(records, Utils.join(code, ","), craftType, qty, details);
                Records mold = getMoldRandom(records, moldList.stream().map(r -> (String) r.get("mold_id")).collect(Collectors.toList()));
                Records moldModel = mold.getRec("model_id");
                task.put("mold_model_id", moldModel.getId());
                task.put("mold_model", moldModel.get("present"));
                task.put("mold_id", mold.getId());
                task.put("mold_code", mold.get("code"));
                tasks.add(task);
            }
        }
        return tasks;
    }

    public Records getMoldRandom(Records records, List<String> moldIds) {
        int index = 0;
        int size = moldIds.size();
        if (size > 1) {
            index = (int) Math.random() * size % size;
        }
        return records.getEnv().get("md.sub_resource", moldIds.get(index));
    }

    static void addError(String key, String error, Map<String, List<String>> errors) {
        List<String> list = errors.get(key);
        if (list == null) {
            list = new ArrayList<>();
            errors.put(key, list);
        }
        list.add(error);
    }

    /**
     * 拆分任务，任务数量不超过最大数量
     */
    public List<Map<String, Object>> splitOrder(Records records, Records settings, Records toPlan) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        if (settings.getBoolean("split_craft_order")) {
            int maxQty = settings.getInteger("split_max_size");
            boolean merge = settings.getBoolean("merge_craft_order");
            int minQty = settings.getInteger("merge_min_size");
            Records orders = merge ? toPlan.filter(r -> Utils.large(r.getDouble("to_plan_qty"), maxQty + minQty))
                : toPlan.filter(r -> Utils.large(r.getDouble("to_plan_qty"), maxQty));
            for (Records order : orders) {
                double qty = order.getDouble("to_plan_qty");
                while (Utils.large(qty, 0d)) {
                    double planQty = Utils.large(qty, maxQty) ? maxQty : qty;
                    if (merge && Utils.lessOrEqual(qty, maxQty + minQty)) {
                        planQty = qty;
                    }
                    qty = Utils.round(qty - planQty);
                    List<Map<String, Object>> details = new ArrayList<>();
                    KvMap detail = createTaskDetail(records, order);
                    detail.set("plan_qty", planQty);
                    details.add(detail);
                    String code = order.getString("code");
                    Records craftType = order.getRec("craft_process_id").getRec("craft_type_id");
                    KvMap task = createTask(records, code, craftType, planQty, details);
                    tasks.add(task);
                }
            }
            orders.set("generate_status", "success");
        }
        return tasks;
    }

    /**
     * 合并任务，任务数量不少于最小数量
     */
    public List<Map<String, Object>> margeOrder(Records records, Records settings, Records toPlan) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        if (settings.getBoolean("merge_craft_order")) {
            int minQty = settings.getInteger("merge_min_size");
            boolean customer = settings.getBoolean("merge_customer");
            boolean dueDate = settings.getBoolean("merge_factory_due_date");
            Map<String, Records> groups = new HashMap<>();
            Records orders = toPlan.filter(r -> Utils.lessOrEqual(r.getDouble("to_plan_qty"), minQty));
            for (Records row : orders) {
                String key = "";
                if (customer) {
                    key += row.getRec("product_order_id").getRec("customer_id").getId() + "|";
                }
                if (dueDate) {
                    key += Utils.format(row.getDate("factory_due_date"), "yyyyMMdd") + "|";
                }
                key += row.getRec("product_order_id").getRec("factory_id").getId();
                Records items = groups.get(key);
                if (items == null) {
                    groups.put(key, row);
                } else {
                    items.union(row);
                }
            }
            boolean split = settings.getBoolean("split_craft_order");
            int maxQty = settings.getInteger("split_max_size");
            for (Records group : groups.values()) {
                double qty = 0d;
                List<Map<String, Object>> details = new ArrayList<>();
                List<String> code = new ArrayList<>();
                Records craftType = group.first().getRec("craft_process_id").getRec("craft_type_id");
                List<Records> list = group.stream().sorted(Comparator.comparing(x -> x.getRec("product_order_id").getDate("factory_due_date")))
                    .sorted(Comparator.comparingDouble(x -> x.getDouble("to_plan_qty"))).collect(Collectors.toList());
                for (Records order : list) {
                    if (split && Utils.large(qty, maxQty)) {
                        KvMap task = createTask(records, Utils.join(code, ","), craftType, qty, details);
                        tasks.add(task);
                        details = new ArrayList<>();
                        qty = 0d;
                    }
                    code.add(order.getString("code"));
                    KvMap detail = createTaskDetail(records, order);
                    details.add(detail);
                    qty = Utils.round(qty + order.getDouble("to_plan_qty"));
                }
                KvMap task = createTask(records, Utils.join(code, ","), craftType, qty, details);
                tasks.add(task);
            }
            orders.set("generate_status", "success");
        }
        return tasks;
    }

    public KvMap createTask(Records records, String code, Records craftType, double qty, List<Map<String, Object>> details) {
        String factoryDueDate = details.stream().map(m -> (String) m.get("factory_due_date")).min(String::compareTo).get();
        String name = details.stream().map(m -> (String) m.get("material_code")).distinct().collect(Collectors.joining());
        KvMap map = new KvMap();
        map.put("id", IdWorker.nextId());
        map.put("code", code);
        map.put("name", name);
        map.put("status", "new");
        map.put("plan_qty", qty);
        map.put("craft_type_id", craftType.getId());
        map.put("craft_type", craftType.get("name"));
        map.put("algorithm", craftType.get("algorithm"));
        map.put("factory_due_date", factoryDueDate);
        map.put("details", details);
        return map;
    }

    public KvMap createTaskDetail(Records records, Records order) {
        Records material = order.getRec("product_id");
        Records productOrder = order.getRec("product_order_id");
        Records craftProcess = order.getRec("craft_process_id");
        Records factory = productOrder.getRec("factory_id");
        return new KvMap().set("craft_order_id", order.getId())
            .set("craft_order_code", order.get("code"))
            .set("order_qty", order.get("plan_qty"))
            .set("plan_qty", order.get("to_plan_qty"))
            .set("order_qty", productOrder.get("plan_qty"))
            .set("efficiency", 1)
            .set("material_id", material.getId())
            .set("material_code", material.get("code"))
            .set("material_name_spec", material.get("name_spec"))
            .set("material_ready_date", order.get("material_ready_date"))
            .set("factory_id", factory.getId())
            .set("factory", factory.get("present"))
            .set("factory_due_date", Utils.format(productOrder.getDate("factory_due_date"), "yyyy-MM-dd"))
            .set("transfer_time", order.get("transfer_time"))
            .set("product_order_id", productOrder.getId())
            .set("product_order_code", productOrder.get("code"))
            .set("craft_process_id", craftProcess.getId())
            .set("craft_process", craftProcess.get("name"))
            .set("next_order_id", order.getRec("next_order_id").getId())
            .set("next_relationship", order.get("next_relationship"));
    }

    /**
     * 检查工时定额
     */
    public void checkQuota(Records records, Records toPlan, List<String> resourceIds, Map<String, List<String>> errors) {
        Records craft = toPlan.first().getRec("craft_process_id");
        Records craftType = craft.getRec("craft_type_id");
        Records resource = records.getEnv().get("md.work_resource", resourceIds)
            .filter(r -> r.getRec("craft_type_id").equals(craftType));
        for (Records row : toPlan) {
            if (resource.any()) {
                Records orderQuota = row.getRec("quota_ids");
                orderQuota = orderQuota.filter(q -> resource.contains(q.getRec("resource_id")));
                if (orderQuota.any()) {
                    continue;
                }
                Records factory = row.getRec("product_order_id").getRec("factory_id");
                Records factoryResource = factory.any() ?
                    resource.filter(r -> Utils.equals(factory.getId(), r.getRec("workshop_id").getRec("parent_id").getId())) : resource;
                if (factoryResource.any()) {
                    continue;
                }
            }
            Records productOrder = row.getRec("product_order_id");
            List<String> list = errors.get(productOrder.getId() + "|" + row.getId());
            if (list == null) {
                list = new ArrayList<>();
                errors.put(productOrder.getId() + "|" + row.getId(), list);
            }
            list.add(records.l10n("制程[%s]没有可用工时定额", craftType.get("present")));
            row.set("generate_status", "error");
        }
    }
}
