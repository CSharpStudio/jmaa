package jmaa.modules.wms.qc.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.exceptions.ValidationException;

import java.sql.Date;

@Model.Meta(name = "iqc.exemption_list", label = "来料免检清单", inherit = "mixin.material")
public class IqcExemptionList extends Model {
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field begin_date = Field.Date().label("开始时间").required();
    static Field end_date = Field.Date().label("结束时间").required();

    @Constrains({"begin_date", "end_date"})
    public void dateConstrains(Records records) {
        for (Records record : records) {
            Date begin = record.getDate("begin_date");
            Date end = record.getDate("end_date");
            if (end.before(begin)) {
                throw new ValidationException(record.l10n("开始日期[%s]不能大于结束时间[%s]",
                    Utils.format(begin, "yyyy-MM-dd"), Utils.format(end, "yyyy-MM-dd")));
            }
        }
    }
}
