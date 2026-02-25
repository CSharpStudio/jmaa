package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.*;

@Model.Meta(name = "wip.batch_code", label = "批次条码")
@Model.Service(remove = "@edit")
public class BatchCode extends Model {
    static Field code = Field.Char().label("编码").index().unique();
    static Field qty = Field.Float().label("数量").greaterThen(0d).required();
    static Field work_order_id = Field.Many2one("mfg.work_order").label("生产工单").required();
    static Field material_id = Field.Many2one("md.material").related("work_order_id.material_id").label("产品编码");
    static Field material_name_spec = Field.Char().related("work_order_id.material_id.name_spec").label("规格型号");
    static Field unit_id = Field.Many2one("md.unit").related("work_order_id.material_id.unit_id").label("单位");
    static Field template_id = Field.Many2one("print.template").label("打印模板").required();
    static Field supplier_id = Field.Many2one("md.supplier").label("生产商");
    static Field product_date = Field.Date().label("生产日期");

    @ServiceMethod(label = "打印标签")
    public Object print(Records record,
                        @Doc("工单") String workOrderId,
                        @Doc("打印模板") String templateId,
                        @Doc("编码规则") String codingId,
                        @Doc("生产日期") Date productDate,
                        @Doc("生产厂商") String supplierId,
                        @Doc("打印数量") double printQty,
                        @Doc("批次大小") double batchQty) {
        Records workOrder = record.getEnv().get("mfg.work_order", workOrderId);
        Records material = workOrder.getRec("material_id");
        Records supplier = record.getEnv().get("md.supplier", supplierId);
        int count = (int) Math.ceil(printQty / batchQty);
        List<String> codes = (List<String>) record.getEnv().get("code.coding", codingId).call("createCodes", count, Collections.emptyMap());
        List<Map<String, Object>> list = new ArrayList<>();
        List<Map<String, Object>> toCreate = new ArrayList<>();
        double leftQty = printQty;
        for (String code : codes) {
            double qty = Math.min(leftQty, batchQty);
            leftQty = Utils.round(leftQty - batchQty);
            list.add(new KvMap()
                .set("code", code)
                .set("qty", qty)
                .set("work_order_code", workOrder.get("code"))
                .set("product_date", productDate)
                .set("supplier_code", supplier.get("code"))
                .set("supplier_name", supplier.get("name"))
                .set("material_code", material.get("code"))
                .set("material_name", material.get("name"))
                .set("material_spec", material.get("spec"))
                .set("unit", material.getRec("unit_id").get("name")));
            toCreate.add(new KvMap()
                .set("work_order_id", workOrderId)
                .set("code", code)
                .set("template_id", templateId)
                .set("product_date", productDate)
                .set("supplier_id", supplierId)
                .set("qty", qty));
        }
        record.createBatch(toCreate);
        Records printTemplate = record.getEnv().get("print.template", templateId);
        Map<String, Object> result = new HashMap<>();
        Object printData = printTemplate.call("print", new KvMap().set("data", list));
        result.put("printData", printData);
        result.put("codes", codes);
        return result;
    }

    @ServiceMethod(label = "补打印标签")
    public Object reprint(Records records) {
        if (!records.any()) {
            throw new ValidationException("请选择要打印的标签");
        }
        Records printTemplate = records.first().getRec("template_id");
        for (Records rec : records) {
            if (!Utils.equals(rec.getRec("template_id").getId(), printTemplate.getId())) {
                throw new ValidationException("补打的标签模板不一致，不能一起打印");
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Records record : records) {
            Records workOrder = record.getRec("work_order_id");
            Records material = workOrder.getRec("material_id");
            Records supplier = record.getRec("supplier_id");
            list.add(new KvMap()
                .set("code", record.get("code"))
                .set("qty", record.get("qty"))
                .set("product_date", record.getDate("product_date"))
                .set("work_order_code", workOrder.get("code"))
                .set("material_code", material.get("code"))
                .set("material_name", material.get("name"))
                .set("material_spec", material.get("spec"))
                .set("supplier_code", supplier.get("code"))
                .set("supplier_name", supplier.get("name"))
                .set("unit", material.getRec("unit_id").get("name")));
        }
        return printTemplate.call("print", new KvMap().set("data", list));
    }
}
