@Manifest(
    name = "md-work",
    label = "生产基础数据",
    category = "基础数据",
    author = "JMAA",
    license = "LGPL v3",
    models = {
        Staff.class,
        WorkTeam.class,
        WorkShift.class,
        WorkShiftDay.class,
        WorkShiftRest.class,
        WorkShiftOvertime.class,
        WorkShiftTime.class,
        WorkCalendar.class,
        WorkWeek.class,
        WorkHoliday.class,
        WorkResource.class,
    },
    demo = {
        "demo/md.work_holiday.csv",
        "demo/md.work_calendar.csv",
        "demo/md.work_shift.csv",
        "demo/md.work_shift_day.csv",
        "demo/md.work_week.csv",
        "demo/md.work_team.csv",
        "demo/md.staff.csv",
        "demo/md.work_resource.csv",
    },
    data = {
        "views/menus.xml",
        "views/staff.xml",
        "views/work_team.xml",
        "views/work_calendar.xml",
        "views/work_shift.xml",
        "views/work_holiday.xml",
        "views/work_resource.xml",
    },
    depends = {
        "md-resource"
    },
    application = false)
package jmaa.modules.md.work;

import jmaa.modules.md.work.models.*;
import org.jmaa.sdk.Manifest;
