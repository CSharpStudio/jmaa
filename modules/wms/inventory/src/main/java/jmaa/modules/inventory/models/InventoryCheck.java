package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.BinaryOp;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.tools.SpringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Model.Meta(name = "wms.inventory_check", label = "仓库盘点", inherit = {"mixin.order_status", "code.auto_code", "mixin.company"})
public class InventoryCheck extends Model {
    static Field code = Field.Char().label("盘点单号").readonly();
    static Field name = Field.Char().label("盘点名称").required();
    static Field blind = Field.Boolean().label("盲盘").defaultValue(false);
    static Field status = Field.Selection(new Options() {{
        put("draft", "草稿");
        put("commit", "已提交");
        put("approving", "审核中");
        put("approve", "已审核");
        put("reject", "驳回");
        put("first_running", "初盘中");
        put("first_done", "初盘完成");
        put("second_running", "复盘中");
        put("second_done", "复盘完成");
        put("done", "完成");
    }}).label("状态").defaultValue("draft").readonly(true).tracking();
    static Field remark = Field.Char().label("备注");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库编码").required();
    static Field store_area_ids = Field.Many2many("md.store_area", "wms_inventory_check_store_area", "inventory_check_id", "area_id").label("库区编码");
    static Field store_location_ids = Field.Many2many("md.store_location", "wms_inventory_check_store_location", "inventory_check_id", "location_id").label("库位编码");
    static Field abc_type = Field.Selection(Selection.method("getMaterialAbcType")).label("ABC分类");
    static Field category = Field.Selection(Selection.method("getMaterialCategory")).label("物料类型").help("物料类型字段");
    static Field second_flag = Field.Boolean().label("复盘").defaultValue(false);
    /*static Field second_ratio = Field.Float().label("复盘比例（%）").defaultValue(100);*/
    static Field line_ids = Field.One2many("wms.inventory_check_line", "inventory_check_id").label("盘点物料明细");
    static Field details_ids = Field.One2many("wms.inventory_check_details", "inventory_check_id").label("盘点标签明细");
    // 给复制用,如果是自动生成,则需要重新查询物料信息(可能有新增的物料) , 手动加,那就直接加现有物料,
    static Field create_type = Field.Boolean().label("创建类型").defaultValue(false).help("true:自动生成物料,false:手动添加物料");

    public Map<String, String> getMaterialCategory(Records records) {
        SelectionField sf = (SelectionField) records.getEnv().get("md.material").getMeta().getField("category");
        return sf.getOptions(records);
    }

    public Map<String, String> getMaterialAbcType(Records records) {
        SelectionField sf = (SelectionField) records.getEnv().get("md.material").getMeta().getField("abc_type");
        return sf.getOptions(records);
    }

    @ActionMethod
    public Action onWarehouseIdChange(Records record) {
        AttrAction action = Action.attr();
        action.setValue("store_area_ids", "");
        action.setValue("store_location_ids", "");
        return action;
    }

    @ActionMethod
    public Action onStoreAreaIdChange(Records record) {
        AttrAction action = Action.attr();
        action.setValue("store_location_ids", "");
        return action;
    }

    @Override
    public Map<String, Object> searchByField(Records rec, String relatedField, Criteria criteria, Integer offset, Integer limit, Collection<String> fields, String order) {
        // relatedField: "store_location_ids"
        if ("store_location_ids".equals(relatedField)) {
            if (criteria.hasField("area_id")) {
                for (Object item : criteria) {
                    BinaryOp binaryOp = (BinaryOp) item;
                    Object field = binaryOp.getField();
                    if ("area_id".equals(field)) {
                        Object value = binaryOp.getValue();
                        if (Utils.isEmpty(value)) {
                            criteria.remove(binaryOp);
                            break;
                        }
                    }
                }
            }
        }
        return (Map<String, Object>) callSuper(rec, relatedField, criteria, offset, limit, fields, order);
    }

    @Model.ServiceMethod(label = "提交", doc = "提交单据，状态改为已提交")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"draft".equals(orderStatus) && !"reject".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以提交", record.getSelection("status")));
            }
        }
        if (values != null) {
            records.update(values);
        }
        String body = records.l10n("提交") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "commit");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "审核", doc = "审核单据，从提交状态改为已审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "approving");
        ThreadPoolExecutor executor = SpringUtils.getBean(ThreadPoolExecutor.class);
        executor.execute(generateMaterialLine(records.getEnv(), records.getIds()));
        return Action.reload(records.l10n("操作成功"));
    }

    public Runnable generateMaterialLine(Environment environment, String[] inventoryCheckIds) {
        return () -> {
            try (Cursor cursor = environment.getDatabase().openCursor()) {
                Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                // 清空环境缓存
                env.getCache().invalidate();
                Records records = env.get("wms.inventory_check", inventoryCheckIds);
                for (Records record : records) {
                    // todo 提交的时候生成还是审核的时候生成,先写在提交
                    //Records inventoryCheckDetails = env.get("wms.inventory_check_details");
                    Records warehouseId = record.getRec("warehouse_id");
                    Records storeAreaIds = record.getRec("store_area_ids");
                    Records storeLocationIds = record.getRec("store_location_ids");
                    String abcType = record.getString("abc_type");
                    String category = record.getString("category");
                    Criteria criteria = Criteria.equal("warehouse_id", warehouseId.getId());
                    if (Utils.isNotEmpty(category)) {
                        criteria.and(Criteria.equal("material_id.category", category));
                    }
                    if (Utils.isNotEmpty(abcType)) {
                        criteria.and(Criteria.equal("material_id.abc_type", abcType));
                    }
                    if (storeLocationIds.any()) {
                        criteria.and(Criteria.in("location_id", storeLocationIds.getIds()));
                    }
                    if (!storeLocationIds.any() && storeAreaIds.any()) {
                        // 全部库位
                        Records location = env.get("md.store_location").find(Criteria.equal("warehouse_id", warehouseId.getId()).and(Criteria.in("area_id", storeAreaIds.getIds())));
                        criteria.and(Criteria.in("location_id", location.getIds()));
                    }
                    Records inventoryCheckLine = record.getRec("line_ids");
                    boolean flag = false;
                    if (inventoryCheckLine.any()) {
                        // 如果有数据,说明手动加的,只需要处理手动加的物料,
                        List<String> materialIdList = inventoryCheckLine.stream().map(e -> e.getRec("material_id").getId()).collect(Collectors.toList());
                        criteria.and(Criteria.in("material_id", materialIdList));
                    } else {
                        //如果没有数据,就是整个仓库都盘,直接全部都生成
                        flag = true;
                    }
                    record.set("create_type", flag);
                    // 批次,数量可能存在使用中的,有冻结数的就先不处理,不然后面还原的时候会出错
                    criteria.and(Criteria.equal("frozen_qty", 0));
                    // 这个条件是给批次和数量的
                    //criteria.and(Criteria.equal("allot_qty", 0));
                    criteria.and(Criteria.equal("status", "onhand"));
                    // 上面获取了所有的库存数据,状态是在库的,存在部分已分配等状态的不处理, 注意区分sn,lot,num,
                    // sn 需要加日志, 其他不管
                    // 先获取所有物料?生成盘点物料,再加处理标签?
                    // 先查物料,通过物料查库存,库存生成标签明细  查2次
                    // 查所有,生成标签明细,汇总物料,生成物料明细 查1次,内存操作
                    Records stockOnhand = env.get("stock.onhand").find(criteria);
                    List<Map<String, Object>> lineCreateList = Utils.asList();
                    //List<Map<String,Object>> detailsCreateList = Utils.asList();
                    Map<String, List<Records>> materialGroup = stockOnhand.stream().collect(Collectors.groupingBy(e -> e.getRec("material_id").getId()));
                    for (Map.Entry<String, List<Records>> entry : materialGroup.entrySet()) {
                        List<Records> valueList = entry.getValue();
                        Records value = valueList.get(0);
                        Records material = value.getRec("material_id");
                        String stockRule = material.getString("stock_rule");
                        double qty = 0d;
                        for (Records onhand : valueList) {
                            //Map<String,Object> detailMap = new HashMap<>();
                            if ("sn".equals(stockRule)) {
                                Records label = onhand.getRec("label_id");
                                // 标签盘点中
                                label.set("status", "checking");
                                Map<String, Object> log = new HashMap<>();
                                log.put("operation", "wms.inventory_check:frozen");
                                log.put("related_id", record.getId());
                                log.put("related_code", record.getString("code"));
                                label.call("logStatus", log);
                                //detailMap.put("label_id",label.getId());
                            }
                            // 库存 可用数 0 , 分配数  冻结
                            double usableQty = onhand.getDouble("usable_qty");
                            onhand.set("status", "frozen");
                            onhand.set("frozen_qty", usableQty);
                            onhand.set("ok_qty", Utils.round(onhand.getDouble("ok_qty") - usableQty));
                            onhand.set("usable_qty", 0d);
                            qty = Utils.round(qty + usableQty);
                        }
                        // 生成明细
                        if (flag) {
                            // 之前没有选物料
                            Map<String, Object> lineMap = new HashMap<>();
                            lineMap.put("inventory_check_id", record.getId());
                            lineMap.put("warehouse_id", warehouseId.getId());
                            lineMap.put("qty", qty);
                            lineMap.put("material_id", material.getId());
                            lineMap.put("status", "create");
                            lineCreateList.add(lineMap);
                        } else {
                            // 之前选了物料  只可能有1个
                            Records line = inventoryCheckLine.stream().filter(e -> e.getRec("material_id").getId().equals(material.getId())).findFirst().get();
                            line.set("qty", qty);
                        }
                    }
                    if (!lineCreateList.isEmpty()) {
                        inventoryCheckLine.createBatch(lineCreateList);
                    }
                    record.set("status", "approve");
                    env.get("base").flush();
                    cursor.commit();
                }
            }
        };
    }

    @ServiceMethod(label = "复制单据", doc = "复制单据物料明细")
    public void copyOrder(Records records) {
        for (Records record : records) {
            String status = record.getString("status");
            if ("draft".equals(status) || "commit".equals(status)) {
                throw new ValidationException("请复制已审核的数据");
            }
            boolean createType = record.getBoolean("create_type");
            //List<String> keyList = new ArrayList<>(record.getMeta().getFields().keySet());
            //List<String> keyList = Utils.asList("name", "blind", "warehouse_id", "abc_type", "category", "second_flag", "second_ratio", "remark", "create_type");
            List<String> keyList = Utils.asList("name", "blind", "warehouse_id", "abc_type", "category", "second_flag", "remark", "create_type");
            Map<String, Object> createMap = record.withContext("usePresent", false).read(keyList).get(0);
            createMap.put("name", createMap.get("name") + "-复制");
            createMap.put("remark", Utils.isNotEmpty(createMap.get("remark")) ? createMap.get("remark") + "-复制" : "复制");
            Records storeAreaIds = record.getRec("store_area_ids");
            Records storeLocationIds = record.getRec("store_location_ids");
            if (storeAreaIds.any()) {
                List<List<Object>> storeAreaList = Arrays.stream(storeAreaIds.getIds()).map(uid -> Utils.asList((Object) 4, uid)).collect(Collectors.toList());
                createMap.put("store_area_ids", storeAreaList);
            }
            if (storeLocationIds.any()) {
                List<List<Object>> storeLocationList = Arrays.stream(storeLocationIds.getIds()).map(uid -> Utils.asList((Object) 4, uid)).collect(Collectors.toList());
                createMap.put("store_location_ids", storeLocationList);
            }
            Records newOrder = record.create(createMap);
            if (!createType) {
                // 选过物料
                Records lineIds = record.getRec("line_ids");
                List<String> lineFieldList = new ArrayList<>(lineIds.getMeta().getFields().keySet());
                lineFieldList.remove("id");
                lineFieldList.remove("status");
                lineFieldList.remove("inventory_balance_id");
                Map<String, Object> stringObjectMap = lineIds.withContext("usePresent", false).read(lineFieldList).get(0);
                stringObjectMap.put("inventory_check_id", newOrder.getId());
                lineIds.create(stringObjectMap);
            }
        }
    }

    @ServiceMethod(label = "模拟测试用")
    public void generateData(Records records) {
        Environment env = records.getEnv();
        Records warehouse = env.get("md.warehouse").find(Criteria.equal("code", "01"));
        // 找物料
        Records materialAll = env.get("md.material").find(Criteria.in("code", Utils.asList("100110120003", "100110080002", "110700120001")));
        Records locationIds = env.get("md.store_location").find(Criteria.equal("warehouse_id", warehouse.getId()));
        Records template = env.get("print.template").find(Criteria.equal("category", "material_label")).first();
        Records supplier = env.get("md.supplier").find(Criteria.equal("code", "32138")).first();
        List<String> collect = locationIds.stream().map(Records::getId).collect(Collectors.toList());
        collect.add(null);
        int size = collect.size();
        Map<String, Object> map = new HashMap<>();
        map.put("supplier_id", supplier.getId());
        ThreadPoolExecutor executor = SpringUtils.getBean(ThreadPoolExecutor.class);
        for (Records material : materialAll) {
            String materialId = material.getId();
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                executor.execute(extractedSn(env, materialId, template.getId(), map, collect, size, warehouse.getId()));
            } else if ("lot".equals(stockRule)) {
                executor.execute(extractedLot(env, materialId, template.getId(), map, collect, size, warehouse.getId()));
            } else {
                executor.execute(extractedNum(env, materialId, template.getId(), map, collect, size, warehouse.getId()));
            }
        }
    }

    private Runnable extractedNum(Environment environment, String materialId, String templateId, Map<String, Object> map, List<String> collect, int size, String warehouseId) {
        return () -> {
            try (Cursor cursor = environment.getDatabase().openCursor()) {
                Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                Records stockOnhand = env.get("stock.onhand");
                Records warehouse = env.get("md.warehouse", warehouseId);
                Map<String, Object> call = (Map<String, Object>) env.get("lbl.material_label").call("printLabel", materialId, 100, 1000000, getDay(), "", "", templateId, "", map);
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) call.get("data");
                for (Map<String, Object> stringObjectMap : dataList) {
                    String locationId = collect.get((int) (Math.random() * size));
                    Records onhand = stockOnhand.find(Criteria.equal("warehouse_id", warehouseId)
                        .and(Criteria.equal("material_id", materialId))
                        .and(Criteria.equal("location_id", locationId)));
                    if (onhand.any()) {
                        // 相同批次号,相同库存,数量加
                        double round = Utils.round(onhand.getDouble("ok_qty") + Utils.toDouble(stringObjectMap.get("qty")));
                        onhand.set("ok_qty", round);
                        onhand.set("usable_qty", round);
                    } else {
                        Map<String, Object> onhandData = new HashMap<>();
                        onhandData.put("material_id", materialId);
                        onhandData.put("ok_qty", stringObjectMap.get("qty"));
                        onhandData.put("usable_qty", stringObjectMap.get("qty"));
                        onhandData.put("company_id", warehouse.getRec("company_id").getId());
                        onhandData.put("warehouse_id", warehouseId);
                        onhandData.put("location_id", locationId);
                        onhandData.put("stock_in_time", new Date());
                        onhandData.put("status", "onhand");
                        stockOnhand.create(onhandData);
                    }
                }
                env.get("base").flush();
                cursor.commit();
            }
        };
    }

    private Runnable extractedLot(Environment environment, String materialId, String templateId, Map<String, Object> map, List<String> collect, int size, String warehouseId) {
        return () -> {
            try (Cursor cursor = environment.getDatabase().openCursor()) {
                Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                Records stockOnhand = env.get("stock.onhand");
                Records warehouse = env.get("md.warehouse", warehouseId);
                Map<String, Object> call = (Map<String, Object>) env.get("lbl.material_label").call("printLabel", materialId, 100, 1000000, getDay(), "", "", templateId, "", map);
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) call.get("data");
                for (Map<String, Object> stringObjectMap : dataList) {
                    String locationId = collect.get((int) (Math.random() * size));
                    String lotNum = (String) stringObjectMap.get("lot_num");
                    Records onhand = stockOnhand.find(Criteria.equal("warehouse_id", warehouse.getId())
                        .and(Criteria.equal("lot_num", lotNum))
                        .and(Criteria.equal("material_id", materialId))
                        .and(Criteria.equal("location_id", locationId)));
                    if (onhand.any()) {
                        // 相同批次号,相同库存,数量加
                        double round = Utils.round(onhand.getDouble("ok_qty") + Utils.toDouble(stringObjectMap.get("qty")));
                        onhand.set("ok_qty", round);
                        onhand.set("usable_qty", round);
                    } else {
                        Map<String, Object> onhandData = new HashMap<>();
                        onhandData.put("material_id", materialId);
                        onhandData.put("ok_qty", stringObjectMap.get("qty"));
                        onhandData.put("lot_num", stringObjectMap.get("lot_num"));
                        onhandData.put("usable_qty", stringObjectMap.get("qty"));
                        onhandData.put("company_id", warehouse.getRec("company_id").getId());
                        onhandData.put("warehouse_id", warehouse.getId());
                        onhandData.put("location_id", locationId);
                        onhandData.put("stock_in_time", new Date());
                        onhandData.put("status", "onhand");
                        stockOnhand.create(onhandData);
                    }
                }
                env.get("base").flush();
                cursor.commit();
            }
        };
    }

    private Runnable extractedSn(Environment environment, String materialId, String templateId, Map<String, Object> map, List<String> collect, int size, String warehouseId) {
        return () -> {
            try (Cursor cursor = environment.getDatabase().openCursor()) {
                Environment env = new Environment(environment.getRegistry(), cursor, environment.getUserId(), environment.getContext());
                Records stockOnhand = env.get("stock.onhand");
                Records warehouse = env.get("md.warehouse", warehouseId);
                Records materialLabel = env.get("lbl.material_label");
                Map<String, Object> call = (Map<String, Object>) env.get("lbl.material_label").call("printLabel", materialId, 100, 1000000, getDay(), "", "", templateId, "", map);
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) call.get("data");
                for (Map<String, Object> stringObjectMap : dataList) {
                    String locationId = collect.get((int) (Math.random() * size));
                    String sn = (String) stringObjectMap.get("sn");
                    Records label = materialLabel.find(Criteria.equal("sn", sn));
                    Map<String, Object> onhandData = new HashMap<>();
                    onhandData.put("material_id", materialId);
                    onhandData.put("ok_qty", stringObjectMap.get("qty"));
                    onhandData.put("usable_qty", stringObjectMap.get("qty"));
                    onhandData.put("sn", sn);
                    onhandData.put("lot_num", stringObjectMap.get("lot_num"));
                    onhandData.put("company_id", warehouse.getRec("company_id").getId());
                    onhandData.put("label_id", label.getId());
                    onhandData.put("warehouse_id", warehouse.getId());
                    onhandData.put("location_id", locationId);
                    onhandData.put("stock_in_time", new Date());
                    onhandData.put("status", "onhand");
                    stockOnhand.create(onhandData);
                }
                env.get("base").flush();
                cursor.commit();
            }
        };
    }

    public Date getDay() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        // 创建YearMonth对象，用于获取该月的天数
        YearMonth yearMonth = YearMonth.of(year, month);
        int lengthOfMonth = yearMonth.lengthOfMonth(); // 获取该月的天数
        // 创建Random对象
        Random random = new Random();
        // 随机生成1到lengthOfMonth之间的整数（包括两端）作为日数
        int randomDay = random.nextInt(lengthOfMonth) + 1;
        // 创建LocalDate对象，表示本月的某一天
        return new Date(year, month, randomDay);
    }
}

