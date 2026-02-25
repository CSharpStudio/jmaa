package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author eric
 */
@Model.Meta(name = "wms.material_receipt_line", label = "物料明细", authModel = "wms.material_receipt", inherit = "mixin.material")
public class MaterialReceiptLine extends Model {
    static Field receipt_id = Field.Many2one("wms.material_receipt").label("收料单");
    static Field po_line_id = Field.Many2one("purchase.order_line").label("采购订单明细");
    static Field po_line_no = Field.Integer().label("订单行号").related("po_line_id.line_no");
    static Field po_id = Field.Many2one("purchase.order").label("采购单号").related("po_line_id.po_id");
    static Field status = Field.Selection(new Options() {{
        put("new", "未收货");
        put("receiving", "部分收货");
        put("received", "已收货");
        put("done", "已完成");
    }}).label("行状态").readonly(true).defaultValue("new");
    static Field request_qty = Field.Float().label("预收数量").required().min(0D);
    static Field gift_qty = Field.Float().label("赠品数");
    static Field receive_qty = Field.Float().label("实收数量").defaultValue(0);
    static Field left_qty = Field.Float().label("待收数量").compute("computeLeftQty");
    static Field purchase_qty = Field.Float().label("采购数量").related("po_line_id.purchase_qty");
    static Field order_surplus = Field.Float().label("订单剩余量");
    static Field stock_rule = Field.Selection().label("库存规则").related("material_id.stock_rule");
    static Field remark = Field.Char().label("备注");
    static Field print_tpl_id = Field.Many2one("print.template").label("标签模板").related("material_id.print_tpl_id").required();
    static Field min_packages = Field.Float().label("标签数量").related("material_id.min_packages");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("收货仓库").lookup("searchWarehouse");
    static Field is_pallet = Field.Boolean().label("码盘").help("是否需要打印码盘").defaultValue(true);

    public Object computeLeftQty(Records record) {
        return Utils.round(record.getDouble("request_qty") - record.getDouble("receive_qty"));
    }

    /**
     * 带出当前用户有权限的仓库
     */
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    @OnSaved("request_qty")
    public void onToReceiveQtySaved(Records records) {
        //更新相应采购订单行的可建单数量
        List<String> lines = new ArrayList<>();
        for (Records record : records) {
            if (Utils.lessOrEqual(record.getDouble("request_qty"), 0)) {
                throw new ValidationException(record.l10n("采购订单[%s]物料[%s]预收数量必须大于0",
                    record.getRec("po_id").getString("code"), record.getRec("material_id").get("code")));
            }
            double deficitQty = record.getDouble("request_qty") - record.getDouble("receive_qty");
            if (Utils.less(deficitQty, 0)) {
                throw new ValidationException(record.l10n("采购订单[%s]物料[%s]预收数量不能小于已收数量[%s]",
                    record.getRec("po_id").getString("code"), record.getRec("material_id").get("code"), record.getDouble("receive_qty")));
            }
            if (Utils.equals(deficitQty, 0d)) {
                record.set("status", "received");
            }
            Records line = record.getRec("po_line_id");
            if (line.any()) {
                lines.add(line.getId());
            }
        }
        if (lines.size() > 0) {
            records.getEnv().get("purchase.order_line", lines).call("updateUncommitQty");
        }
    }

    @OnSaved("receive_qty")
    public void onReceivedQtySaved(Records records) {
        //更新待收数量，更新po收货数量
        Cursor cr = records.getEnv().getCursor();
        //执行sql前先flush保存
        records.flush();
        boolean receiptAutoInspect = records.getEnv().getConfig().getBoolean("receipt_auto_inspect");
        for (Records row : records) {
            Double recQty = row.getDouble("receive_qty");
            Double toQty = row.getDouble("request_qty");
            if (Utils.largeOrEqual(recQty, toQty)) {
                row.set("status", "received");
                if (receiptAutoInspect) {
                    row.getRec("receipt_id").call("createStockIn", Utils.asList(row.getRec("material_id").getId()), "自动报检");
                }
            } else if (Utils.large(recQty, 0)) {
                row.set("status", "receiving");
            }
            Records poLine = row.getRec("po_line_id");
            if (poLine.any()) {
                cr.execute("select sum(receive_qty) from wms_material_receipt_line where po_line_id=%s", Arrays.asList(poLine.getId()));
                double receivedQty = Utils.toDouble(cr.fetchOne()[0]);
                poLine.set("receive_qty", receivedQty);
            }
        }
        if (!receiptAutoInspect) {
            // 开启配置,上面会报检,这里不需要再执行, 没开启配置,则全部收完默认报检,不需要手动点击,
            autoInspection(records.first().getRec("receipt_id"));
        }
    }

    public void autoInspection(Records record) {
        Cursor cr = record.getEnv().getCursor();
        String sql = "select distinct status from wms_material_receipt_line where receipt_id=%s";
        cr.execute(sql, Arrays.asList(record.getId()));
        List<String> status = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
        if (status.isEmpty()) {
            return;
        }
        boolean done = !status.stream().anyMatch(e-> e.equals("new") || e.equals("receiving"));
        if (done) {
            // 全部完成,自动报检
            record.call("createStockIn", null, "收料自动报检");
        }
    }

    @OnSaved("status")
    public void onLineStateSaved(Records records) {
        //行状态更新后，更新收料单状态
        Set<String> receiptIds = new HashSet<>();
        for (Records record : records) {
            receiptIds.add(record.getRec("receipt_id").getId());
        }
        records.getEnv().get("wms.material_receipt", receiptIds).call("updateMaterialReceiptStatus");
    }

    /**
     * 删除后，更新相应采购订单行的可建单数量和收料单状态
     *
     * @param records
     * @return
     */
    @Override
    public boolean delete(Records records) {
        List<String> lines = new ArrayList<>();
        Set<String> receiptIds = new HashSet<>();
        for (Records record : records) {
            receiptIds.add(record.getRec("receipt_id").getId());
            Records line = record.getRec("po_line_id");
            if (line.any()) {
                lines.add(line.getId());
            }
        }
        boolean result = (Boolean) callSuper(records);
        if (lines.size() > 0) {
            records.getEnv().get("purchase.order_line", lines).call("updateUncommitQty");
        }
        records.getEnv().get("wms.material_receipt", receiptIds).call("updateMaterialReceiptStatus");
        return result;
    }

    public double updateReceiveQty(Records records, Double receiveQty) {
        double leftQty = 0d;
        double qty = receiveQty;
        for (Records rec : records) {
            if ("received".equals(rec.get("status"))) {
                continue;
            }
            double toReceiveQty = rec.getDouble("request_qty");
            double receivedQty = rec.getDouble("receive_qty");
            if (Utils.largeOrEqual(qty, toReceiveQty - receivedQty)) {
                rec.set("receive_qty", toReceiveQty);
                qty -= toReceiveQty - receivedQty;
            } else if (Utils.large(qty, 0)) {
                rec.set("receive_qty", receivedQty + qty);
                qty = 0;
            }
            leftQty += rec.getDouble("request_qty") - rec.getDouble("receive_qty");
        }
        return leftQty;
    }

    @ServiceMethod(label = "生成码盘")
    public Object createPallet(Records record,
                               @Doc("物料") String materialId,
                               @Doc("包装规格") String packingId,
                               @Doc("打印模板") String printTplId) {
        Environment env = record.getEnv();
        Records receipt = record.getRec("receipt_id");
        Records lines = record.find(Criteria.equal("po_line_id.material_id", materialId).and("receipt_id", "=", receipt.getId()));
        List<Map<String, Object>> toCreate = new ArrayList<>();
        double qty = Utils.round(lines.stream().mapToDouble(l -> l.getDouble("request_qty") - l.getDouble("receive_qty")).sum());
        Records packing = env.get("md.packing_level", packingId);
        double packageQty = packing.getDouble("package_qty");
        int count = (int) Math.ceil(qty / packageQty);
        Records coding = packing.getRec("coding_id");
        List<String> codes = (List<String>) coding.call("createCodes", count, Collections.emptyMap());
        String stockRule = env.get("md.material", materialId).getString("stock_rule");
        for (int i = 0; i < count; i++) {
            Map<String, Object> pkg = new HashMap<>();
            pkg.put("code", codes.get(i));
            if ("num".equals(stockRule)) {
                pkg.put("qty", Utils.large(qty, packageQty) ? packageQty : qty);
            }
            pkg.put("package_qty", Utils.large(qty, packageQty) ? packageQty : qty);
            pkg.put("supplier_id", receipt.getRec("supplier_id").getId());
            pkg.put("material_id", materialId);
            pkg.put("packing_level_id", packingId);
            pkg.put("print_template_id", printTplId);
            pkg.put("related_code", receipt.get("code"));
            pkg.put("related_model", receipt.getMeta().getName());
            pkg.put("related_id", receipt.getId());
            toCreate.add(pkg);
            qty = Utils.round(qty - packageQty);
        }
        lines.set("is_pallet", false);
        Records labels = env.get("packing.package").createBatch(toCreate);
        Records printTemplate = env.get("print.template").browse(printTplId);
        return printTemplate.call("print", new KvMap().set("package", labels));
    }

    @ServiceMethod(label = "取消码盘")
    public Object cancelPallet(Records record) {
        Records material = record.first().getRec("material_id");
        Records receipt = record.getRec("receipt_id");
        Records packages = record.getEnv().get("packing.package").find(Criteria.equal("related_id", receipt.getId()).and("related_model", "=", receipt.getMeta().getName()));
        Double leftQty = 0d;
        if ("lot".equals(material.getString("stock_rule"))) {
            Set<String> lotNumList = new HashSet<>();
            for (Records pkg : packages) {
                // 通过中间表,查询到所有批次,删除
                Records lotPackage = record.getEnv().get("md.lot_package").find(Criteria.equal("material_id", material.getId()).and("package_id", "=", pkg.getId()));
                lotNumList.addAll(lotPackage.stream().map(e -> e.getString("lot_num")).collect(Collectors.toSet()));
                leftQty = Utils.round(leftQty + lotPackage.stream().mapToDouble(e -> e.getDouble("qty")).sum());
                lotPackage.delete();
            }
            record.getEnv().get("wms.material_receipt_details").find(Criteria.equal("receipt_id", receipt.getId()).and(Criteria.in("lot_num", lotNumList))).delete();
            record.getEnv().get("lbl.lot_num").find(Criteria.equal("material_id", material.getId()).and(Criteria.in("code", lotNumList))).delete();
        } else {
            for (Records pkg : packages) {
                Records details = record.getEnv().get("wms.material_receipt_details").find(Criteria.equal("receipt_id", receipt.getId()).and("label_id.package_id", "=", pkg.getId()));
                List<String> labelIds = new ArrayList<>();
                for (Records detail : details) {
                    Records label = detail.getRec("label_id");
                    if (!"new".equals(label.getString("status"))) {
                        throw new ValidationException(record.l10n("条码状态为[%s]，不能取消", label.getSelection("status")));
                    }
                    labelIds.add(label.getId());
                    leftQty = Utils.round(leftQty + detail.getDouble("qty"));
                }
                record.getEnv().get("lbl.material_label", labelIds).delete();
                details.delete();
            }
        }
        packages.delete();
        Records lines = record.find(Criteria.equal("po_line_id.material_id", record.getRec("material_id").getId()).and("receipt_id", "=", receipt.getId()));
        lines.set("is_pallet", true);
        // 取消全删,
        lines.set("receive_qty", Utils.round(lines.getDouble("receive_qty") - leftQty));
        if (Utils.equals(lines.getDouble("receive_qty"), 0d)) {
            lines.set("status", "new");
        } else {
            lines.set("status", "receiving");
        }
        lines.set("status", "new");
        return Action.reload(record.l10n("操作成功"));
    }

    @ServiceMethod(auth = "createPallet", label = "读取码盘包装信息")
    public Object getPalletPacking(Records record) {
        Map<String, Object> info = new HashMap<>();
        Records material = record.getRec("material_id");
        Records packingRule = material.getRec("packing_rule_ids").filter(p -> p.getBoolean("is_default"));
        if (packingRule.any()) {
            Records packing = packingRule.getRec("rule_id").getRec("level_ids").filter(p -> p.getBoolean("is_in"));
            info.put("packing_level_id", packing.getPresent());
            info.put("package_qty", packing.getInteger("package_qty"));
            info.put("template_id", packing.getRec("print_template_id").getPresent());
        }
        Cursor cr = record.getEnv().getCursor();
        cr.execute("select sum(request_qty - receive_qty) from wms_material_receipt_line where id=%s and material_id=%s", Utils.asList(record.getId(), material.getId()));
        info.put("qty", Utils.toDouble(cr.fetchOne()[0]));
        info.put("material_id", material.getPresent());
        return info;
    }
}
