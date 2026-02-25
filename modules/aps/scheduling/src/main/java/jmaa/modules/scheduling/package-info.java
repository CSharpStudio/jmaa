@Manifest(
    name = "scheduling",
    label = "生产排程",
    category = "高级计划排程",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        CraftQuota.class,
        CraftOrder.class,
        CraftOrderQuota.class,
        TaskScheduling.class,
        ProductOrder.class,
        UserSettings.class,
        ToPlanOrder.class,
        Scheduler.class,
        SchedulerPermission.class,
        SpecialTime.class,
        PlanTask.class,
        PlanTaskDetails.class,
        WorkOrder.class,
        Scheme.class,
        SchemeDetails.class,
        SchemeSort.class,
        SchemeFilter.class,
    },
    data = {
        "data/code_coding.xml",
        "views/menus.xml",
        "views/craft_quota.xml",
        "views/task_scheduling.xml",
        "views/product_order.xml",
        "views/craft_order.xml",
        "views/user_settings.xml",
        "views/to_plan_order.xml",
        "views/scheduler.xml",
        "views/plan_task.xml",
        "views/scheme.xml",
    },
    depends = {
        "md-craft",
        "md-resource",
        "md-product",
        "md-work",
        "manufacturing",
    })
package jmaa.modules.scheduling;

import jmaa.modules.scheduling.models.*;
import org.jmaa.sdk.Manifest;
