package jmaa.modules.inventory.models.report;

import org.jmaa.sdk.*;

import java.util.Map;

/**
 * @author 梁荣振
 */
public class ReportBase extends Model {
    public ReportBase() {
        isAuto = false;
    }

    protected String getCodeName(String code, String name) {
        String project = Utils.toString(code);
        if (Utils.isNotEmpty(name)) {
            project += " (" + name + ")";
        }
        return project;
    }

    protected String getNameSpec(String name, String spec) {
        if (Utils.isEmpty(spec)) {
            return name;
        }
        return name + "/" + spec;
    }

    protected void updateData(Map<String, Object> row) {
        row.put("name_spec", getNameSpec((String) row.get("material_name"), (String) row.get("spec")));
        row.put("ok_qty", Utils.round(Utils.toDouble(row.get("ok_qty"))));
        row.put("frozen_qty", Utils.round(Utils.toDouble(row.get("frozen_qty"), 0D)));
        row.put("ng_qty", Utils.round(Utils.toDouble(row.get("ng_qty"))));
        row.put("allot_qty", Utils.round(Utils.toDouble(row.get("allot_qty"))));
        row.put("total_qty", Utils.round(Utils.toDouble(row.get("ok_qty")) + Utils.toDouble(row.get("ng_qty"))));
        //row.put("available_qty", Utils.round(Utils.toDouble(row.get("ok_qty")) - Utils.toDouble(row.get("allot_qty")) - Utils.toDouble(row.get("frozen_qty"))));
        row.put("available_qty", Utils.round(Utils.toDouble(row.get("usable_qty"))));
    }
}
