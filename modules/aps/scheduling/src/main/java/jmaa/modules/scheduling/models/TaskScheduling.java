package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.UserException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.fields.RelationalMultiField;
import org.jmaa.sdk.tools.DateUtils;
import org.jmaa.sdk.util.KvMap;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "as.task_scheduling", label = "车间排程")
public class TaskScheduling extends ValueModel {
    @ServiceMethod(label = "读取用户设置", auth = "read")
    public Map<String, Object> readUserSettings(Records records, List<String> fields) {
        Environment env = records.getEnv();
        Records settings = env.get("as.user_settings").find(Criteria.equal("user_id", env.getUserId()));
        if (!settings.any()) {
            settings = settings.create(new KvMap().set("user_id", env.getUserId()));
        }
        return settings.read(fields).get(0);
    }

    @ServiceMethod(label = "更新用户设置", auth = "read")
    public Object updateUserSettings(Records records, Map<String, Object> values) {
        Records settings = records.getEnv().get("as.user_settings").find(Criteria.equal("user_id", records.getEnv().getUserId()));
        if (!settings.any()) {
            values.put("user_id", records.getEnv().getUserId());
            settings.create(values);
        } else {
            settings.update(values);
        }
        return Action.success();
    }

    @ServiceMethod(label = "加载数据", auth = "read", ids = false)
    public Object loadData(Records records) {
        Environment env = records.getEnv();
        env.getContext().put("usePresent", false);
        KvMap result = new KvMap();
        List<String> fields = env.get("as.user_settings").getMeta().getFields().keySet().stream()
            .filter(key -> key.startsWith("task_")).collect(Collectors.toList());
        fields.add("hide_resource_ids");
        Map<String, Object> settings = readUserSettings(records, fields);
        List<String> invisibleResources = Utils.asList((String[]) settings.remove("hide_resource_ids"));
        result.put("settings", settings);
        Records permission = env.get("as.scheduler_permission").find(Criteria.equal("scheduler_id.user_id", env.getUserId())
            .and("scheduler_id.active", "=", true), 0, 0, "resource_id.seq");
        if (!permission.any()) {
            result.put("holidays", Collections.emptyList());
            result.put("resources", Collections.emptyMap());
            result.put("calendars", Collections.emptyMap());
            result.put("craftProcess", Collections.emptyMap());
            result.put("craftQuota", Collections.emptyList());
            result.put("craftOrderQuota", Collections.emptyMap());
            result.put("auxiliaryResources", Collections.emptyMap());
            result.put("tasks", Collections.emptyMap());
            result.put("materials", Collections.emptyMap());
            result.put("productOrders", Collections.emptyMap());
            return result;
        }
        Date begin = DateUtils.addDays(new Date(), -Utils.toInt(settings.get("task_begin_offset")));
        Date end = DateUtils.addDays(new Date(), Utils.toInt(settings.get("task_end_offset")));
        result.put("holidays", loadHoliday(records, begin, end));
        Set<String> calendarIds = new HashSet<>();
        result.put("resources", loadResource(records, permission, begin, end, calendarIds, invisibleResources));
        result.put("calendars", loadCalendar(records, calendarIds, begin));
        result.put("craftProcess", loadCraftProcess(records));
        result.put("craftQuota", loadCraftQuota(records));
        result.put("craftOrderQuota", loadCraftOrderQuota(records, begin, end));
        Records tasks = records.getEnv().get("as.plan_task").find(Criteria.binary("plan_end", ">=", begin)
            .and("plan_start", "<=", end));
        result.putAll(getTaskData(records, tasks));
        List<String> moldIds = new ArrayList<>();
        for (Records row : tasks) {
            Records mold = row.getRec("mold_id");
            if (mold.any()) {
                moldIds.add(mold.getId());
            }
        }
        if (!moldIds.isEmpty()) {
            result.putAll(loadMoldResource(records, moldIds));
        }
        return result;
    }

    public Map<String, Object> getTaskData(Records records, Records task) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> taskData = new HashMap<>();
        Map<String, Object> materialData = new HashMap<>();
        Map<String, Object> productOrderData = new HashMap<>();
        result.put("tasks", taskData);
        result.put("materials", materialData);
        result.put("productOrders", productOrderData);
        if (task.any()) {
            List<String> fields = task.getMeta().getFields().values().stream().filter(f -> !f.isAuto()
                && !(f instanceof RelationalMultiField)).map(f -> f.getName()).collect(Collectors.toList());
            List<String> detailFields = task.getEnv().get("as.plan_task_details").getMeta().getFields().values().stream()
                .filter(f -> !f.isAuto()).map(f -> f.getName()).collect(Collectors.toList());
            Set<String> materialIds = new HashSet<>();
            Set<String> productOrderIds = new HashSet<>();
            for (Records row : task) {
                Map<String, Object> values = row.read(fields).get(0);
                Records details = row.getRec("details_ids");
                values.put("details", details.read(detailFields));
                taskData.put(row.getId(), values);
                for (Records detail : details) {
                    materialIds.add(detail.getRec("material_id").getId());
                    productOrderIds.add(detail.getRec("product_order_id").getId());
                }
            }
            List<String> materialFields = Utils.asList("code", "name_spec");
            for (Records row : records.getEnv().get("md.material", materialIds)) {
                materialData.put(row.getId(), row.read(materialFields).get(0));
            }
            List<String> productOrderFields = Utils.asList("code", "plan_qty", "factory_id", "customer_due_date", "customer_id", "sales_order_id");
            for (Records row : records.getEnv().get("mfg.product_order", productOrderIds).withContext("usePresent", true)) {
                productOrderData.put(row.getId(), row.read(productOrderFields).get(0));
            }
        }
        return result;
    }

    public List<Map<String, Object>> loadHoliday(Records records, Date begin, Date end) {
        Records holiday = records.getEnv().get("md.work_holiday")
            .find(Criteria.binary("end_date", ">=", begin).and("start_date", "<=", end));
        return holiday.read(Utils.asList("name", "start_date", "end_date"));
    }

    public List<Map<String, Object>> loadCraftQuota(Records records) {
        Records quota = records.getEnv().get("as.craft_quota").find(new Criteria());
        List<String> fields = Utils.asList("craft_process_id", "resource_id", "material_id", "priority", "cycle_time", "transfer_time");
        return quota.read(fields);
    }

    @ServiceMethod(label = "读取机模配", auth = "read")
    public Map<String, Object> loadMoldResource(Records records, List<String> moldIds) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> moldProductData = new HashMap<>();
        Map<String, Object> moldResourceData = new HashMap<>();
        Map<String, Object> moldData = new HashMap<>();
        result.put("moldProduct", moldProductData);
        result.put("moldResource", moldResourceData);
        result.put("molds", moldData);
        if (Utils.isNotEmpty(moldIds)) {
            Records molds = records.getEnv().get("md.sub_resource", moldIds);
            for (Records row : molds) {
                moldData.put(row.getId(), new KvMap()
                    .set("code", row.get("code"))
                    .set("model_id", row.getRec("model_id").getPresent()));
            }
            Records moldProduct = records.getEnv().get("md.sub_resource_product").find(
                Criteria.in("sub_resource_id", moldIds).and("active", "=", true));
            moldProductData.putAll(moldProduct.read(Utils.asList("sub_resource_id", "cycle_time", "material_id", "qty")).stream()
                .collect(Collectors.groupingBy(r -> (String) r.get("sub_resource_id"))));
            Records moldModel = records.getEnv().get("md.sub_resource_model").find(
                Criteria.in("sub_resource_ids", moldProductData.keySet()));
            List<Map<String, Object>> moldResources = (List<Map<String, Object>>) moldModel.call("getModelResources");
            moldResourceData.putAll(moldResources.stream()
                .collect(Collectors.groupingBy(r -> (String) r.get("model_id"))));
        }
        return result;
    }

    /**
     * 加载任务的制造单工时定额
     */
    public Map<String, Object> loadCraftOrderQuota(Records records, Date begin, Date end) {
        Map<String, Object> result = new HashMap<>();
        Cursor cr = records.getEnv().getCursor();
        cr.execute("select distinct q.id from as_craft_order_quota q join as_plan_task_details d" +
                " on q.order_id=d.craft_order_id join as_plan_task t on d.task_id=t.id where t.plan_end>=%s and t.plan_start<=%s",
            Utils.asList(begin, end));
        List<String> ids = cr.fetchAll().stream().map(row -> Utils.toString(row[0])).collect(Collectors.toList());
        Records quota = records.getEnv().get("as.craft_order_quota", ids);
        List<String> fields = quota.getMeta().getFields().values().stream().filter(f -> !f.isAuto()).map(f -> f.getName()).collect(Collectors.toList());
        for (Records row : quota) {
            String orderId = row.getRec("order_id").getId();
            List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(orderId);
            if (list == null) {
                list = new ArrayList<>();
                result.put(orderId, list);
            }
            list.add(row.read(fields).get(0));
        }
        return result;
    }

    /**
     * 加载制程工艺的工时定额
     */
    public Map<String, Object> loadCraftProcess(Records records) {
        Records craftProcess = records.getEnv().get("md.craft_process").withContext("usePresent", true)
            .find(Criteria.equal("active", true));
        Map<String, Object> result = new HashMap<>();
        List<String> fields = Utils.asList("name", "craft_type_id", "transfer_time", "cycle_time");
        for (Records row : craftProcess) {
            result.put(row.getId(), row.read(fields).get(0));
        }
        return result;
    }

    /**
     * 加载资源，按时间范围加载资源的特殊时间，计算资源的工作日历
     */
    public Map<String, Object> loadResource(Records records, Records permission, Date begin, Date end, Set<String> calendarIds, List<String> invisibleResources) {
        Environment env = records.getEnv();
        Map<String, Object> result = new LinkedHashMap<>();
        Records specialTimes = env.get("as.special_time").withNewContext(Collections.emptyMap())
            .find(Criteria.binary("date", ">=", begin).and("date", "<=", end));
        Records defaultCalendar = env.get("md.work_calendar").find(Criteria.equal("is_default", true).and("active", "=", true));
        for (Records row : permission) {
            Records resource = row.getRec("resource_id");
            Map<String, List<Map<String, Object>>> specialTime = loadSpecialTime(records, specialTimes, resource.getId());
            Records calendar = resource.getRec("calendar_id");
            if (!calendar.any()) {
                calendar = defaultCalendar;
            }
            if (!calendar.any()) {
                throw new ValidationException(env.l10n("资源[%s]没有设置工作日历", resource.get("present")));
            }
            calendarIds.add(calendar.getId());
            Records workshop = row.getRec("workshop_id");
            KvMap map = new KvMap().set("id", resource.getId())
                .set("present", resource.get("present"))
                .set("name", resource.get("name"))
                .set("special_time", specialTime)
                .set("craft_type_id", resource.getRec("craft_type_id").getId())
                .set("calendar_id", calendar.getId())
                .set("visible", !invisibleResources.contains(resource.getId()))
                .set("seq", resource.get("seq"))
                .set("readonly", "read".equals(row.get("permission")))
                .set("factory", workshop.getRec("parent_id").get("name"))
                .set("workshop", String.format("%s(%s)", workshop.get("name"), workshop.get("code")));
            result.put(resource.getId(), map);
        }
        return result;
    }

    /**
     * 加载工作日历，包含周方案和班次的工作时间
     */
    public Map<String, Object> loadCalendar(Records records, Collection<String> calendarIds, Date begin) {
        Environment env = records.getEnv();
        Map<String, Object> result = new HashMap<>();
        Records calendar = env.get("md.work_calendar", calendarIds);
        Records weeks = env.get("md.work_week").find(Criteria.in("calendar_id", calendarIds)
            .and("begin_date", "<=", begin), 0, 0, "begin_date desc");
        List<String> calendarFields = Utils.asList("code", "name");
        List<String> weekFields = Utils.asList("begin_date", "shift_id", "mon", "tue", "wed", "thu", "fri", "sat", "sun");
        Set<String> shiftIds = new HashSet<>();
        for (Records row : calendar) {
            Map<String, Object> values = row.read(calendarFields).get(0);
            Records calendarWeeks = weeks.filter(r -> r.getRec("calendar_id").equals(row));
            if (!calendarWeeks.any()) {
                throw new ValidationException(env.l10n("工作日历[%s]没有有效周方案", row.get("present")));
            }
            for (Records w : calendarWeeks) {
                shiftIds.add(w.getRec("shift_id").getId());
                values.put("weeks", w.read(weekFields));
            }
            result.put(row.getId(), values);
        }
        Records shifts = env.get("md.work_shift", shiftIds);
        Map<String, Object> shiftData = new HashMap<>();
        for (Records row : shifts) {
            shiftData.put(row.getId(), loadShiftTime(records, row));
        }
        result.put("shift_ids", shiftData);
        return result;
    }

    public List<Map<String, Object>> loadShiftTime(Records records, Records shift) {
        Records times = records.getEnv().get("md.work_shift_time").find(Criteria.equal("shift_id", shift.getId()), 0, 0, "start_time");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Records row : times) {
            KvMap m = new KvMap()
                .set("day_id", row.getRec("day_id").getPresent())
                .set("start_time", row.get("start_time"))
                .set("start", getMinutes(row.getString("start_time")))
                .set("end_time", row.get("end_time"))
                .set("end", getMinutes(row.getString("end_time")))
                .set("next_day", row.get("next_day"))
                .set("is_ot", row.get("is_ot"));
            result.add(m);
        }
        return result;
    }

    static double getMinutes(String time) {
        try {
            Date begin = Utils.Dates.parseDate("2000-01-01 00:00:00", "yyyy-MM-DD HH:mm:ss");
            Date end = Utils.Dates.parseDate("2000-01-01 " + time, "yyyy-MM-DD HH:mm:ss");
            return Utils.round((end.getTime() - begin.getTime()) / 1000 / 60);
        } catch (Exception e) {
            throw new UserException("日期时间解析失败", e);
        }
    }

    /**
     * 加载指定资源的特殊时间
     */
    public Map<String, List<Map<String, Object>>> loadSpecialTime(Records records, Records specialTime, String resourceId) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        Records times = specialTime.filter(p -> resourceId.equals(p.getRec("resource_id").getId()));
        List<String> fields = Utils.asList("is_rest", "start_hour", "end_hour", "remark", "resource_id", "date");
        for (Records time : times) {
            String date = Utils.format(time.getDate("date"), "yyyy-MM-dd");
            List<Map<String, Object>> list = result.get(date);
            if (list == null) {
                list = new ArrayList<>();
                result.put(date, list);
            }
            list.add(time.read(fields).get(0));
        }
        return result;
    }

    @ServiceMethod(label = "保存计划任务", auth = "read")
    public Map<String, Object> saveTasks(Records records,
                                         @Doc("新建任务") List<Map<String, Object>> toCreate,
                                         @Doc("更新任务") List<Map<String, Object>> toUpdate,
                                         @Doc("删除任务") List<String> toDelete) {
        Records task = records.getEnv().get("as.plan_task").withContext("#autoId", false);
        if (Utils.isNotEmpty(toCreate)) {
            task = task.createBatch(toCreate);
        }
        if (Utils.isNotEmpty(toUpdate)) {
            for (Map<String, Object> row : toUpdate) {
                String id = (String) row.remove("id");
                task.browse(id).update(row);
            }
        }
        if (Utils.isNotEmpty(toDelete)) {
            task.browse(toDelete).delete();
        }
        return getTaskData(records, task);
    }
}
