package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "wms.pick_out", label = "挑选", inherit = {"code.auto_code", "mixin.material", "mixin.order_status"})
public class PickOut extends Model {
    static Field code = Field.Char().label("挑选单号").unique().readonly();
    // 存iqc检验单 iqc.sheet
    static Field related_code = Field.Char().label("相关单据").required();
    static Field material_id = Field.Many2one("md.material").label("物料");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field qty = Field.Float().label("送检数量").readonly();
    static Field type = Field.Selection().addSelection(new Options() {{
        put("iqc.sheet", "来料检验");
    }}).label("单据类型");
    static Field pick_type = Field.Selection(new Options() {{
        put("pick_stock", "挑选入库");
        put("pick_return", "挑选退货");
    }}).label("挑选结果").defaultValue("pick_stock").help("挑选入库/挑选退货");
    static Field detail_ids = Field.One2many("wms.pick_out_details", "pick_out_id");
    static Field status = Field.Selection(new Options() {{
        put("draft", "草稿");
        put("commit", "已提交");
        put("approve", "已审核");
        put("reject", "驳回");
        put("done", "已完成");
    }}).label("状态").defaultValue("draft").readonly();
    static Field material_stock_rule = Field.Selection().related("material_id.stock_rule").label("库存规则");

    @ActionMethod
    public Action onRelatedCodeChange(Records record) {
        Environment env = record.getEnv();
        AttrAction action = Action.attr();
        String type = record.getString("type");
        if (Utils.isBlank(type)) {
            throw new ValidationException("请先选择单据类型");
        }
        String relatedCode = record.getString("related_code");
        if ("iqc.sheet".equals(type)) {
            Records sheetRecord = env.get(type).find(Criteria.equal("code", relatedCode));
            if (!sheetRecord.any()) {
                throw new ValidationException("来料检验单无数据,请检查相关单据");
            }
            Records material = sheetRecord.getRec("material_id");
            Records supplier = sheetRecord.getRec("supplier_id");
            action.setValue("material_id", material.getId());
            action.setValue("supplier_id", supplier.getId());
            action.setValue("qty", sheetRecord.get("qty"));
            action.setValue("material_name_spec", material.get("name_spec"));
            action.setValue("material_category", material.get("category"));
            action.setValue("qty", sheetRecord.get("qty"));
        }
        return action;
    }

    @ServiceMethod(auth = "read")
    public Map<String, Object> searchRelatedCode(Records record,
                                                 @Doc(doc = "查询条件") Criteria criteria,
                                                 @Doc(doc = "偏移量") Integer offset,
                                                 @Doc(doc = "行数") Integer limit,
                                                 @Doc(doc = "排序") String order,
                                                 @Doc(doc = "收料类型") String type) {
        // 这里的标签,可能是序列号,批次 ,  可能是asn单打印的标签, 也可能是其他地方打印的无任何关联的标签,
        // 码盘  RGF2505300024   packing.package
        // sn 标签  RGF2505300024|200110040001   sn|code lbl.material_label
        // 批次 标签  RGF2505300024-1|200110040001|RGF2505300024|100   sn|code lbl.lot_num | qty
        // 数量管控 RGF2505300024|200110040001|100      sn|code|qty
        if (Utils.equals("iqc.sheet", type)) {
            criteria.and(Criteria.equal("result", "ng"));
            Map<String, Object> data = record.getEnv().get(type).searchLimit(Arrays.asList("present"), criteria, offset, limit, order);
            List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("values");
            for (Map<String, Object> row : rows) {
                row.put("id", row.get("present"));
            }
            return data;
        }
        return Collections.emptyMap();
    }

    @ServiceMethod(auth = "read", label = "扫描物料标签")
    public Map<String, Object> scanMaterialCode(Records record, String code, Integer qty) {
        // 这里的标签,可能是序列号,批次 ,  可能是asn单打印的标签, 也可能是其他地方打印的无任何关联的标签,
        // 码盘  RGF2505300024   packing.package
        // sn 标签  RGF2505300024|200110040001   sn|code lbl.material_label
        // 批次 标签  RGF2505300024-1|200110040001|RGF2505300024|100   sn|code lbl.lot_num | qty
        // 数量管控 RGF2505300024|200110040001|100      sn|code|qty
        Environment env = record.getEnv();
        Records material = record.getRec("material_id");
        String stockRule = material.getString("stock_rule");
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records mdPackage = null;
        if (codes.length > 1) {
            getMaterialAndCheck(record, material.getId(), codes[1]);
            if ("sn".equals(stockRule)) {
                // 查一下是否存在
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                if (!label.any()) {
                    throw new ValidationException(record.l10n("标签序列号不存在,请检查数据"));
                }
                return pickSnMaterialLabel(record, label);
            } else if ("lot".equals(stockRule)) {
                Records lotNum = env.get("lbl.lot_num").find(Criteria.equal("code", codes[2]).and(Criteria.equal("material_id",material.getId())));
                if (!lotNum.any()) {
                    throw new ValidationException(record.l10n("标签批次号不存在,请检查数据"));
                }
                return pickLotMaterialLabel(record, lotNum, qty, codes[codes.length - 1], codes[0]);
            } else {
                // 数量
                // RGF2505300024|200110040001|100
                return pickNumMaterialLabel(record, material, qty, codes[codes.length - 1]);
            }
        } else if ("num".equals(stockRule) && (mdPackage = env.get("packing.package").find(Criteria.equal("code", code))).any()) {
            // 这种挑选扫码逻辑,只让数量管控的进入, 序列号标签,数量不好分配
            return pickNumMaterialLabel(record, material, qty, mdPackage.get("qty").toString());
        } else {
            throw new ValidationException(record.l10n("扫描标签无法识别,请检查数据"));
        }
    }

    public void getMaterialAndCheck(Records record, String materialId, String materialCode) {
        Records scanMaterial = record.getEnv().get("md.material").find(Criteria.equal("code", materialCode));
        if (!Utils.equals(materialId, scanMaterial.getId())) {
            throw new ValidationException(record.l10n("扫描标签非当前所需挑选物料,请检查数据"));
        }
    }

    public Map<String, Object> pickNumMaterialLabel(Records record, Records material, Integer qty, String labelQty) {
        Records pickOutDetails = record.getEnv().get("wms.pick_out_details");
        Map<String, Object> result = createResultMap(record, material);
        // 数量管控不需要序列号, 跟批次号一样,他可以一个标签反复的扫
        if (null != qty) {
            double totalQty = record.getDouble("qty");
            Records pickOutDetail = pickOutDetails.find(Criteria.equal("pick_out_id", record.get("id")).and(Criteria.equal("material_id", material.getId())));
            if (pickOutDetail.any()) {
                // 之前这个批次的数据就已经扫描过了,
                // 再次扫这个批次的数据
                double baseQty = Utils.round(pickOutDetail.getDouble("qty") + qty);
                if (Utils.large(baseQty, totalQty)) {
                    throw new ValidationException(record.l10n("输入数量合计大于检验数量,请检查"));
                }
                pickOutDetail.set("qty", baseQty);
            } else {
                if (Utils.large(qty, totalQty)) {
                    throw new ValidationException(record.l10n("输入数量大于检验数量,请检查"));
                }
                result.put("qty", qty);
                pickOutDetails.create(result);
            }
        } else {
            result.put("qty", labelQty);
        }
        return result;
    }

    public Map<String, Object> pickLotMaterialLabel(Records record, Records lotNum, Integer qty, String labelQty, String sn) {
        Environment env = record.getEnv();
        // 先确认是否使用
        Records material = lotNum.getRec("material_id");
        boolean lotInQtyFlag = env.getConfig().getBoolean("lot_in_qty");
        if (lotInQtyFlag) {
            Records lotSnTransient = env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId())
                .and(Criteria.equal("type", "wms.pick_out")).and(Criteria.equal("material_id",material.getId()))
                .and(Criteria.equal("lot_num", lotNum.getString("code"))).and(Criteria.equal("sn", sn)));
            if (lotSnTransient.any()) {
                // 存在,
                throw new ValidationException(record.l10n("当前批次标签已使用,序列号[%s],请扫描其他标签", sn));
            }
        }
        Records pickOutDetails = record.getEnv().get("wms.pick_out_details");
        Map<String, Object> result = createResultMap(record, material);
        result.put("sn", lotNum.get("code"));
        result.put("lot_in_qty", lotInQtyFlag);
        if (null != qty) {
            Records iqcDetails = getIqcDetails(record, lotNum, material);
            double detailQty = iqcDetails.getDouble("qty");
            if (Utils.less(detailQty, qty)) {
                throw new ValidationException(record.l10n("挑选数量超过当前批次入库数量"));
            }
            double totalQty = record.getDouble("qty");
            Records pickOutDetail = pickOutDetails.find(Criteria.equal("pick_out_id", record.get("id")).and(Criteria.equal("sn", lotNum.get("code"))));
            if (pickOutDetail.any()) {
                // 之前这个批次的数据就已经扫描过了,
                double baseQty = Utils.round(pickOutDetail.getDouble("qty") + qty);
                if (Utils.less(detailQty, baseQty)) {
                    throw new ValidationException(record.l10n("累计挑选数量超过当前批次入库数量"));
                }
                if (Utils.large(baseQty, totalQty)) {
                    throw new ValidationException(record.l10n("输入挑选数量合计大于检验数量,请检查"));
                }
                pickOutDetail.set("qty", baseQty);
            } else {
                if (Utils.large(qty, totalQty)) {
                    throw new ValidationException(record.l10n("输入挑选数量大于检验数量,请检查"));
                }
                result.put("qty", qty);
                pickOutDetails.create(result);
            }
            if (lotInQtyFlag) {
                // 不存在的就新增到临时表
                Map<String, Object> data = new HashMap<>();
                data.put("order_id", record.getId());
                data.put("sn", sn);
                data.put("material_id", material.getId());
                data.put("lot_num", lotNum.getString("code"));
                data.put("type", "wms.pick_out");
                env.get("lbl.lot_status").create(data);
            }
        } else {
            result.put("qty", labelQty);
        }
        return result;
    }

    public Records getIqcDetails(Records record, Records lotNum, Records material) {
        // 要控制数量不能大于入库单明细的数量,
        Environment env = record.getEnv();
        Records iqcSheet = env.get("iqc.sheet").find(Criteria.equal("code", record.getString("related_code")));
        // 来料接收单号
        Records iqcDetails = env.get("iqc.material_details").find(Criteria.equal("material_id", material.getId())
            .and(Criteria.equal("iqc_id", iqcSheet.getId())).and(Criteria.equal("lot_num", lotNum.getString("code"))));
        if (!iqcDetails.any()) {
            throw new ValidationException(record.l10n("当前标签无关联入库明细,不能挑选"));
        }
        return iqcDetails;
    }

    public Map<String, Object> pickSnMaterialLabel(Records record, Records label) {
        Records pickOutDetails = record.getEnv().get("wms.pick_out_details");
        Records records = pickOutDetails.find(Criteria.equal("sn", label.get("sn")));
        if (records.any()) {
            // 不重复扫描
            throw new ValidationException(record.l10n("当前标签已扫描"));
        }
        Records material = label.getRec("material_id");
        Map<String, Object> result = createResultMap(record, material);
        result.put("sn", label.get("sn"));
        result.put("qty", label.get("qty"));
        pickOutDetails.create(result);
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "wms.pick_out");
        log.put("related_id", record.getId());
        log.put("related_code", record.getString("code"));
        label.call("logStatus", log);
        return result;
    }

    public Map<String, Object> createResultMap(Records record, Records material) {
        Map<String, Object> result = new HashMap<>();
        result.put("material_id", material.get("id"));
        result.put("material_code", material.get("code"));
        result.put("material_name_spec", material.get("name_spec"));
        result.put("material_stock_rule", material.get("stock_rule"));
        result.put("pick_out_id", record.get("id"));
        result.put("status", "new");
        return result;
    }

    @Model.ServiceMethod(label = "审核", doc = "审核单据，从提交状态改为已审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
            Environment env = record.getEnv();
            Records pickOutDetails = env.get("wms.pick_out_details")
                .find(Criteria.equal("pick_out_id", record.getId()).and(Criteria.equal("status", "new")));
            pickOutDetails.set("status", "done");
            // 生成退供应商数据,
            // 首先需要明确,一张挑选单,实际物料不会有多条, 下面明细数据的物料都是与主表相同,挑选的时候,只看数量,不管其他,
            String type = record.getString("type");
            Records material = record.getRec("material_id");
            // 新增逻辑
            // 入库数据, 要扣减退货数量, 扣为0 为止 0 修改状态为已完成
            // 如果扣为0 ,则判断 所有明细数据是否为 已完成, 则修改主表单据状态为 已完成
            String stockRule = material.getString("stock_rule");
            if ("iqc.sheet".equals(type)) {
                pickOutIqcSheet(record, record.getString("pick_type"), material, stockRule, pickOutDetails);
            } else {
                throw new ValidationException(record.l10n("非来料检验单据,不能生成挑选单"));
            }
            env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId()).and(Criteria.equal("type", "wms.pick_out"))).delete();
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "done");
        return Action.reload(records.l10n("操作成功"));
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
        // 自动审核
        boolean pickOutAutoAudit = records.getEnv().getConfig().getBoolean("pick_out_auto_audit");
        if(pickOutAutoAudit){
            approve(records,records.l10n("自动审核"));
        }
        return Action.reload(records.l10n("操作成功"));
    }

    /*@ServiceMethod(label = "提交")
    public void submitPick(Records record, String pickType) {
        Environment env = record.getEnv();
        // 直接改状态,
        Records pickOutDetails = env.get("wms.pick_out_details")
            .find(Criteria.equal("pick_out_id", record.getId()).and(Criteria.equal("status", "new")));
        if (!pickOutDetails.any()) {
            throw new ValidationException(record.l10n("无可挑选数据"));
        }
        record.set("status", "done");
        record.set("pick_type", pickType);
        pickOutDetails.set("status", "done");
        // 生成退供应商数据,
        // 首先需要明确,一张挑选单,实际物料不会有多条, 下面明细数据的物料都是与主表相同,挑选的时候,只看数量,不管其他,
        String type = record.getString("type");
        Records material = record.getRec("material_id");
        // 新增逻辑
        // 入库数据, 要扣减退货数量, 扣为0 为止 0 修改状态为已完成
        // 如果扣为0 ,则判断 所有明细数据是否为 已完成, 则修改主表单据状态为 已完成
        String stockRule = material.getString("stock_rule");
        if ("iqc.sheet".equals(type)) {
            pickOutIqcSheet(record, pickType, env, material, stockRule, pickOutDetails);
        } else {
            throw new ValidationException(record.l10n("非来料检验单据,不能生成挑选单"));
        }
    }*/

    public void pickOutIqcSheet(Records record, String pickType, Records material, String stockRule, Records pickOutDetails) {
        Environment env = record.getEnv();
        Records iqcDetails = env.get("iqc.material_details");
        // 来料检验单
        Records iqcSheet = env.get("iqc.sheet").find(Criteria.equal("code", record.getString("related_code")));
        // 来料接收单号
        String relatedCode = iqcSheet.getString("related_code");
        Records materialReceipt = env.get("wms.material_receipt").find(Criteria.equal("code", relatedCode));
        Criteria criteria = Criteria.equal("stock_in_id.related_code", relatedCode)
            .and(Criteria.equal("material_id", material.getId())).and(Criteria.equal("iqc_id", iqcSheet.getId()));
        // 来料退货
        if ("sn".equals(stockRule)) {
            pickOutSn(record, pickType, material, pickOutDetails, iqcDetails, criteria, materialReceipt);
        } else if ("lot".equals(stockRule)) {
            pickOutLot(record, pickType, material, pickOutDetails, iqcDetails, criteria, materialReceipt);
        } else {
            pickOutNum(record, pickType, material, pickOutDetails, iqcDetails, criteria, materialReceipt);
        }
    }

    public void pickOutNum(Records record, String pickType, Records material, Records pickOutDetails, Records iqcDetails, Criteria criteria, Records materialReceipt) {
        // 数量管控, 这种只要物料相同,那就只会有一条数据
        iqcDetails = iqcDetails.find(criteria);
        double detailQty = iqcDetails.getDouble("qty");
        double pickQty = pickOutDetails.getDouble("qty");
        if ("pick_stock".equals(pickType)) {
            // 挑选入库
            double round = Utils.round(detailQty - pickQty);
            iqcDetails.set("return_qty", round);
            iqcDetails.set("status", "to-stock");
            createReturnSupplier(record, materialReceipt, Utils.round(detailQty - pickQty), material, iqcDetails);
        } else {
            iqcDetails.set("return_qty", pickQty);
            if (Utils.equals(detailQty, pickQty)) {
                iqcDetails.set("status", "done");
            } else {
                iqcDetails.set("status", "to-stock");
            }
            // 挑选退货
            createReturnSupplier(record, materialReceipt, pickQty, material, iqcDetails);
        }
    }

    public void pickOutLot(Records record, String pickType, Records material, Records pickOutDetails, Records iqcDetails, Criteria criteria, Records materialReceipt) {
        // 批号
        Map<String, Double> snQtyMap = pickOutDetails.stream().collect(Collectors.toMap(e -> e.getString("sn"), e -> e.getDouble("qty")));
        Set<String> snList = snQtyMap.keySet();
        // 挑选出来的数量,
        double baseQty = snQtyMap.values().stream().mapToDouble(e -> e).sum();
        // 未命中sn的数据
        Records notHitSnDetails = iqcDetails.find(Criteria.notIn("lot_num", snList).and(criteria));
        double notHitQty = notHitSnDetails.stream().mapToDouble(e -> e.getDouble("qty")).sum();
        // 命中sn的数据
        Records hitSnDetail = iqcDetails.find(Criteria.in("lot_num", snList).and(criteria));
        double hitQty = hitSnDetail.stream().mapToDouble(e -> e.getDouble("qty")).sum();
        // 可能多个批号,并且每个批号退一部分, 这块如何处理
        if ("pick_stock".equals(pickType)) {
            pickOutLotInStock(record, material, materialReceipt, notHitSnDetails, notHitQty, hitQty, baseQty, hitSnDetail, snQtyMap);
        } else {
            // 未命中的就要入库
            pickOutLotReturn(record, material, materialReceipt, baseQty, hitSnDetail, snQtyMap, notHitSnDetails);
        }
    }

    public void pickOutLotReturn(Records record, Records material, Records materialReceipt, double baseQty, Records hitSnDetail, Map<String, Double> snQtyMap, Records notHitSnDetails) {
        // 挑选退货
        // 这种需要处理数量,不能直接改0
        for (Records detail : hitSnDetail) {
            String sn = detail.getString("lot_num");
            double detailQty = detail.getDouble("qty");
            Double pickQty = snQtyMap.get(sn);
            if (Utils.equals(detailQty, pickQty)) {
                // 相等
                detail.set("status", "done");
                detail.set("return_qty", detail.get("qty"));
            } else {
                // 这里只能是小于, 需要再前面控制住,不能超过当前送货批次号数量
                detail.set("return_qty", pickQty);
                detail.set("status", "to-stock");
            }
        }
        notHitSnDetails.set("status", "to-stock");
        createReturnSupplier(record, materialReceipt, baseQty, material, hitSnDetail);
    }

    public void pickOutLotInStock(Records record, Records material, Records materialReceipt, Records notHitSnDetails, double notHitQty, double hitQty, double baseQty, Records hitSnDetail, Map<String, Double> snQtyMap) {
        // 挑选入库
        // 可能有好几个批号数据一起入, 也可能只有一条或者当前扫码的数据
        if (notHitSnDetails.any()) {
            // 未被选中的 + 挑选剩余的,
            // 扣减入库明细数量
            notHitSnDetails.set("status", "done");
            for (Records notHitSnDetail : notHitSnDetails) {
                notHitSnDetail.set("return_qty", notHitSnDetail.get("qty"));
            }
            // 实际上,他的这个数量只是不超过检验数,是否对应得上入库数,前面需要控制住,或者这里根本不控制,只管数量
            for (Records detail : hitSnDetail) {
                String sn = detail.getString("lot_num");
                Double pickQty = snQtyMap.get(sn);
                // 这里只能是小于, 需要再前面控制住,不能超过当前送货批次号数量
                double round = Utils.round(detail.getDouble("qty") - pickQty);
                detail.set("return_qty", round);
                detail.set("status", Utils.equals(round, 0d) ? "done" : "to-stock");
            }
            createReturnSupplier(record, materialReceipt, Utils.round(notHitQty + hitQty - baseQty), material, notHitSnDetails.union(hitSnDetail));
        } else {
            // 需要先设置退货数量,下面的创建
            for (Records detail : hitSnDetail) {
                String sn = detail.getString("lot_num");
                Double pickQty = snQtyMap.get(sn);
                // 这里只能是小于, 需要再前面控制住,不能超过当前送货批次号数量
                double round = Utils.round(detail.getDouble("qty") - pickQty);
                detail.set("return_qty", round);
                detail.set("status", Utils.equals(round, 0d) ? "done" : "to-stock");
            }
            // 当前入库单,只有这几条批次数据,并且所有的批次数据都有要退货的数量
            createReturnSupplier(record, materialReceipt, Utils.round(hitQty - baseQty), material, hitSnDetail);
            // 这种需要处理数量,不能直接改0
        }
    }

    public void pickOutSn(Records record, String pickType, Records material, Records pickOutDetails, Records iqcDetails, Criteria criteria, Records materialReceipt) {
        // 序列号
        // 这种按照标签来退料,将明细对应数量改为0, 状态为已完成,最后如果所有的明细数据都是已完成,那就当前入库单状态改为已完成
        List<String> snList = pickOutDetails.stream().map(e -> e.getString("sn")).collect(Collectors.toList());

        double totalQty = 0d;
        if ("pick_stock".equals(pickType)) {
            // 挑选入库
            // 这里查的是没被挑选的明细,
            iqcDetails = iqcDetails.find(Criteria.notIn("sn", snList).and(criteria));
            totalQty = iqcDetails.stream().mapToDouble(e -> e.getDouble("qty")).sum();
            // 剩余数据,待入库
            iqcDetails.find(Criteria.in("sn", snList).and(criteria)).set("status", "to-stock");
        } else {
            // 挑选退货
            iqcDetails = iqcDetails.find(Criteria.in("sn", snList).and(criteria));
            totalQty = iqcDetails.stream().mapToDouble(e -> e.getDouble("qty")).sum();
            // 剩余数据,待入库
            iqcDetails.find(Criteria.notIn("sn", snList).and(criteria)).set("status", "to-stock");
        }
        // 将要退货的sn标签状态改为退货,
        List<String> sn = iqcDetails.stream().map(e -> e.getString("sn")).collect(Collectors.toList());
        record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", sn)).set("status", "return");
        iqcDetails.set("status", "done");
        iqcDetails.stream().forEach(e -> e.set("return_qty", e.getDouble("qty")));
        createReturnSupplier(record, materialReceipt, totalQty, material, iqcDetails);
    }

    public void createReturnSupplier(Records record, Records materialReceipt, double totalQty, Records material, Records iqcDetails) {
        Environment env = record.getEnv();
        // 退货 直接退供应商
        Records returnSupplier = env.get("wms.return_supplier");
        Records returnSupplierLine = env.get("wms.return_supplier_line");
        Map<String, Object> createMap = new HashMap<>();
        createMap.put("supplier_id", materialReceipt.getRec("supplier_id").getId());
        createMap.put("return_date", new Date());
        createMap.put("company_id", env.getCompany().getId());
        createMap.put("related_code", record.get("code"));
        // 既然直接从挑选出来,生成的退供应商单,修改退供应商单据状态为已审核,
        createMap.put("status", "commit");
        returnSupplier = returnSupplier.create(createMap);
        String returnId = returnSupplier.getId();
        // 处理采购订单号
        // 收料明细数据汇总
        Records materialReciptLine = materialReceipt.getRec("line_ids");
        Map<String, Double> poLineAndQtyMap = materialReciptLine.stream().filter(e ->
                Utils.large(e.getDouble("receive_qty"), 0d) && e.getRec("material_id").getId().equals(material.getId()))
            .collect(Collectors.toMap(e -> e.getRec("po_line_id").getId(), e -> e.getDouble("receive_qty")));
        Map<String, Object> poLineIdAndQtyMap = getPoLineIdAndQtyMap(record, totalQty, poLineAndQtyMap);
        Set<String> poLineSet = poLineIdAndQtyMap.keySet();
        createReturnSupplierLine(record, materialReciptLine, poLineSet, returnId, poLineIdAndQtyMap, returnSupplierLine);
        createReturnSupplierDetails(iqcDetails, returnId);
        // 建单以后,执行审核,
        returnSupplier.call("approve", "系统审核");
    }

    public void createReturnSupplierDetails(Records iqcDetails, String returnId) {
        Environment env = iqcDetails.getEnv();
        Records returnSupplierDetails = env.get("wms.return_supplier_details");
        // 批次号处理的时候, 每个批次都要退,   挑选入库, 挑选部分数据,
        for (Records iqcDetail : iqcDetails) {
            HashSet<String> keySet = new HashSet<>(iqcDetail.getMeta().getFields().keySet());
            // 挑选, 生成退供应商的时候不能有仓库,有仓库的是在库的数据
            keySet.remove("warehouse_id");
            List<Map<String, Object>> returnSupplierDetailsRead = iqcDetail.read(keySet);
            returnSupplierDetailsRead.forEach(e -> {
                e.put("return_id", returnId);
                e.put("qty", e.get("return_qty"));
            });
            returnSupplierDetails.createBatch(returnSupplierDetailsRead);
        }
    }

    public void createReturnSupplierLine(Records record, Records materialReciptLine, Set<String> poLineSet, String returnId, Map<String, Object> poLineIdAndQtyMap, Records returnSupplierLine) {
        Environment env = record.getEnv();
        Records mixinMaterialRecord = env.get("mixin.material");
        Set<String> keySet = mixinMaterialRecord.getMeta().getFields().keySet();
        HashSet<String> readField = new HashSet<>(keySet);
        readField.add("receive_qty");
        readField.add("po_line_id");
        readField.add("po_id");
        readField.remove("id");
        List<Map<String, Object>> read = materialReciptLine.read(readField).stream().filter(e -> poLineSet.contains(e.get("po_line_id"))).collect(Collectors.toList());
        if (read.isEmpty()) {
            throw new ValidationException(record.l10n("无法获取关联的收料物料明细数据,请检查"));
        } else {
            for (Map<String, Object> receiptLine : read) {
                receiptLine.put("return_id", returnId);
                receiptLine.put("status", "new");
                receiptLine.put("request_qty", poLineIdAndQtyMap.get(receiptLine.get("po_line_id")));
                receiptLine.put("return_qty", receiptLine.get("request_qty"));
            }
            returnSupplierLine.createBatch(read);
        }
    }

    public Map<String, Object> getPoLineIdAndQtyMap(Records records, double totalQty, Map<String, Double> poLineAndQtyMap) {
        Map<String, Object> insertMap = new HashMap<>();
        for (Map.Entry<String, Double> stringDoubleEntry : poLineAndQtyMap.entrySet()) {
            // 采购订单, 实收数量
            String key = stringDoubleEntry.getKey();
            Double value = stringDoubleEntry.getValue();
            double oldValue = Utils.toDouble(insertMap.get(key));
            if (Utils.large(totalQty, value)) {
                // 大于
                insertMap.put(key, Utils.round(oldValue + value));
            } else {
                // 小于
                insertMap.put(key, Utils.round(oldValue + totalQty));
            }
            totalQty = Utils.round(totalQty - value);
            if (Utils.lessOrEqual(totalQty, 0d)) {
                break;
            }
        }
        return insertMap;
    }
}
