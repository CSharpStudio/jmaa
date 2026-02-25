package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.ArrayUtils;
import org.apache.commons.collections4.SetUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_delivery", label = "销售发货", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class SalesDelivery extends Model {
    static Field code = Field.Char().label("销售发货单号").index(true).readonly();
    static Field related_code = Field.Char().label("相关单据").index(true);
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("发货仓库").lookup("searchWarehouse").required();

    /**
     * 带出当前用户有权限的仓库
     */
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    static Field delivery_date = Field.Date().label("发货日期");
    static Field remark = Field.Char().label("备注");
    static Field line_ids = Field.One2many("wms.sales_delivery_line", "delivery_id").label("物料列表");
    static Field details_ids = Field.One2many("wms.sales_delivery_details", "delivery_id").label("发货明细");

    @ActionMethod
    public Action onRelatedCodeChange(Records rec) {
        AttrAction action = Action.attr();
        Records order = rec.getEnv().get("sales.order").find(Criteria.equal("code", rec.getString("related_code")));
        if (order.any()) {
            // 订单类型为项目类型才带出来
            Records customer = order.getRec("customer_id");
            action.setValue("customer_id", customer);
        }
        return action;
    }

    @Model.ServiceMethod(label = "审核", doc = "审核单据，从提交状态改为已审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
            // 审核的时候,回写销售订单可建单数,
            String relatedCode = record.getString("related_code");
            Records salesOrder = record.getEnv().get("sales.order").find(Criteria.equal("code", relatedCode));
            if (salesOrder.any()) {
                Records deliveryLine = record.getRec("line_ids");
                for (Records line : deliveryLine) {
                    Records material = line.getRec("material_id");
                    // 预发数
                    double requestQty = line.getDouble("request_qty");
                    Records salesOrderLine = record.getEnv().get("sales.order_line")
                        .find(Criteria.equal("so_id", salesOrder.getId()).and(Criteria.equal("material_id", material.getId())));
                    // 这种不考虑有多条的情况了,
                    salesOrderLine.ensureOne();
                    double uncommit_qty = salesOrderLine.getDouble("uncommit_qty");
                    salesOrderLine.set("uncommit_qty", Utils.round(uncommit_qty - requestQty));
                }
            }
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "approve");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "驳回", doc = "驳回单据，从提交或审核状态改为驳回")
    public Object reject(Records records, @Doc(doc = "驳回原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus) && !"approve".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以驳回", record.getSelection("status")));
            }
            Records details = record.getEnv().get("wms.sales_delivery_details").find(Criteria.equal("delivery_id", record.getId()));
            if (details.any()) {
                throw new ValidationException(record.l10n("存在发货明细数据,不能驳回"));
            }
            // 驳回的时候,回写销售订单可建单数,
            String relatedCode = record.getString("related_code");
            Records salesOrder = record.getEnv().get("sales.order").find(Criteria.equal("code", relatedCode));
            if (salesOrder.any()) {
                Records deliveryLine = record.getRec("line_ids");
                for (Records line : deliveryLine) {
                    Records material = line.getRec("material_id");
                    // 预发数
                    double requestQty = line.getDouble("request_qty");
                    Records salesOrderLine = record.getEnv().get("sales.order_line")
                        .find(Criteria.equal("so_id", salesOrder.getId()).and(Criteria.equal("material_id", material.getId())));
                    // 这种不考虑有多条的情况了,
                    salesOrderLine.ensureOne();
                    double uncommit_qty = salesOrderLine.getDouble("uncommit_qty");
                    salesOrderLine.set("uncommit_qty", Utils.round(uncommit_qty + requestQty));
                }
            }
        }
        String body = records.l10n("驳回") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "reject");
        return Action.reload(records.l10n("操作成功"));
    }

    @ServiceMethod(auth = "read", label = "查找相关单据")
    public Map<String, Object> searchRelatedCode(Records record,
                                                 @Doc(doc = "查询条件") Criteria criteria,
                                                 @Doc(doc = "偏移量") Integer offset,
                                                 @Doc(doc = "行数") Integer limit,
                                                 @Doc(doc = "排序") String order) {
        //相关单据没有Many2one引用关系，保存present字段的值
        criteria.and(Criteria.equal("status", "approve"))
            .and(Criteria.equal("company_id", record.getEnv().getCompany().getId()));
        Map<String, Object> data = record.getEnv().get("sales.order").searchLimit(Arrays.asList("present"), criteria, offset, limit, order);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("values");
        for (Map<String, Object> row : rows) {
            row.put("id", row.get("present"));
        }
        return data;
    }

    @ServiceMethod(auth = "read", label = "查找销售订单行")
    public List<Map<String, Object>> querySoLine(Records records, String poCode, List<String> fields, String order) {
        Criteria criteria = Criteria.equal("so_id.code", poCode).and("uncommit_qty", ">", 0);
        return records.getEnv().get("sales.order_line").search(fields, criteria);
    }

    @ServiceMethod(label = "读取发料信息", auth = "issue")
    public Map<String, Object> readDeliveryMaterial(Records record, @Doc("物料") String materialId) {
        Criteria statusCriteria = Criteria.in("status", Arrays.asList("new", "delivering"));
        Criteria criteria = Criteria.equal("delivery_id", record.getId()).and(statusCriteria);
        if (Utils.isNotEmpty(materialId)) {
            criteria.and("material_id", ">", materialId);
        }
        Records line = record.getEnv().get("wms.sales_delivery_line");
        line = line.find(criteria, 0, 1, "material_id");
        if (!line.any()) {
            line = line.find(Criteria.equal("delivery_id", record.getId()).and(statusCriteria), 0, 1, "material_id");
        }
        if (!line.any()) {
            throw new ValidationException("没有待发物料");
        }
        return getDeliveryMaterial(record, line);
    }

    public Map<String, Object> getDeliveryMaterial(Records record, Records line) {
        Double requestQty = line.getDouble("request_qty");
        Double deliveredQty = line.getDouble("delivered_qty");
        Double deficitQty = Math.max(0, Utils.round(requestQty - deliveredQty));
        Records material = line.getRec("material_id");
        Records unit = material.getRec("unit_id");
        Map<String, Object> data = new HashMap<>();
        data.put("material_id", material.getPresent());
        data.put("request_qty", requestQty);
        data.put("deficit_qty", deficitQty);
        data.put("material_name_spec", material.get("name_spec"));
        data.put("material_category", material.get("category"));
        data.put("stock_rule", material.get("stock_rule"));
        data.put("unit_id", unit.getPresent());
        data.put("unit_accuracy", unit.getInteger("accuracy"));
        Set<String> warehouseIds = SetUtils.hashSet(record.getEnv().getUser().getRec("warehouse_ids").getIds());
        Records warehouses = record.getRec("warehouse_id").filter(r -> warehouseIds.contains(r.getId()));
        if (warehouses.any()) {
            Cursor cr = record.getEnv().getCursor();
            String sql = "select sum(ok_qty-allot_qty),warehouse_id from stock_onhand where material_id=%s and warehouse_id in %s group by warehouse_id";
            cr.execute(sql, Arrays.asList(material.getId(), warehouses.getIds()));
            for (Object[] row : cr.fetchAll()) {
                Double onhandQty = Utils.toDouble(row[0]);
                if (Utils.largeOrEqual(onhandQty, deficitQty)) {
                    data.put("onhand_qty", onhandQty);
                    data.put("warehouse_id", warehouses.browse(Utils.toString(row[1])).getPresent());
                    break;
                }
            }
        }
        return data;
    }

    @ServiceMethod(label = "拆分标签", doc = "拆分生成新标签")
    public Object splitLabel(Records record, @Doc("条码") String sn, @Doc("拆分数量") Double splitQty, @Doc("是否打印原标签") Boolean printOld) {
        Records newLabel = (Records) record.getEnv().get("lbl.product_label").call("splitLabel", sn, splitQty);
        Records printTemplate = newLabel.getRec("print_template_id");
        List<String> labelIds = new ArrayList<>();
        if (printOld) {
            Records label = newLabel.find(Criteria.equal("sn", sn));
            labelIds.add(label.getId());
        }
        labelIds.add(newLabel.getId());
        Map<String, Object> printData = (Map<String, Object>) printTemplate.call("print", new HashMap<String, Object>() {{
            put("labels", newLabel.browse(labelIds));
        }});
        printData.put("newSn", newLabel.get("sn"));
        return printData;
    }

    @ServiceMethod(label = "发货")
    public List<Map<String, Object>> deliver(Records records, String poCode, List<String> fields, String order) {
        Criteria criteria = Criteria.equal("so_id.code", poCode).and("uncommit_qty", ">", 0);
        return records.getEnv().get("sales.order_line").search(fields, criteria);
    }

    @ServiceMethod(label = "出库", doc = "根据发货明细生成出库单")
    public Object stockOut(Records records) {
        for (Records record : records) {
            Records details = record.getEnv().get("wms.sales_delivery_details").find(Criteria.equal("delivery_id", record.getId()).and("stock_out_id", "=", null));
            if (!details.any()) {
                throw new ValidationException("没有可出库物料");
            }
            Set<String> materialIds = new HashSet<>();
            for (Records detail : details) {
                materialIds.add(detail.getRec("material_id").getId());
            }
            Records lines = record.getEnv().get("wms.sales_delivery_line").find(
                Criteria.equal("delivery_id", record.getId()).and("material_id", "in", materialIds).and("status", "=", "delivered"));
            lines.set("status", "done");
            //创建出库单
            Records stockOut = record.getEnv().get("stock.stock_out");
            Map<String, Object> data = new HashMap<>();
            data.put("type", "sales-out");
            data.put("related_code", record.get("code"));
            data.put("related_model", record.getMeta().getName());
            data.put("related_id", record.getId());
            stockOut = stockOut.create(data);
            details.set("stock_out_id", stockOut.getId());
            stockOut.call("stockOut");
            //记录消息
            String message = record.l10n("生成出库单: %s", stockOut.get("code"));
            record.call("trackMessage", message);
            // 处理销售订单行数量
            // 销售订单号
            String relatedCode = record.getString("related_code");
            for (Records line : lines) {
                Records material = line.getRec("material_id");
                Records salesOrderLine = record.getEnv().get("sales.order_line").find(Criteria.equal("so_id.code", relatedCode).and(Criteria.equal("material_id", material.getId())));
                salesOrderLine.set("delivery_qty", Utils.round(salesOrderLine.getDouble("delivery_qty") + line.getDouble("delivered_qty")));
            }
        }
        updateDeliveryStatus(records);
        return Action.reload(records.l10n("出库成功"));
    }

    public void updateDeliveryStatus(Records records) {
        //执行sql前flush保存
        records.flush();
        Cursor cr = records.getEnv().getCursor();
        String sql = "select distinct status from wms_sales_delivery_line where delivery_id=%s";
        for (Records record : records) {
            cr.execute(sql, Arrays.asList(record.getId()));
            boolean done = cr.fetchAll().stream().map(r -> (String) r[0]).allMatch("done"::equals);
            if (done) {
                // 如果全部为完成状态，则更新为已完成状态
                record.set("status", "done");
                break;
            }
        }
    }

    @ServiceMethod(label = "扫码")
    public Object scanCode(Records record, String code, String warehouseId, Boolean autoConfirm) {
        Map<String, Object> resultMap = new HashMap<>();
        // 销售发货,现在暂时只管成品/包装 标签, 其他都先不管,待定  todo
        Environment env = record.getEnv();
        Records deliveryLine = record.getRec("line_ids");
        List<String> materialIdList = deliveryLine.stream().map(e -> e.getRec("material_id").getId()).collect(Collectors.toList());
        Records mdPackage = null;
        if (env.getRegistry().contains("packing.package")) {
            mdPackage = env.get("packing.package");
        }
        // 销售出库都是成品标签,暂时不支持非成品物料, 成品物料都是使用lbl_material_label表存数据
        Records label = env.get("lbl.material_label").find(Criteria.equal("sn", code));
        Records material = null;
        double qty = 0d;
        if (label.any()) {
            // 校验当前物料对不对
            material = label.getRec("material_id");
            qty = label.getDouble("qty");
        } else if (null != mdPackage && (mdPackage = mdPackage.find(Criteria.equal("code", code))).any()) {
            material = mdPackage.getRec("material_id");
            qty = mdPackage.getDouble("package_qty");
            if (mdPackage.getRec("parent_id").any()) {
                throw new ValidationException(record.l10n("包装标签请扫描最外箱标签码"));
            }
        } else {
            throw new ValidationException(record.l10n("标签[%s]无法识别,请检查标签数据", code));
        }
        String materialId = material.getId();
        if (!materialIdList.contains(materialId)) {
            throw new ValidationException(record.l10n("条码[%s]非当前单据所需物料", code));
        }
        // 查库存
        Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("sn", code).and(Criteria.equal("material_id", material.getId()))
            .and(Criteria.equal("warehouse_id", warehouseId)));
        if (!stockOnhand.any()) {
            throw new ValidationException(record.l10n("当前标签[%s],在仓库[%s],无库存数据",
                code, env.get("md.warehouse", warehouseId).getString("present")));
        }
        if (!"onhand".equals(stockOnhand.getString("status"))) {
            throw new ValidationException(record.l10n("当前标签[%s],库存状态[%s],不能使用", code, stockOnhand.getSelection("status")));
        }
        Records line = deliveryLine.find(Criteria.equal("material_id", materialId).and(Criteria.equal("delivery_id", record.getId())));
        // 预发数
        double requestQty = line.getDouble("request_qty");
        // 实发数
        double deliveredQty = line.getDouble("delivered_qty");
        double operableQty = Utils.round(requestQty - deliveredQty);
        if (Utils.less(operableQty, qty)) {
            if (mdPackage != null && mdPackage.any()) {
                throw new ValidationException(record.l10n("当前包装标签数量大于剩余可发数"));
            }
            Map<String, Object> splitData = new HashMap<>();
            splitData.put("sn", code);
            splitData.put("qty", qty);
            splitData.put("deficit_qty", operableQty);
            splitData.put("split_qty", operableQty);
            resultMap.put("split", splitData);
            resultMap.put("action", "split");
            resultMap.put("message", record.l10n("条码[%s]识别成功,需拆分", code));
        } else {
            if (Utils.toBoolean(autoConfirm)) {
                Map<String, Object> resultData = (Map<String, Object>) record.call("deliveryMaterial", code, warehouseId, stockOnhand.getRec("location_id").getId());
                resultData.put("message", record.l10n("条码[%s]识别使用成功", code));
                return resultData;
            }
            resultMap.put("message", record.l10n("条码[%s]识别成功", code));
        }
        Map<String, Object> materialByWarehouse = getMaterialByWarehouse(record, material, env.get("md.warehouse", warehouseId), operableQty, code, stockOnhand);
        materialByWarehouse.put("commit_qty", qty);
        resultMap.put("data", materialByWarehouse);
        return resultMap;
    }

    @ServiceMethod(label = "扫码确认", doc = "扫码以后确认功能")
    public Object deliveryMaterial(Records record, String code, String warehouseId, String locationId) {
        if (Utils.isBlank(warehouseId)) {
            throw new ValidationException(record.l10n("请选择入库仓库"));
        }
        Environment env = record.getEnv();
        Records mdPackage = null;
        if (env.getRegistry().contains("packing.package")) {
            mdPackage = env.get("packing.package");
        }
        Records stockOnhand = env.get("stock.onhand");
        Records salesDeliveryDetails = record.getEnv().get("wms.sales_delivery_details");
        // lbl.product_label = lbl.material_label
        Records materialLabel = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", code));
        Map<String, Object> resultData = new HashMap<>();
        double qty = 0d;
        Records material = null;
        if (materialLabel.any()) {
            if (salesDeliveryDetails.find(Criteria.equal("delivery_id", record.getId()).and(Criteria.equal("sn", code))).any()) {
                throw new ValidationException(record.l10n("当前标签已使用,请检查标签数据"));
            }
            // 处理标签数据和标签日志
            material = materialLabel.getRec("material_id");
            materialLabel.set("status", "allot");
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.sales_delivery");
            log.put("related_id", record.getId());
            log.put("related_code", record.getString("code"));
            materialLabel.call("logStatus", log);
            // 标签数据处理完毕
            // 处理发货明细
            Map<String, Object> createMap = new HashMap<>();
            createMap.put("material_id", material.getId());
            createMap.put("qty", qty = materialLabel.getDouble("qty"));
            createMap.put("warehouse_id", warehouseId);
            createMap.put("location_id", locationId);
            createMap.put("status", "new");
            createMap.put("delivery_id", record.getId());
            createMap.put("lot_num", materialLabel.getString("lot_num"));
            createMap.put("label_id", materialLabel.getId());
            salesDeliveryDetails.create(createMap);
            resultData.put("message", record.l10n("条码[%s]使用成功", code));
        } else if (null != mdPackage && (mdPackage = mdPackage.find(Criteria.equal("code", code))).any()) {
            material = mdPackage.getRec("material_id");
            Map<String, Object> createMap = new HashMap<>();
            createMap.put("material_id", material.getId());
            createMap.put("qty", qty = mdPackage.getDouble("package_qty"));
            createMap.put("warehouse_id", warehouseId);
            createMap.put("location_id", locationId);
            createMap.put("status", "new");
            createMap.put("delivery_id", record.getId());
            createMap.put("sn", code);
            salesDeliveryDetails.create(createMap);
            resultData.put("message", record.l10n("条码[%s]使用成功", code));
        } else {
            // 这种基本不可能, 除非,扫码,确定之前,改了条码(误触)并且没有回车
            throw new ValidationException(record.l10n("标签无法识别,请检查标签数据"));
        }
        // 处理库存数据
        stockOnhand = stockOnhand.find(Criteria.equal("material_id", material.getId()).and(Criteria.equal("sn", code))
            .and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId)));
        // 这里多数情况下不会进来,因为扫码的时候已经处理过了, 可能存在扫描以后,自己改标签数据,误触等等, 控制一下,避免异常
        if (!stockOnhand.any()) {
            throw new ValidationException(record.l10n("当前标签[%s],在仓库[%s] , 库位[%s]中,无库存数据",
                code, env.get("md.warehouse", warehouseId).getString("present"), Utils.isNotBlank(locationId) ? env.get("md.store_location", locationId).get("present") : ""));
        }
        if (!"onhand".equals(stockOnhand.getString("status"))) {
            throw new ValidationException(record.l10n("当前标签[%s],库存状态[%s],不能使用", code, stockOnhand.getSelection("status")));
        }
        stockOnhand.set("status", "allot");
        // 这种标签管理的,就直接改0了
        //stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") - stockOnhand.getDouble("ok_qty")));
        // 可用数
        stockOnhand.set("usable_qty", 0);
        // 分配数
        stockOnhand.set("allot_qty", stockOnhand.getDouble("ok_qty"));
        // 库存处理完毕,
        if ("draft".equals(record.getString("status"))) {
            record.set("status", "stocking");
        }
        Records line = env.get("wms.sales_delivery_line").find(Criteria.equal("delivery_id", record.getId()).and(Criteria.equal("material_id", material.getId())));
        double deliveredQty = Utils.round(line.getDouble("delivered_qty") + qty);
        line.set("delivered_qty", deliveredQty);
        String status = line.getString("status");
        if ("new".equals(status)) {
            line.set("status", "delivering");
        }
        double requestQty = line.getDouble("request_qty");
        if (Utils.equals(requestQty, deliveredQty)) {
            line.set("status", "delivered");
        }
        double toDeliveryQty = Utils.round(line.getDouble("request_qty") - line.getDouble("delivered_qty"));
        resultData.put("data", getMaterialByWarehouse(record, material, env.get("md.warehouse", warehouseId), toDeliveryQty, code, stockOnhand));
        return resultData;
    }

    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待调拨物料，根据出库规则推荐物料标签")
    public Object loadDeliveryMaterial(Records record,
                                       @Doc("仓库") String warehouseId,
                                       @Doc("偏移量") Integer offset) {
        Environment env = record.getEnv();
        Records lines = env.get("wms.sales_delivery_line").find(Criteria.equal("delivery_id", record.getId())
            .and("status", "not in", Utils.asList("done", "delivered")));
        if (!lines.any()) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", record.getString("status"));
            resultMap.put("id", record.getId());
            return resultMap;
        }
        Records line = lines.browse();
        Records warehouse = env.get("md.warehouse", warehouseId);
        Records material = env.get("md.material");
        List<String> materialIds = lines.stream().map(l -> l.getRec("material_id").getId()).collect(Collectors.toList());
        String[] onhandMaterial = null;
        if (Utils.isNotEmpty(warehouseId)) {
            onhandMaterial = findOnhandMaterial(record, materialIds, warehouseId, offset);
        }
        if (Utils.isEmpty(onhandMaterial)) {
            onhandMaterial = findOnhandMaterial(record, materialIds, null, offset);
        }
        if (Utils.isNotEmpty(onhandMaterial)) {
            String materialId = onhandMaterial[0];
            line = lines.filter(l -> Utils.equals(materialId, l.getRec("material_id").getId()));
            warehouse = warehouse.browse(onhandMaterial[1]);
            material = line.getRec("material_id");
        }
        if (!line.any()) {
            int idx = Utils.toInt(offset) % lines.size();
            if (idx < 0) {
                idx += lines.size();
            }
            line = lines.browse(lines.getIds()[idx]);
            material = line.getRec("material_id");
        }
        double toSalesDeliveryQty = Utils.round(line.getDouble("request_qty") - line.getDouble("delivered_qty"));
        return getMaterialByWarehouse(record, material, warehouse, toSalesDeliveryQty, null, null);
    }

    public String[] findOnhandMaterial(Records record, List<String> materialIds, String warehouseId, int offset) {
        Cursor cr = record.getEnv().getCursor();
        List<Object> params = new ArrayList<>();
        params.add(materialIds);
        String sql = "select distinct o.material_id,o.warehouse_id,l.code from stock_onhand o left join md_store_location l on o.location_id=l.id"
            + " where o.status='onhand' and o.material_id in %s and usable_qty > 0";
        if (Utils.isNotEmpty(warehouseId)) {
            sql += " and o.warehouse_id=%s order by l.code";
            params.add(warehouseId);
        } else {
            sql += " and o.warehouse_id in %s order by l.code";
            Records warehouses = record.getRec("warehouse_id");
            params.add(warehouses.getIds());
        }
        cr.execute(sql, params);
        List<Object[]> rows = cr.fetchAll();
        if (!rows.isEmpty()) {
            Set<String> set = new LinkedHashSet<>();
            set.addAll(rows.stream().map(r -> r[0] + "," + r[1]).collect(Collectors.toList()));
            int idx = Utils.toInt(offset) % set.size();
            if (idx < 0) {
                idx += set.size();
            }
            return set.toArray(ArrayUtils.EMPTY_STRING_ARRAY)[idx].split(",");
        }
        return null;
    }

    public Map<String, Object> getMaterialByWarehouse(Records record, Records material, Records warehouse, double toSalesDeliveryQty, String code, Records stockOnhand) {
        Environment env = record.getEnv();
        Records onhand = env.get("stock.onhand");
        if (warehouse.any() && Utils.large(toSalesDeliveryQty, 0)) {
            onhand = findOnhandByRule(record, material, warehouse);
        }
        return getReturnMaterialByOnhand(record, material, warehouse, onhand, toSalesDeliveryQty, code, stockOnhand);
    }

    public Records findOnhandByRule(Records record, Records material, Records warehouse) {
        Environment env = record.getEnv();
        Records onhand = env.get("stock.onhand");
        Cursor cr = env.getCursor();
        String rule = warehouse.getString("stock_out_rule");
        String column = env.getConfig().getString("fifo_date");
        String stockRule = material.getString("stock_rule");
        String order = ("product_date".equals(column) && "sn".equals(stockRule) ? " order by l.product_date" : " order by s.stock_in_time")
            + ("lifo".equals(rule) ? " desc" : " asc");
        String sql = null;
        if ("sn".equals(stockRule)) {
            sql = "select s.id from stock_onhand s join lbl_material_label l on s.label_id=l.id " +
                "where s.material_id=%s and s.warehouse_id=%s and s.status='onhand' and s.usable_qty > 0";
        } else {
            sql = "select s.id from stock_onhand s " +
                "where s.material_id=%s and s.warehouse_id=%s and s.status='onhand'  and s.usable_qty > 0";
        }
        if (warehouse.getBoolean("bulk_fo")) {
            String bulkSql = sql;
            if ("sn".equals(stockRule)) {
                bulkSql = bulkSql + " and l.qty<l.original_qty" + order;
            }
            cr.execute(cr.getSqlDialect().getPaging(bulkSql, 1, 0), Utils.asList(material.getId(), warehouse.getId()));
            if (cr.getRowCount() > 0) {
                return onhand.browse((String) cr.fetchOne()[0]);
            }
        }
        cr.execute(cr.getSqlDialect().getPaging(sql + order, 1, 0), Utils.asList(material.getId(), warehouse.getId()));
        if (cr.getRowCount() > 0) {
            onhand = onhand.browse((String) cr.fetchOne()[0]);
        }
        return onhand;
    }

    public Map<String, Object> getReturnMaterialByOnhand(Records record, Records material, Records warehouse, Records warehouseOnhand, double toSalesDeliveryQty, String code, Records stockOnhand) {
        Map<String, Object> result = new HashMap<>();
        String stockRule = material.getString("stock_rule");
        Records unit = material.getRec("unit_id");
        result.put("id", record.getId());
        result.put("status", record.get("status"));
        result.put("suggest_location", warehouseOnhand.getRec("location_id").get("code"));
        result.put("material_id", material.getPresent());
        result.put("material_name_spec", material.get("name_spec"));
        result.put("deficit_qty", toSalesDeliveryQty);
        result.put("warehouse_id", warehouse.getPresent());
        result.put("stock_rule", material.get("stock_rule"));
        result.put("unit_id", unit.getPresent());
        result.put("unit_accuracy", unit.get("accuracy"));
        if (Utils.isNotEmpty(code)) {
            // 不确定后面会不会有批次标签,库位暂时保留 todo
            result.put("location_id", null != stockOnhand ? stockOnhand.getRec("location_id").getId() : "");
        }
        if ("sn".equals(stockRule)) {
            result.put("suggest_sn", warehouseOnhand.get("sn"));
        } else {
            result.put("suggest_sn", warehouseOnhand.get("lot_num"));
        }
        if (warehouse.any()) {
            Cursor cr = record.getEnv().getCursor();
            cr.execute("select sum(usable_qty) from stock_onhand where status='onhand' and material_id=%s and warehouse_id=%s",
                Utils.asList(material.getId(), warehouse.getId()));
            result.put("onhand_qty", cr.fetchOne()[0]);
        } else {
            result.put("onhand_qty", 0);
        }
        return result;
    }
}
