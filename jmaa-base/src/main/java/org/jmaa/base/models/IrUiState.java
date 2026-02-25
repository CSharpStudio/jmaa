package org.jmaa.base.models;

import com.alibaba.fastjson.JSON;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.util.KvMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "ir.ui.state", label = "视图状态")
public class IrUiState extends Model {
    static Field user_id = Field.Many2one("rbac.user").label("用户").ondelete(DeleteMode.Cascade);
    static Field view_id = Field.Char().label("视图");
    static Field field_order = Field.Text().label("字段顺序");
    static Field field_hidden = Field.Text().label("字段隐藏");

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "保存视图状态")
    public void saveViewState(Records rec, String viewId, Map<String, Object> values) {
        String userId = rec.getEnv().getUserId();
        Records record = rec.find(Criteria.equal("user_id", userId).and(Criteria.equal("view_id", viewId)));
        if (record.any()) {
            record.update(values);
        } else {
            values.put("user_id", userId);
            values.put("view_id", viewId);
            record.create(values);
        }
    }

    @ServiceMethod(auth = Constants.ANONYMOUS, label = "加载视图状态")
    public Map<String, Object> loadViewState(Records rec, String viewId) {
        Records record = rec.find(Criteria.equal("user_id", rec.getEnv().getUserId()).and(Criteria.equal("view_id", viewId)));
        if (record.any()) {
            return record.read(Arrays.asList("field_order", "field_hidden")).get(0);
        }
        return Collections.emptyMap();
    }
}
