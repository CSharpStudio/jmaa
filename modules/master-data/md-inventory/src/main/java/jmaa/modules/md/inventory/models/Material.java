package jmaa.modules.md.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author eric
 */
@Model.Meta(inherit = "md.material")
public class Material extends Model {
    static Field stock_class_id = Field.Many2one("md.stock_class").label("仓储分类");
    static Field store_limit = Field.Selection(Selection.related("md.stock_class", "store_limit")).defaultValue("none").label("存储位置");
    static Field warehouse_ids = Field.Many2many("md.warehouse", "md_material_warehouse", "material_id", "warehouse_id").label("仓库");
    static Field store_location_ids = Field.Many2many("md.store_location", "md_material_location", "material_id", "store_location_id").label("库位");

    /**
     * 查找推荐的收货仓库
     *
     * @param record
     * @return
     */
    public Records findWarehouse(Records record) {
        String storeLimit = record.getString("store_limit");
        if ("warehouse".equals(storeLimit)) {
            Records warehouseIds = record.getRec("warehouse_ids");
            if (!warehouseIds.any()) {
                throw new ValidationException(record.l10n("物料[%s]存储位置为仓库，请维护数据", record.get("code")));
            }
            return warehouseIds.first();
        } else if ("location".equals(storeLimit)) {
            Records storeLocationIds = record.getRec("store_location_ids");
            if (!storeLocationIds.any()) {
                throw new ValidationException(record.l10n("物料[%s]存储位置为库位，请维护数据", record.get("code")));
            }
            return storeLocationIds.first().getRec("warehouse_id");
        }
        Records stockClass = record.getRec("stock_class_id");
        if (stockClass.any()) {
            storeLimit = stockClass.getString("store_limit");
            if ("warehouse".equals(storeLimit)) {
                Records warehouseIds = stockClass.getRec("warehouse_ids");
                if (!warehouseIds.any()) {
                    throw new ValidationException(record.l10n("物料[%s]仓储分类[%s]存储位置为仓库，请维护数据", record.get("code"), stockClass.get("present")));
                }
                return warehouseIds.first();
            } else if ("location".equals(storeLimit)) {
                Records storeLocationIds = stockClass.getRec("store_location_ids");
                if (!storeLocationIds.any()) {
                    throw new ValidationException(record.l10n("物料[%s]仓储分类[%s]存储位置库位，请维护数据", record.get("code"), stockClass.get("present")));
                }
                return storeLocationIds.first().getRec("warehouse_id");
            }
        }
        return record.getEnv().get("md.warehouse");
    }

    public Records findWarehouses(Records record) {
        String storeLimit = record.getString("store_limit");
        Function<Records, Records> getWarehouse = (location) -> {
            Set<String> warehouseIds = new HashSet<>();
            for (Records r : location) {
                warehouseIds.add(r.getRec("warehouse_id").getId());
            }
            return record.getEnv().get("md.warehouse", warehouseIds);
        };
        if ("warehouse".equals(storeLimit)) {
            return record.getRec("warehouse_ids");
        } else if ("location".equals(storeLimit)) {
            return getWarehouse.apply(record.getRec("store_location_ids"));
        }
        Records stockClass = record.getRec("stock_class_id");
        if (stockClass.any()) {
            storeLimit = stockClass.getString("store_limit");
            if ("warehouse".equals(storeLimit)) {
                return stockClass.getRec("warehouse_ids");
            } else if ("location".equals(storeLimit)) {
                return getWarehouse.apply(stockClass.getRec("store_location_ids"));
            }
        }
        return record.getEnv().get("md.warehouse");
    }

    public Records findLocation(Records record) {
        String storeLimit = record.getString("store_limit");
        if ("location".equals(storeLimit)) {
            return record.getRec("store_location_ids");
        }
        Records stockClass = record.getRec("stock_class_id");
        if (stockClass.any()) {
            storeLimit = stockClass.getString("store_limit");
            if ("location".equals(storeLimit)) {
                return stockClass.getRec("store_location_ids");
            }
        }
        return record.getEnv().get("md.store_location");
    }
}
