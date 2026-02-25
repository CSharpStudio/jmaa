package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.Expression;
import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.util.TextBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Model.Service(remove = "@edit")
@Model.Meta(name = "wms.return_supplier_mobile", label = "退供应商-移动端", inherit = "wms.return_supplier", table = "wms_return_supplier")
public class ReturnSupplierMobile extends ValueModel {

    @ServiceMethod(label = "读取退供应商单列表", doc = "退供应商单号、物料编码、标签条码查询收料单", auth = "read")
    public Object searchReturnOrder(Records record, String keyword, Collection<String> fields, Integer limit, Integer offset, String order) {
        Criteria criteria = new Criteria();
        boolean hasSnOrLot = false;
        if (Utils.isNotEmpty(keyword)) {
            Records material = record.getEnv().get("md.material").find(Criteria.equal("code", keyword));
            if (material.any()) {
                criteria.and(Criteria.equal("a.material_id", material.getId()).and("return_id.status", "=", "approve"));
            } else {
                // 解析标签
                String[] codes = (String[]) record.getEnv().get("lbl.code_parse").call("parse", keyword);
                if (codes.length > 1) {
                    material = record.getEnv().get("md.material").find(Criteria.equal("code", codes[1]));
                    criteria.and("return_id.status", "=", "approve");
                    String stockRule = material.getString("stock_rule");
                    if ("sn".equals(stockRule)) {
                        hasSnOrLot = true;
                        criteria.and(Criteria.equal("c.sn", codes[0]));
                    } else if ("lot".equals(stockRule) && codes.length > 2) {
                        hasSnOrLot = true;
                        criteria.and(Criteria.equal("c.lot_num", codes[2]));
                    } else if (material.any()) {
                        criteria.and(Criteria.equal("a.material_id", material.getId()));
                    }
                } else {
                    criteria.and(Criteria.in("return_id.status", Arrays.asList("approve", "done")));
                    // 如果是直接批次码和条码的情况
                    hasSnOrLot = true;
                    criteria.and(Criteria.like("return_id.code", keyword).or(Criteria.equal("c.sn", keyword)).or(Criteria.equal("c.lot_num", keyword)));
                }
            }
        } else {
            criteria.and("return_id.status", "=", "approve");
        }
        Cursor cursor = record.getEnv().getCursor();
        SqlFormat where = Expression.toWhereSql(criteria);
        TextBuilder sql = new TextBuilder();
        sql.append("SELECT return_id.id FROM wms_return_supplier_line a JOIN wms_return_supplier return_id on a.return_id = return_id.id ");
        if (hasSnOrLot) {
            sql.append(" JOIN stock_stock_out_details c ON return_id.id = c.return_id ");
        }
        sql.append(" WHERE").append(where.getSql());
        sql.append(" GROUP BY return_id.id ");
        if (Utils.isNotBlank(order)) {
            sql.append(" ORDER BY ").append(order);
        }
        String querySql = cursor.getSqlDialect().getPaging(sql.toString(), limit, offset);
        cursor.execute(querySql, where.getParmas());
        List<String> ids = cursor.fetchAll().stream().map(r -> Utils.toString(r[0])).collect(Collectors.toList());
        return record.browse(ids).read(fields);
    }

    @ServiceMethod(label = "扫描条码", doc = "物料编码则查询退货需求，物料标签则退货", auth = "read")
    public Object scanCode(Records record, @Doc("标签条码/物料编码") String code) {
        return record.getEnv().get("wms.return_supplier", record.getId()).call("scanCode", code);
    }

    @ServiceMethod(label = "扫描以后,确定", auth = "read")
    public Object submitScanCode(Records record, String materialId, String warehouseId, String locationId, String sn, Double qty) {
        return record.getEnv().get("wms.return_supplier", record.getId()).call("submitScanCode", materialId, warehouseId, locationId, sn, qty);
    }

    @ServiceMethod(label = "出库", doc = "根据退货明细生成出库单", auth = "read")
    public Object stockOut(Records records, String comment) {
        return records.getEnv().get("wms.return_supplier", records.getIds()).call("stockOut", comment);
    }

    @ServiceMethod(auth = "read", label = "删除已扫描的标签")
    public void deleteDetail(Records records) {
        records.getEnv().get("wms.return_supplier_details", records.getIds()).call("deleteDetails");
    }

    @ServiceMethod(label = "拆分标签", doc = "拆分生成新标签", auth = "read")
    public Object splitLabel(Records record, @Doc("条码") String sn, @Doc("拆分数量") Double splitQty, @Doc("是否打印原标签") Boolean printOld) {
        return record.getEnv().get("wms.return_supplier", record.getIds()).call("splitLabel", sn, splitQty, printOld);
    }
}
