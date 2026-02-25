package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.util.KvMap;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "print.rule.transfer_order", label = "仓库调拨单打印规则", inherit = {"print.rule"}, authModel = "wms.transfer_order")
public class TransferOrderPrintRule extends AbstractModel {

    static Field rule = Field.Selection().addSelection(new LinkedHashMap<String, String>(16) {
        {
            put("print.rule.transfer_order", "仓库调拨单打印规则");
        }
    });

    /**
     * 获取打印字段
     */
    public Map<String, Object> getFields(Records rec) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("code", "单据编号");
        fields.put("sourceWarehouseIds", "原仓库");
        fields.put("targetWarehouseId", "目标仓库");
        fields.put("targetWarehouseContact", "目标仓库联系人");
        fields.put("requiredTime", "需求时间");
        fields.put("printTime", "打印时间");
        fields.put("remark", "备注");
        Map<String, Object> list = new HashMap<>();
        list.put("materialCode", "物料编码");
        list.put("materialName", "物料名称");
        list.put("materialSpec", "物料规格");
        list.put("unit", "单位");
        list.put("requestQty", "需求数");
        list.put("transferQty", "调拨数");
        fields.put("list", list);
        return fields;
    }

    public List<Map<String, Object>> getData(Records records, Map<String, Object> data) {
        List<Map<String, Object>> result = new ArrayList<>();
        Records transferOrder = (Records) data.get("transferOrder");
        if (transferOrder != null) {
            for (Records row : transferOrder) {
                Records source_warehouse_ids = row.getRec("source_warehouse_ids");
                Records target_warehouse_id = row.getRec("target_warehouse_id");
                List<Map<String, Object>> list = new ArrayList<>();
                Map<String, Object> map = new KvMap() {{
                    put("code", row.get("code"));
                    put("sourceWarehouseIds", source_warehouse_ids.stream().map(e -> e.getString("present")).collect(Collectors.joining(";")));
                    put("targetWarehouseId", target_warehouse_id.getString("present"));
                    put("targetWarehouseContact", row.get("target_warehouse_contact"));
                    put("requiredTime", Utils.format(row.getDateTime("required_time"), "yyyy-MM-dd"));
                    put("printTime", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                    put("remark", row.get("remark"));
                    put("list", list);
                }};
                for (Records line : row.getRec("line_ids")) {
                    Records material = line.getRec("material_id");
                    Map<String, Object> kvMap = new HashMap() {};
                    kvMap.put("materialCode", material.get("code"));
                    kvMap.put("materialName", material.get("name"));
                    kvMap.put("materialSpec", material.get("spec"));
                    kvMap.put("unit", line.getRec("unit_id").get("name"));
                    kvMap.put("requestQty", line.get("request_qty"));
                    kvMap.put("transferQty", line.get("transfer_qty"));
                    list.add(kvMap);
                }
                result.add(map);
            }
        }
        return result;
    }
}
