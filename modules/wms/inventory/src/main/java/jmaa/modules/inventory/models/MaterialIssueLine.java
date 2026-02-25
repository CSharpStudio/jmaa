package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author eric
 */
@Model.Meta(inherit = "mfg.material_issue_line")
public class MaterialIssueLine extends Model {
    @OnSaved("status")
    public void onStatusSave(Records records) {
        Set<String> issueIds = new HashSet<>();
        for (Records row : records) {
            if ("issued".equals(row.getString("status"))) {
                Records issue = row.getRec("issue_id");
                issueIds.add(issue.getId());
            }
        }
        if (!issueIds.isEmpty()) {
            records.flush();
            Cursor cr = records.getEnv().getCursor();
            for (String id : issueIds) {
                cr.execute("select distinct status from mfg_material_issue_line where issue_id=%s", Utils.asList(id));
                boolean issued = cr.fetchAll().stream().allMatch(r -> "issued".equals(r[0]) || "done".equals(r[0]));
                if (issued) {
                    records.getEnv().get("mfg.material_issue", id).call("stockOut");
                }
            }
        }
    }
}
