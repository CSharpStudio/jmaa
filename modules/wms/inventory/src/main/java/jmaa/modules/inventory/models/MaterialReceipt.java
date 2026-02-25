package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ServerDate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author eric
 */
@Model.Meta(name = "wms.material_receipt", label = "收料单", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class MaterialReceipt extends Model {
    static Field code = Field.Char().label("收料单号").unique().readonly();
    static Field type = Field.Selection(new Options() {{
        put("purchase.order", "采购收料");
    }}).label("收料类型").required(true).defaultValue("purchase.order");
    static Field delivery_note = Field.Char().label("送货单号");
    static Field receipt_status = Field.Selection(new Options() {{
        put("new", "未收货");
        put("receiving", "收货中");
        put("received", "已收货");
        put("done", "已完成");
        // MRB挑选,只要有一个退货,那这里就需要改为已退货
        put("returned", "已退货");
    }}).label("收料状态").defaultValue("new");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("收货仓库").lookup("searchWarehouse");
    static Field receive_date = Field.Date().label("到货日期");
    static Field remark = Field.Char().label("备注");
    static Field line_ids = Field.One2many("wms.material_receipt_line", "receipt_id").label("物料列表");
    static Field details_ids = Field.One2many("wms.material_receipt_details", "receipt_id").label("收料明细");
    static Field pallet_ids = Field.One2many("wms.material_receipt_pallet", "related_id").label("码盘明细");
    static Field iqc_sheet_ids = Field.One2many("iqc.sheet", "related_id").label("检验单").compute("computeIqcSheet");

    public Object computeIqcSheet(Records record) {
        return Collections.emptyList();
    }

    /**
     * 带出当前用户有权限的仓库
     */
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    @OnSaved("status")
    public void onStatusSave(Records records) {
        for (Records record : records) {
            if ("commit".equals(record.get("status"))) {
                Records lineIds = record.getRec("line_ids");
                if (!lineIds.any()) {
                    throw new ValidationException(records.l10n("收货单[%s]没有物料明细", record.get("code")));
                }
            }
        }
    }

    @Constrains("status")
    public void checkWarehouse(Records records) {
        for (Records record : records) {
            if ("commit".equals(record.getString("status"))) {
                Records defaultWarehouse = record.getRec("warehouse_id");
                Records lines = record.getRec("line_ids");
                Map<String, String> materialWarehouse = new HashMap<>();
                for (Records line : lines) {
                    Records material = line.getRec("material_id");
                    Records limit = (Records) material.call("findWarehouses");
                    Records warehouse = line.getRec("warehouse_id");
                    if (!warehouse.any()) {
                        warehouse = defaultWarehouse;
                        line.set("warehouse_id", warehouse);
                    }
                    if (!warehouse.any()) {
                        throw new ValidationException(record.l10n("采购订单[%s]物料[%s]没指定收货仓库", line.getRec("po_id").getString("code"), material.get("present")));
                    }
                    String warehouseId = materialWarehouse.get(material.getId());
                    if (Utils.isEmpty(warehouseId)) {
                        materialWarehouse.put(material.getId(), warehouse.getId());
                    } else if (!warehouseId.equals(warehouse.getId())) {
                        throw new ValidationException(record.l10n("物料[%s]不能收货到不同仓库", material.get("present")));
                    }
                    if (limit.any() && !limit.contains(warehouse)) {
                        throw new ValidationException(record.l10n("采购订单[%s]，仓库[%s]不能存放物料[%s]", line.getRec("po_id").getString("code"), warehouse.get("present"), material.get("present")));
                    }
                }
            }
        }
    }

    /**
     * 查找未建单数量大于0的采购订单行
     */
    @ServiceMethod(auth = "read", label = "查询采购行数据")
    public Map<String, Object> searchPoLine(Records rec, Collection<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        criteria = criteria.and("uncommit_qty", ">", 0).and("po_id.status", "=", "approve");
        return rec.getEnv().get("purchase.order_line").searchLimit(fields, criteria, offset, limit, order);
    }

    /**
     * 根据收料类型，带出相关单据
     */
    @ServiceMethod(auth = "read")
    public Map<String, Object> searchRelatedCode(Records record,
                                                 @Doc(doc = "查询条件") Criteria criteria,
                                                 @Doc(doc = "偏移量") Integer offset,
                                                 @Doc(doc = "行数") Integer limit,
                                                 @Doc(doc = "排序") String order,
                                                 @Doc(doc = "收料类型") String type) {
        //相关单据没有Many2one引用关系，保存present字段的值
        if (Utils.equals("purchase.order", type)) {
            criteria.and(Criteria.equal("status", "approve"))
                .and(Criteria.equal("company_id", record.getEnv().getCompany().getId()));
            Map<String, Object> data = record.getEnv().get("purchase.order").searchLimit(Arrays.asList("present"), criteria, offset, limit, order);
            List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("values");
            for (Map<String, Object> row : rows) {
                row.put("id", row.get("present"));
            }
            return data;
        }
        return Collections.emptyMap();
    }

    /**
     * 读取待收物料信息
     */
    @ServiceMethod(label = "读取收料明细", auth = "receipt")
    public Map<String, Object> readReceiptMaterial(Records record, @Doc("物料") String materialId, @Doc("是否加载下一个") Boolean next) {
        Records lines = null;
        Cursor cr = record.getEnv().getCursor();
        java.util.function.Supplier<Records> findLines = () -> {
            String sql = "select material_id from wms_material_receipt_line where receipt_id=%s and status in %s order by material_id";
            sql = cr.getSqlDialect().getPaging(sql, 1, 0);
            cr.execute(sql, Arrays.asList(record.getId(), Arrays.asList("new", "receiving")));
            if (cr.getRowCount() > 0) {
                return record.getEnv().get("wms.material_receipt_line").find(Criteria.equal("receipt_id", record.getId()).and(Criteria.equal("material_id", cr.fetchOne()[0])));
            } else {
                throw new ValidationException("没有待收物料");
            }
        };
        if (!Utils.toBoolean(next)) {
            //有物料id则按物料查找，没有则找第一条未完成的物料
            if (Utils.isNotEmpty(materialId)) {
                lines = record.getEnv().get("wms.material_receipt_line").find(Criteria.equal("receipt_id", record.getId()).and(Criteria.equal("material_id", materialId)));
            } else {
                lines = findLines.get();
            }
        } else {
            //循环找下一个未完成的物料
            String sql = "select material_id from wms_material_receipt_line where receipt_id=%s and status in %s and material_id > %s order by material_id";
            sql = cr.getSqlDialect().getPaging(sql, 1, 0);
            cr.execute(sql, Arrays.asList(record.getId(), Arrays.asList("new", "receiving"), materialId));
            if (cr.getRowCount() > 0) {
                lines = record.getEnv().get("wms.material_receipt_line").find(Criteria.equal("receipt_id", record.getId()).and(Criteria.equal("material_id", cr.fetchOne()[0])));
            } else {
                lines = findLines.get();
            }
        }
        if (!lines.any()) {
            throw new ValidationException("没有待收物料");
        }
        return getReceiptMaterial(record, lines);
    }

    /**
     * 序列管制收料并打印标签
     */
    public Map<String, Object> receiptAndPrintSN(Records receipt, Records lines, Records material, double minPackages,
                                                 double receiveQty, double giftQty, Date productDate, String productLot,
                                                 String lotAttr, String lpn, String printTplId) {
        Environment env = receipt.getEnv();
        Records printTemplate = env.get("print.template").browse(printTplId);
        Records supplier = receipt.getRec("supplier_id");
        Records customer = receipt.getRec("customer_id");
        String packageId = null;
        if (Utils.isNotEmpty(lpn)) {
            Records pkg = env.get("packing.package").find(Criteria.equal("code", lpn));
            if (!pkg.any()) {
                throw new ValidationException(receipt.l10n("LPN[%s]不存在", lpn));
            }
            if (!pkg.getRec("material_id").equals(material)) {
                throw new ValidationException(receipt.l10n("LPN[%s]物料[%s]与打印物料[%s]不一致",
                    lpn, pkg.getRec("material_id").get("code"), material.get("code")));
            }
            double qty = pkg.getDouble("qty");
            double packageQty = pkg.getDouble("package_qty");
            if (Utils.large(qty + receiveQty + giftQty, packageQty)) {
                throw new ValidationException(receipt.l10n("打印数量已超过LPN包装数量[%s]", packageQty));
            }
            pkg.set("qty", Utils.round(qty + receiveQty + giftQty));
            packageId = pkg.getId();
        }
        //根据接收数量和最小包装数计算标签张数
        Map<String, Object> labelData = new KvMap()
            .set("supplier_id", supplier.getId())
            .set("customer_id", customer.getId())
            .set("print_template_id", printTplId)
            .set("print_times", 1)
            .set("package_id", packageId)
            .set("last_print_time", new ServerDate());
        String lotNum = (String) env.get("lbl.lot_num").call("getLotNum", material.getId(), productDate, lotAttr, supplier.getId());
        Records labels = (Records) env.get("lbl.material_label").call("createLabel", material.getId(), minPackages,
            Utils.round(receiveQty + giftQty), productDate, productLot, lotNum, labelData);
        Records line = lines.first();
        line.set("gift_qty", line.getDouble("gift_qty") + giftQty);
        Map<String, Object> result = receiptWithLabel(receipt, lines, "", labels, "confirm");
        Object printData = printTemplate.call("print", new KvMap().set("labels", labels));
        result.put("printData", printData);
        return result;
    }

    /**
     * 批次或数量管制收料并打印标签
     */
    public Map<String, Object> receiptAndPrintQty(Records receipt, String stockRule, Records lines, Records material,
                                                  double minPackages, double receiveQty, double giftQty, Date productDate,
                                                  String productLot, String lotAttr, String lpn, String printTplId) {
        Environment env = receipt.getEnv();
        Records printTemplate = env.get("print.template").browse(printTplId);
        Records supplier = receipt.getRec("supplier_id");
        double printQty = Utils.round(receiveQty + giftQty);
        int count = (int) Math.ceil(printQty / minPackages);
        String lotNum = null;
        List<String> codes = (List<String>) env.get("lbl.material_label").call("createCodes", material.getId(), count);
        String packageId = null;
        if ("lot".equals(stockRule)) {
            lotNum = (String) env.get("lbl.lot_num").call("getLotNum", material.getId(), productDate, lotAttr, supplier.getId());
            if (Utils.isNotEmpty(lpn)) {
                Records pkg = env.get("packing.package").find(Criteria.equal("code", lpn));
                if (!pkg.any()) {
                    throw new ValidationException(receipt.l10n("LPN[%s]不存在", lpn));
                }
                if (!pkg.getRec("material_id").equals(material)) {
                    throw new ValidationException(receipt.l10n("LPN[%s]物料[%s]与打印物料[%s]不一致",
                        lpn, pkg.getRec("material_id").get("code"), material.get("code")));
                }
                double qty = pkg.getDouble("qty");
                double packageQty = pkg.getDouble("package_qty");
                if (Utils.large(qty + printQty + giftQty, packageQty)) {
                    throw new ValidationException(receipt.l10n("打印数量已超过LPN包装数量[%s]", packageQty));
                }
                pkg.set("qty", Utils.round(qty + printQty + giftQty));
                packageId = pkg.getId();
                Map<String, Object> lotPkg = new HashMap<>();
                lotPkg.put("material_id", material.getId());
                lotPkg.put("package_id", packageId);
                lotPkg.put("lot_num", lotNum);
                lotPkg.put("qty", Utils.round(printQty + giftQty));
                env.get("md.lot_package").create(lotPkg);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        double leftQty = printQty;
        for (String sn : codes) {
            double qty = Utils.lessOrEqual(leftQty, minPackages) ? leftQty : minPackages;
            leftQty = Utils.round(leftQty - minPackages);
            String code = sn + "|" + material.get("code") + "|";
            if ("lot".equals(stockRule)) {
                code += lotNum + "|";
            }
            code += qty;
            Map<String, Object> labelData = new KvMap()
                .set("code", code)
                .set("sn", sn)
                .set("lot_num", lotNum)
                .set("qty", qty)
                .set("material_code", material.get("code"))
                .set("material_name", material.get("name"))
                .set("material_spec", material.get("spec"))
                .set("unit", material.getRec("unit_id").get("name"))
                .set("product_date", Utils.format(productDate, "yyyy-MM-dd"))
                .set("product_lot", productLot)
                .set("supplier_name", Utils.replaceAll(supplier.getString("name"), ",", "，"))
                .set("supplier_chars", supplier.get("chars"))
                .set("supplier_code", supplier.get("code"))
                .set("package_id", packageId)
                .set("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            list.add(labelData);
        }
        // 直接打印收料的,管不管重复的呢,管一下
        Map<String, Object> result = receiptWithQty(receipt, lines, lotNum, receiveQty, giftQty, lpn, "confirm");
        if ("lot".equals(stockRule)) {
            boolean lotInQty = env.getConfig().getBoolean("lot_in_qty");
            if (lotInQty && Utils.toBoolean(result.get("submit"))) {
                List<Map<String, Object>> statusList = new ArrayList<>();
                for (String sn : codes) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("order_id", receipt.getId());
                    data.put("sn", sn);
                    data.put("lot_num", lotNum);
                    data.put("material_id", material.getId());
                    data.put("type", "wms.material_receipt");
                    statusList.add(data);
                }
                receipt.getEnv().get("lbl.lot_status").createBatch(statusList);
            }
        }
        Object printData = printTemplate.call("print", new KvMap().set("data", list));
        result.put("printData", printData);
        return result;
    }

    /**
     * 打印物料标签并接收，根据数量和最小包装计算打印标签数量
     */
    @ServiceMethod(label = "收料", doc = "打印标签并收料")
    public Map<String, Object> receipt(Records receipt,
                                       @Doc("物料") String materialId,
                                       @Doc("标签数量") double minPackages,
                                       @Doc("接收数量") double receiveQty,
                                       @Doc("赠品数") double giftQty,
                                       @Doc("生产日期") Date productDate,
                                       @Doc("生产批次") String productLot,
                                       @Doc("批次属性") String lotAttr,
                                       @Doc("LPN") String lpn,
                                       @Doc("打印模板") String printTplId,
                                       @Doc("仓库") String warehouseId) {
        Environment env = receipt.getEnv();
        if (Utils.isEmpty(warehouseId)) {
            throw new ValidationException(receipt.l10n("仓库不能为空"));
        }
        Records material = env.get("md.material", materialId);
        Records lines = getAndLockLines(receipt, material);
        checkParams(lines, material, receiveQty, giftQty, minPackages, productDate, printTplId);
        String stockRule = material.getString("stock_rule");
        Map<String, Object> result = "sn".equals(stockRule) ?
            receiptAndPrintSN(receipt, lines, material, minPackages, receiveQty, giftQty, productDate, productLot, lotAttr, lpn, printTplId)
            : receiptAndPrintQty(receipt, stockRule, lines, material, minPackages, receiveQty, giftQty, productDate, productLot, lotAttr, lpn, printTplId);
        updateMaterialReceiptStatus(receipt);
        return result;
    }

    public void checkParams(Records lines, Records material, double receiveQty, double giftQty, double minPackages, Date productDate, String printTplId) {
        if (Utils.lessOrEqual(receiveQty, 0)) {
            throw new ValidationException(lines.l10n("实收数量必须大于0"));
        }
        if (Utils.lessOrEqual(minPackages, 0)) {
            throw new ValidationException(lines.l10n("标签数量必须大于0"));
        }
        if (Utils.isEmpty(printTplId)) {
            throw new ValidationException(lines.l10n("标签模板不能为空"));
        }
        Integer shelfLife = material.getInteger("shelf_life");
        // 如果保质期为空则不进行判断
        if (Utils.large(shelfLife, 0)) {
            Date offset = Utils.addDays(productDate, shelfLife);
            if (offset.before(new Date())) {
                throw new ValidationException(lines.l10n("物料[%s]已过有效期，不允许接收", material.get("code")));
            }
        }
        double deficitQty = 0d;
        for (Records r : lines) {
            deficitQty += r.getDouble("request_qty") - r.getDouble("receive_qty");
        }
        if (Utils.large(receiveQty, deficitQty)) {
            throw new ValidationException(lines.l10n("实收数量不能大于待收数量"));
        }
    }

    /**
     * 成品标签收料
     */
    public Object receiptByProductLabelCode(Records record, String code, Records productLabel, Records lines, String action, double receiveQty, double giftQty) {
        Records line = lines.first();
        if (Utils.large(giftQty, 0)) {
            line.set("gift_qty", line.getDouble("gift_qty") + giftQty);
        }
        return receiptWithLabel(record, lines, productLabel.getString("sn"), productLabel, action);
    }

    /**
     * 按标签收料
     */
    public Object receiptByLabelCode(Records record, String code, String[] codes, Records lines, String action, double receiveQty, double giftQty) {
        Environment env = record.getEnv();
        Records line = lines.first();
        Records material = line.getRec("material_id");
        String category = material.getString("category");
        String stockRule = material.getString("stock_rule");
        // 外部标签,解析,  可能是成品,
        String lotNum = null;
        boolean lotInQty = false;
        // 成品半成品,会进来这里,
        if ("sn".equals(stockRule) || "semi-finished".equals(category) || "finished".equals(stockRule)) {
            Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
            if (!label.any()) {
                throw new ValidationException(record.l10n("标签[%s]不存在,请检查标签数据", code));
            }
            if (Utils.large(giftQty, 0)) {
                line.set("gift_qty", line.getDouble("gift_qty") + giftQty);
            }
            return receiptWithLabel(record, lines, label.getString("sn"), label, action);
        } else if ("lot".equals(stockRule)) {
            lotNum = codes[2];
            lotInQty = env.getConfig().getBoolean("lot_in_qty");
            if (lotInQty) {
                Records lotSnStatus = env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId())
                    .and(Criteria.equal("type", "wms.material_receipt"))
                    .and(Criteria.equal("lot_num", lotNum)).and(Criteria.equal("sn", codes[0]))
                    .and(Criteria.equal("material_id", material.getId())));
                if (lotSnStatus.any()) {
                    throw new ValidationException(record.l10n("当前批次标签序列号[%s]已有记录，不能重复使用", codes[0]));
                }
            }
        }
        if (!"confirm".equals(action)) {
            receiveQty = Utils.toDouble(codes[codes.length - 1]);
        }
        Map<String, Object> result = receiptWithQty(record, lines, lotNum, Utils.round(receiveQty - giftQty), giftQty, null, action);
        if (lotInQty && Utils.toBoolean(result.get("submit"))) {
            Map<String, Object> data = new HashMap<>();
            data.put("order_id", record.getId());
            data.put("sn", codes[0]);
            data.put("material_id", material.getId());
            data.put("lot_num", lotNum);
            data.put("type", "wms.material_receipt");
            record.getEnv().get("lbl.lot_status").create(data);
        }
        return result;
    }

    /**
     * 按包装标签（LPN）收料
     */
    public Object receiptByPackageCode(Records record, String code, Records pkg, Records lines, String action, double receiveQty, double giftQty) {
        Environment env = record.getEnv();
        Records line = lines.first();
        Records material = line.getRec("material_id");
        String stockRule = material.getString("stock_rule");
        if ("sn".equals(stockRule)) {
            Records labels = env.get("lbl.material_label").find(Criteria.equal("package_id", pkg.getId()).and("status", "=", "new"));
            if (!labels.any()) {
                labels = env.get("lbl.material_label").find(Criteria.equal("package_id", pkg.getId()), 0, 1, null);
                if (labels.any()) {
                    throw new ValidationException(record.l10n("包装标签[%s]已完成接收", code));
                }
                throw new ValidationException(record.l10n("包装标签[%s]没绑定物料标签", code));
            }
            if (Utils.large(giftQty, 0)) {
                line.set("gift_qty", line.getDouble("gift_qty") + giftQty);
            }
            return receiptWithLabel(record, lines, code, labels, action);
        } else if ("lot".equals(stockRule)) {
            // 根据包装查找批次
            Records lotPackage = env.get("md.lot_package").find(Criteria.equal("package_id", pkg.getId())
                .and(Criteria.equal("material_id", material.getId())));
            if (!lotPackage.any()) {
                throw new ValidationException(record.l10n("批次包装标签[%s]无关联批次数据,请检查数据", code));
            }
            if (!"confirm".equals(action)) {
                receiveQty = pkg.getDouble("qty");
            }
            // 扫码批次包装,无法控制标签是否重复提交
            return receiptWithLotPkgQty(record, lines, lotPackage, action, receiveQty, giftQty);
        }
        if (!"confirm".equals(action)) {
            receiveQty = pkg.getDouble("qty");
        }
        return receiptWithQty(record, lines, null, receiveQty, giftQty, null, action);
    }

    /**
     * 按批次号收料（批次管控）
     */
    public Object receiptByLotCode(Records record, Records lot, Records lines, String action, double receiveQty, double giftQty) {
        Records line = lines.first();
        Records material = line.getRec("material_id");
        String stockRule = material.getString("stock_rule");
        if (!"lot".equals(stockRule)) {
            throw new ValidationException(record.l10n("[%s]物料[%s]不能使用批次条码接收", material.getSelection("stock_rule"), material.get("code")));
        }
        if (!"confirm".equals(action)) {
            receiveQty = lot.getDouble("qty");
        }
        return receiptWithQty(record, lines, lot.getString("code"), receiveQty, giftQty, null, action);
    }

    /**
     * 按物料编码收料（数量管控）
     */
    public Object receiptByMaterialCode(Records record, Records material, Records lines, String action, double receiveQty, double giftQty) {
        String stockRule = material.getString("stock_rule");
        if (!"num".equals(stockRule)) {
            throw new ValidationException(record.l10n("[%s]物料[%s]不能使用物料编码接收", material.getSelection("stock_rule"), material.get("code")));
        }
        if (!"confirm".equals(action)) {
            receiveQty = Utils.round(lines.stream().mapToDouble(r -> r.getDouble("request_qty") - r.getDouble("receive_qty")).sum());
        }
        return receiptWithQty(record, lines, null, Utils.round(receiveQty - giftQty), giftQty, null, action);
    }

    /**
     * 读取并锁定物料行，防止并发
     */
    public Records getAndLockLines(Records record, Records material) {
        Records lines = record.getEnv().get("wms.material_receipt_line").find(Criteria.equal("receipt_id", record.getId())
            .and("material_id", "=", material.getId()));
        if (!lines.any()) {
            throw new ValidationException(record.l10n("收料单不包含物料[%s]", material.get("code")));
        }
        Cursor cr = record.getEnv().getCursor();
        //事务锁定收料行
        cr.execute("update wms_material_receipt_line set id=id where id in %s", Arrays.asList(Arrays.asList(lines.getIds())));
        return lines;
    }

    /**
     * 根据物料行获取并校验仓库
     */
    public Records getAndCheckWarehouse(Records record, Records line) {
        Records warehouse = line.getRec("warehouse_id");
        Records warehouses = record.getEnv().getUser().getRec("warehouse_ids");
        if (!warehouses.contains(warehouse)) {
            throw new ValidationException(record.l10n("当前用户没有仓库[%s]权限", warehouse.get("present")));
        }
        return warehouse;
    }

    /**
     * 扫码收料，条码可以是物料标签，码盘LPN，物料编码。
     * <pre>
     * 数量管控的物料，可通过标签/LPN/物料编码收料。
     * 批次管控的物料，可通过标签/批次编码收料。
     * 序列号管控的物料，可通过标签/LPN收料。
     * 提交：序列号管控可选择自动提交，无需要确认
     * </pre>
     *
     * @param code       物料编码\码盘LPN\物料编码
     * @param action     操作：auto/confirm/none
     * @param receiveQty 收货数量，数量管控没有LPN时，需要用户确认接收数量
     */
    @ServiceMethod(label = "扫描条码", doc = "物料编码则查询收料信息，物料标签则收料", auth = "receipt")
    public Object receiptByCode(Records record,
                                @Doc("标签条码/物料编码") String code,
                                @Doc("操作：none/auto/confirm") String action,
                                @Doc("收货数量(含赠品)") double receiveQty,
                                @Doc("赠品数量") double giftQty) {
        return doReceiptByCode(record, code, action, receiveQty, giftQty, null);
    }

    public Object doReceiptByCode(Records record, String code, String action, double receiveQty, double giftQty, String materialId) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            if (Utils.isNotEmpty(materialId) && !Utils.equals(materialId, material.getId())) {
                throw new ValidationException(record.l10n("标签物料[%s]与当前任务物料[%s]不一致", material.get("code"), material.browse(materialId).get("code")));
            }
            Records lines = getAndLockLines(record, material);
            return receiptByLabelCode(record, code, codes, lines, action, receiveQty, giftQty);
        }
        Records pkg = env.get("packing.package").find(Criteria.equal("code", code));
        if (pkg.any()) {
            Records material = pkg.getRec("material_id");
            if (Utils.isNotEmpty(materialId) && !Utils.equals(materialId, material.getId())) {
                throw new ValidationException(record.l10n("标签物料[%s]与当前任务物料[%s]不一致", material.get("code"), material.browse(materialId).get("code")));
            }
            Records lines = getAndLockLines(record, material);
            return receiptByPackageCode(record, code, pkg, lines, action, receiveQty, giftQty);
        }
        // 成品,委外采购全是成品物料
        Records productLabel = env.get("lbl.product_label").find(Criteria.equal("sn", code));
        if (productLabel.any()) {
            Records material = productLabel.getRec("material_id");
            if (Utils.isNotEmpty(materialId) && !Utils.equals(materialId, material.getId())) {
                throw new ValidationException(record.l10n("标签物料[%s]与当前任务物料[%s]不一致", material.get("code"), material.browse(materialId).get("code")));
            }
            Records lines = getAndLockLines(record, material);
            return receiptByProductLabelCode(record, code, productLabel, lines, action, receiveQty, giftQty);
        }
        // todo 收料现阶段不会有单独的批号,入参的物料id一直都是null, 下面暂时不处理,
        /*Records lot = env.get("lbl.lot_num").find(Criteria.equal("code", code).and(Criteria.equal("material_id",materialId)));
        if (lot.any()) {
            Records material = lot.getRec("material_id");
            if (Utils.isNotEmpty(materialId) && !Utils.equals(materialId, material.getId())) {
                throw new ValidationException(record.l10n("标签物料[%s]与当前任务物料[%s]不一致", material.get("code"), material.browse(materialId).get("code")));
            }
            Records lines = getAndLockLines(record, material);
            return receiptByLotCode(record, lot, lines, action, receiveQty, giftQty);
        }*/
        Records material = env.get("md.material").find(Criteria.equal("code", code));
        if (material.any()) {
            if (Utils.isNotEmpty(materialId) && !Utils.equals(materialId, material.getId())) {
                throw new ValidationException(record.l10n("标签物料[%s]与当前任务物料[%s]不一致", material.get("code"), material.browse(materialId).get("code")));
            }
            Records lines = getAndLockLines(record, material);
            return receiptByMaterialCode(record, material, lines, action, receiveQty, giftQty);
        }
        throw new ValidationException(record.l10n("条码[%s]无法识别", code));
    }

    /**
     * 批次管控物料按包装标签（LPN）接收。
     */
    public Map<String, Object> receiptWithLotPkgQty(Records record, Records lines, Records lotPackage, String action, double receiveQty, double giftQty) {
        if (Utils.lessOrEqual(receiveQty, 0)) {
            throw new ValidationException(record.l10n("实收数量必须大于0"));
        }
        Environment env = record.getEnv();
        Records line = lines.first();
        Records material = line.getRec("material_id");
        Records warehouse = getAndCheckWarehouse(record, line);
        Map<String, Object> result = new HashMap<>();
        if ("auto".equals(action) || "confirm".equals(action)) {
            if (Utils.large(giftQty, 0d)) {
                line.set("gift_qty", line.getDouble("gift_qty") + Utils.toDouble(giftQty));
                receiveQty = Utils.round(receiveQty - giftQty);
            }
            for (Records lotPkg : lotPackage) {
                String lotNum = lotPkg.getString("lot_num");
                Double qty = lotPkg.getDouble("qty");
                if (Utils.large(giftQty, 0d)) {
                    qty = Utils.round(qty + giftQty);
                    giftQty = 0d;
                }
                Records detail = env.get("wms.material_receipt_details").find(Criteria.equal("receipt_id", record.getId())
                    .and("material_id", "=", material.getId()).and("lot_num", "=", lotNum).and("status", "=", "new"));
                if (!detail.any()) {
                    Map<String, Object> data = createReceiptDetails(record, material.getId(), warehouse.getId(), qty, lotNum, null);
                    data.put("lpn", lotPkg.getString("package_code"));
                    env.get("wms.material_receipt_details").create(data);
                } else {
                    env.getCursor().execute("update stock_stock_in_details set qty=qty+%s where id=%s", Arrays.asList(qty, detail.first().getId()));
                }
            }
            lines.call("updateReceiveQty", receiveQty);
            updateMaterialReceiptStatus(record);
            result.put("submit", true);
            result.put("data", getLineInfo(record, lines, null));
            result.put("message", record.l10n("物料[%s]接收数量[%s]成功", material.get("code"), receiveQty));
        } else {
            Map<String, Object> data = getLineInfo(record, lines, receiveQty);
            data.put("lock_qty", true);
            result.put("data", data);
            result.put("message", record.l10n("物料[%s]识别成功，待确认", material.get("code")));
        }
        return result;
    }

    /**
     * 序列号管控物料的接收。
     */
    public Map<String, Object> receiptWithLabel(Records record, Records lines, String code, Records labels, String action) {
        Environment env = record.getEnv();
        Records line = lines.first();
        Records warehouse = getAndCheckWarehouse(record, line);
        Records material = line.getRec("material_id");
        List<String> status = Utils.asList("new", "receiving", "received");
        lines = lines.filter(l -> status.contains(l.getString("status")));
        if (!lines.any()) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求，不能超收", material.get("code")));
        }
        double receiveQty = 0d;
        for (Records label : labels) {
            if (!"new".equals(label.getString("status"))) {
                throw new ValidationException(record.l10n("标签[%s]状态为[%s]，不能接收", label.get("sn"), label.getSelection("status")));
            }
            receiveQty += label.getDouble("qty");
        }
        double requestQty = 0d;
        double receivedQty = 0d;
        double gQty = 0d;
        for (Records row : lines) {
            requestQty += row.getDouble("request_qty");
            receivedQty += row.getDouble("receive_qty");
            gQty += row.getDouble("gift_qty");
        }
        Map<String, Object> result = new HashMap<>();
        if ("auto".equals(action) || "confirm".equals(action)) {
            double deficitQty = Utils.round(requestQty - receivedQty);
            if (Utils.lessOrEqual(deficitQty, 0)) {
                throw new ValidationException(record.l10n("物料[%s]已满足需求，不能超收", material.get("code")));
            }
            if (Utils.large(receiveQty, deficitQty + gQty)) {
                if ("confirm".equals(action)) {
                    throw new ValidationException(record.l10n("标签[%s]数量[%s]超出待接收数量[%s]", code, receiveQty, deficitQty));
                } else {
                    result.put("data", getLineInfo(record, lines, receiveQty));
                    result.put("message", record.l10n("标签[%s]数量[%s]超出待接收数量[%s]，请确认", code, receiveQty, deficitQty));
                    return result;
                }
            }
            labels.set("status", "received");
            List<Map<String, Object>> details = new ArrayList<>();
            for (Records label : labels) {
                Map<String, Object> data = createReceiptDetails(record, material.getId(), warehouse.getId(),
                    label.getDouble("qty"), label.getString("lot_num"), label.getId());
                details.add(data);
                Map<String, Object> log = new HashMap<>();
                log.put("operation", "wms.material_receipt");
                log.put("related_id", record.getId());
                log.put("related_code", record.get("code"));
                log.put("location", warehouse.get("present"));
                label.call("logStatus", log);
            }
            env.get("wms.material_receipt_details").createBatch(details);
            lines.call("updateReceiveQty", receiveQty);
            result.put("submit", true);
            result.put("data", getLineInfo(record, lines, null));
            result.put("message", record.l10n("物料[%s]接收数量[%s]成功", material.get("code"), receiveQty));
        } else {
            result.put("data", getLineInfo(record, lines, receiveQty));
            result.put("message", record.l10n("物料[%s]识别成功，待确认", material.get("code")));
        }
        return result;
    }

    /**
     * 数量/批次管控物料的接收。
     */
    public Map<String, Object> receiptWithQty(Records record, Records lines, String lotNum, double receiveQty, double giftQty, String lpn, String action) {
        if (Utils.lessOrEqual(receiveQty, 0)) {
            throw new ValidationException(record.l10n("实收数量必须大于0"));
        }
        if (Utils.isEmpty(lotNum)) {
            lotNum = null;
        }
        Environment env = record.getEnv();
        Records line = lines.first();
        Records warehouse = getAndCheckWarehouse(record, line);
        Records material = line.getRec("material_id");
        double deficitQty = lines.stream().mapToDouble(l -> l.getDouble("request_qty") - l.getDouble("receive_qty")).sum();
        if (Utils.lessOrEqual(deficitQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求，不能超收", material.get("code")));
        }
        Map<String, Object> result = new HashMap<>();
        if (Utils.large(receiveQty, deficitQty)) {
            if ("confirm".equals(action)) {
                throw new ValidationException(Utils.isEmpty(lotNum) ? lines.l10n("实收数量[%s]不能大于待收数量[%s]", receiveQty, deficitQty)
                    : lines.l10n("批次[%s]实收数量[%s]不能大于待收数量[%s]", lotNum, receiveQty, deficitQty));
            }
            result.put("data", getLineInfo(record, lines, receiveQty));
            result.put("message", Utils.isEmpty(lotNum) ? record.l10n("标签数量[%s]超出待接收数量[%s]，请确认", receiveQty, deficitQty)
                : record.l10n("批次[%s]数量[%s]超出待接收数量[%s]，请确认", lotNum, receiveQty, deficitQty));
            return result;
        }
        if ("auto".equals(action) || "confirm".equals(action)) {
            Records detail = env.get("wms.material_receipt_details").find(Criteria.equal("receipt_id", record.getId())
                .and("material_id", "=", material.getId()).and("lot_num", "=", lotNum).and("status", "=", "new"));
            if (!detail.any()) {
                Map<String, Object> data = createReceiptDetails(record, material.getId(), warehouse.getId(),
                    Utils.round(receiveQty + giftQty), lotNum, null);
                if (Utils.isNotEmpty(lpn)) {
                    data.put("lpn", lpn);
                }
                env.get("wms.material_receipt_details").create(data);
            } else {
                double qty = Utils.round(receiveQty + giftQty);
                env.getCursor().execute("update stock_stock_in_details set qty=qty+%s where id=%s", Arrays.asList(qty, detail.first().getId()));
            }
            line.set("gift_qty", line.getDouble("gift_qty") + Utils.toDouble(giftQty));
            lines.call("updateReceiveQty", receiveQty);
            updateMaterialReceiptStatus(record);
            result.put("submit", true);
            result.put("data", getLineInfo(record, lines, null));
            result.put("message", record.l10n("物料[%s]接收数量[%s]成功", material.get("code"), Utils.round(receiveQty + giftQty)));
        } else {
            result.put("data", getLineInfo(record, lines, receiveQty));
            result.put("message", record.l10n("物料[%s]识别成功，待确认", material.get("code")));
        }
        return result;
    }

    public Map<String, Object> createReceiptDetails(Records record, String materialId, String warehouseId, double receiveQty, String lotNum, String labelId) {
        Map<String, Object> data = new HashMap<>();
        data.put("receipt_id", record.getId());
        data.put("status", "receive");
        data.put("material_id", materialId);
        data.put("qty", receiveQty);
        data.put("warehouse_id", warehouseId);
        data.put("label_id", labelId);
        data.put("lot_num", lotNum);
        return data;
    }

    /**
     * 获取条码信息进行收货确认
     */
    public Map<String, Object> getLineInfo(Records record, Records lines, Double confirmQty) {
        Map<String, Object> data = new HashMap<>();
        Records material = lines.first().getRec("material_id");
        Records unit = material.getRec("unit_id");
        boolean lockQty = false;
        String stockRule = material.getString("stock_rule");
        if ("sn".equals(stockRule)) {
            lockQty = true;
        } else if ("lot".equals(stockRule)) {
            lockQty = record.getEnv().getConfig().getBoolean("lot_in_qty");
        }
        data.put("lock_qty", lockQty);
        data.put("unit_id", unit.getPresent());
        data.put("unit_accuracy", unit.get("accuracy"));
        data.put("material_id", material.getPresent());
        data.put("material_name_spec", material.get("name_spec"));
        data.put("confirm_qty", confirmQty);
        Map<String, Double> qtyMap = new HashMap<>();
        for (Records line : lines) {
            qtyMap.merge("request_qty", line.getDouble("request_qty"), Double::sum);
            qtyMap.merge("gift_qty", line.getDouble("gift_qty"), Double::sum);
            qtyMap.merge("receive_qty", line.getDouble("receive_qty"), Double::sum);
            qtyMap.merge("left_qty", line.getDouble("left_qty"), Double::sum);
        }
        data.put("request_qty", qtyMap.get("request_qty"));
        data.put("gift_qty", qtyMap.get("gift_qty"));
        data.put("receive_qty", qtyMap.get("receive_qty"));
        data.put("left_qty", qtyMap.get("left_qty"));
        data.put("warehouse_id", lines.first().getRec("warehouse_id").getPresent());
        return data;
    }

    public Map<String, Object> getReceiptMaterial(Records record, Records lines) {
        Records warehouse = record.getRec("warehouse_id");
        Double requestQty = 0d;
        Double deficitQty = 0d;
        for (Records line : lines) {
            Records w = line.getRec("warehouse_id");
            if (w.any()) {
                warehouse = w;
            }
            requestQty += line.getDouble("request_qty");
            deficitQty += line.getDouble("request_qty") - line.getDouble("receive_qty");
        }
        Records line = lines.first();
        Records material = line.getRec("material_id");
        Records unit = material.getRec("unit_id");
        Map<String, Object> data = new HashMap<>();
        data.put("material_id", material.getPresent());
        data.put("material_name_spec", material.get("name_spec"));
        data.put("stock_rule", material.get("stock_rule"));
        data.put("print_tpl_id", material.getRec("print_tpl_id").getPresent());
        data.put("min_packages", material.get("min_packages"));
        data.put("warehouse_id", warehouse.getPresent());
        data.put("request_qty", Utils.round(requestQty));
        data.put("deficit_qty", Utils.round(deficitQty));
        data.put("unit_id", unit.getPresent());
        data.put("unit_accuracy", unit.getInteger("accuracy"));
        return data;
    }

    @ServiceMethod(label = "创建入库单", doc = "根据已经收货的物料创建入库单")
    public Object createStockIn(Records records, Collection<String> materialIds, String comment) {
        boolean exempted = false;
        for (Records record : records) {
            Criteria criteria = Criteria.equal("status", "receive")
                .and(Criteria.equal("stock_in_id", null))
                .and(Criteria.equal("receipt_id", record.getId()));
            if (Utils.isNotEmpty(materialIds)) {
                criteria.and(Criteria.in("material_id", materialIds));
            }
            Environment env = record.getEnv();
            Records details = env.get("wms.material_receipt_details").find(criteria);
            if (!details.any()) {
                // 一张单,多行相同物料,一次性收满,
                // 在解析 @OnSaved("receive_qty")时, 会执行多次,
                // 第一次会将检验单生成好,并且,上面获取明细时,会将所有物料明细都查询出来,没有区分行数量,
                // 非第一次时, 获取明细就为空, 那么进来这里的逻辑, 先确认是否存在检验单, 单号, 物料 , 不管状态
                // 存在说明之前生成了,那就变更收料单行状态,同时也要更改单据状态,
                Records sheet = records.getEnv().get("iqc.sheet").find(Criteria.equal("related_code", record.getString("code"))
                    .and(Criteria.in("material_id", materialIds)));
                if (sheet.any()) {
                    updateMaterialReceiptLineStatus(records, materialIds);
                    updateMaterialReceiptStatus(records);
                    return Action.reload(records.l10n("操作成功"));
                } else {
                    throw new ValidationException(record.l10n(String.format("收料单号[%s]没有状态为已收货的收料明细", record.getString("code"))));
                }
            }
            Map<String, Records> groupByMaterial = new HashMap<>();
            //更新标签质量状态
            for (Records detail : details) {
                Records label = detail.getRec("label_id");
                if (label.any()) {
                    label.set("quality_status", "ok");
                }
                String materialId = detail.getRec("material_id").getId();
                Records group = groupByMaterial.get(materialId);
                if (group == null) {
                    group = detail.browse();
                    groupByMaterial.put(materialId, group);
                }
                group.union(detail);
            }
            //创建入库单
            Records stockIn = env.get("stock.stock_in");
            Map<String, Object> data = new HashMap<>();
            data.put("status", "to-inspect");
            data.put("type", "purchase");
            data.put("related_code", record.get("code"));
            data.put("related_model", record.getMeta().getName());
            data.put("related_id", record.getId());
            stockIn = stockIn.create(data);
            details.set("stock_in_id", stockIn.getId());
            details.set("status", "to-stock");
            //创建来料检验单
            for (Records detail : groupByMaterial.values()) {
                exempted = createIqcSheet(record, detail) || exempted;
                if (!exempted) {
                    details.set("status", "to-inspect");
                }
            }
            //更新收料状态
            Cursor cr = env.getCursor();
            String sql = "update wms_material_receipt_line set status='done' where receipt_id=%s and material_id in %s and receive_qty>=request_qty and status!='close'";
            cr.execute(sql, Arrays.asList(record.getId(), groupByMaterial.keySet()));
            //记录消息
            String message = record.l10n("生成入库单: %s。", stockIn.get("code"));
            if (Utils.isNotEmpty(comment)) {
                message += record.l10n("备注：") + comment;
            }
            record.call("trackMessage", message);
            // 多次报检也没关系,每次都清除, 先不管反复修改配置的情况
            env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId()).and(Criteria.equal("type", "wms.material_receipt"))).delete();
        }
        updateMaterialReceiptStatus(records);
        if (exempted) {
            return new KvMap()
                .set("message", records.l10n("操作成功。存在免检物料，请及时入库"))
                .set("exempted", true);
        }
        return Action.reload(records.l10n("操作成功"));
    }

    public boolean createIqcSheet(Records record, Records details) {
        Records material = details.first().getRec("material_id");
        if (!material.getBoolean("iqc")) {
            return true;
        }
        Records iqc = record.getEnv().get("iqc.sheet");
        Records supplier = record.getRec("supplier_id");
        boolean exempted = (boolean) iqc.call("isExempt", material, supplier);
        double qty = details.stream().mapToDouble(d -> d.getDouble("qty")).sum();
        iqc = iqc.create(new KvMap()
            .set("qty", qty)
            .set("material_id", material.getId())
            .set("supplier_id", supplier.getId())
            .set("status", exempted ? "exempted" : "to-inspect")
            .set("related_code", record.get("code"))
            .set("related_model", record.getMeta().getName())
            .set("related_id", record.getId())
            .set("result", exempted ? "ok" : null));
        record.getEnv().get("iqc.material_details", details.getIds()).set("iqc_id", iqc.getId());
        return exempted;
    }

    /**
     * 更新收料单状态
     */
    public void updateMaterialReceiptStatus(Records records) {
        //执行sql前flush保存
        records.flush();
        Cursor cr = records.getEnv().getCursor();
        String sql = "select distinct status from wms_material_receipt_line where receipt_id=%s";
        for (Records record : records) {
            cr.execute(sql, Arrays.asList(record.getId()));
            List<String> status = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
            if (status.isEmpty()) {
                continue;
            }
            boolean done = status.stream().allMatch(s -> "done".equals(s));
            if (done) {
                // 如果全部为完成状态，则更新为已完成状态
                record.set("receipt_status", "done");
                record.set("status", "done");
                break;
            }
            boolean receiving = status.stream().anyMatch(s -> "receiving".equals(s));
            if (receiving) {
                record.set("receipt_status", "receiving");
                break;
            }
            boolean received = status.stream().allMatch(s -> !"new".equals(s) && !"receiving".equals(s));
            if (received) {
                record.set("receipt_status", "received");
                break;
            }
            record.set("receipt_status", "receiving");
        }
    }

    public void updateMaterialReceiptLineStatus(Records records, Collection<String> materialIds) {
        for (Records record : records) {
            Records lines = record.getEnv().get("wms.material_receipt_line").find(Criteria.equal("receipt_id", record.getId()).and(Criteria.in("material_id", materialIds)).and(Criteria.equal("status", "received")));
            lines.set("status", "done");
        }
    }
}
