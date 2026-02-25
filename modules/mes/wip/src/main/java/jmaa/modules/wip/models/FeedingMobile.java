package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "mfg.feeding_mobile", label = "移动端上料")
@Model.Service(remove = "@edit")
public class FeedingMobile extends ValueModel {
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间").required();
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源").required();
    static Field process_id = Field.Many2one("md.work_process").label("工序").required();
    static Field station_id = Field.Many2one("md.work_station").label("工位").required();
    static Field station_material_ids = Field.One2many("mfg.work_station_material", "").label("工位库存");
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单").required();

    @ActionMethod
    public Action onStationChange(Records record) {
        Records station = record.getRec("station_id");
        AttrAction action = new AttrAction();
        action.setValue("process_id", station.getRec("process_id"));
        Records resource = station.getRec("resource_id");
        action.setValue("resource_id", resource);
        action.setValue("workshop_id", resource.getRec("workshop_id"));
        return action;
    }

    @ServiceMethod(label = "加载工位", auth = "read")
    public Object loadStation(Records records, String code) {
        Records station = records.getEnv().get("md.work_station").find(Criteria.equal("code", code));
        if (!station.any()) {
            throw new ValidationException(records.l10n("工位[%s]不存在", code));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("station_id", station.getPresent());
        result.put("workshop_id", station.getRec("workshop_id").getPresent());
        result.put("resource_id", station.getRec("resource_id").getPresent());
        return result;
    }

    @ServiceMethod(label = "上料", auth = "read")
    public Object feeding(Records records, String code, double qty, String stationId, String workOrderId) {
        return records.getEnv().get("mfg.work_station_material").call("feeding", code, qty, stationId, workOrderId);
    }

    @ServiceMethod(label = "读取工位库存信息", auth = "read")
    public Object loadWorkStationMaterial(Records records, String dataId, List<String> fields) {
        Records station = records.getEnv().get("mfg.work_station_material", dataId);
        return station.read(fields).get(0);
    }

    @ServiceMethod(label = "查找工位库存", auth = "read", doc = "根据工位，条码查找工位库存")
    public Map<String, Object> findStationMaterial(Records records, String stationId, String code, List<String> fields) {
        return (Map<String, Object>) records.getEnv().get("mfg.work_station_material").call("findStationMaterial", stationId, code, fields);
    }

    @ServiceMethod(label = "下料")
    public Object unload(Records record,
                         @Doc("条码") String code,
                         @Doc("下料数量") double qty,
                         @Doc("是否打印") boolean print) {
        return record.getEnv().get("mfg.work_station_material", record.getIds()).call("unload", code, qty, print);
    }

    @ServiceMethod(label = "查询工位库存", auth = "read")
    public Object searchStock(Records records, String stationId, String code, List<String> fields, Integer offset, Integer limit) {
        return records.getEnv().get("mfg.work_station_material").call("searchStock", stationId, code, fields, offset, limit);
    }
}
