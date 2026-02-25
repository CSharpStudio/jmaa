package jmaa.modules.inventory.models.report;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.BinaryOp;

@Model.Meta(name = "stock.in_details_report",label = "待入库数据报表明细",inherit = "stock.stock_in_details")
public class StockInDetailsReport extends ValueModel {
    static Field stock_in_report_id = Field.Many2one("stock.in_report");

    @Override
    public long count(Records rec, Criteria criteria) {
        String detailId = null;
        for (Object item : criteria) {
            BinaryOp binaryOp = (BinaryOp) item;
            Object field = binaryOp.getField();
            if ("stock_in_report_id".equals(field)) {
                criteria.remove(binaryOp);
                detailId = (String) binaryOp.getValue();
                break;
            }
        }
        Records stockInDetail = rec.getEnv().get("stock.stock_in_details", detailId);
        criteria.and(Criteria.equal("material_id", stockInDetail.getRec("material_id").getId())
            .and(Criteria.equal("stock_in_id", stockInDetail.getRec("stock_in_id").getId())));
        return stockInDetail.count(criteria);
    }
}
