//@ sourceURL=job.js
jmaa.editor('cron_editor', {
    css: 'cron',
    getTpl: function () {
        return `<div class="input-group">
                    <input id="${this.getId()}" class="form-control" readonly="readonly"/>
                    <div class="input-suffix btn-dropdown">
                        <i class="fa fa-clock"></i>
                    </div>
                    <div class="dropdown-menu dropdown-menu-right tabs">
                        <div class="d-flex">
                            <div class="tab-panel flex-fill">
                                <ul class="nav nav-tabs" id="CronGenTabs">
                                    <li class="tab-head"><a data-toggle="pill" class="nav-link active" id="MinutesTab" href="#Minutes">${'分钟'.t()}</a></li>
                                    <li class="tab-head"><a data-toggle="pill" class="nav-link" id="HourlyTab" href="#Hourly">${'小时'.t()}</a></li>
                                    <li class="tab-head"><a data-toggle="pill" class="nav-link" id="DailyTab" href="#Daily">${'日'.t()}</a></li>
                                    <li class="tab-head"><a data-toggle="pill" class="nav-link" id="MonthlyTab" href="#Monthly">${'月'.t()}</a></li>
                                    <li class="tab-head"><a data-toggle="pill" class="nav-link" id="WeeklyTab" href="#Weekly">${'周'.t()}</a></li>
                                    <li class="tab-head"><a data-toggle="pill" class="nav-link" id="YearlyTab" href="#Yearly">${'年'.t()}</a></li>
                                </ul>
                                <div class="tab-content">
                                    <div class="tab-pane active" id="Minutes">
                                        <div class="line"><input type="radio" value="1" name="min">${'每分钟 允许的通配符[, - * /]<'.t()}/div>
                                        <div class="line"><input type="radio" value="2" name="min">${'周期(分钟)'.t()}<input type="text" id="minStart_0" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">-<input type="text" id="minEnd_0" value="2" style="width:35px; height:20px; text-align: center; margin: 0 3px;"></div>
                                        <div class="line"><input type="radio" value="3" name="min">${'从'.t()}<input type="text" id="minStart_1" value="0" style="width:35px; height:20px; text-align: center; margin: 0 3px;">分钟开始,每<input type="text" id="minEnd_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">分钟执行一次</div>
                                        <div class="line"><input type="radio" value="4" name="min" id="min_appoint">${'指定'.t()}</div>
                                        <div class="imp minList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="0">00<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="1">01<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="2">02<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="3">03<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="4">04<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="5">05<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="6">06<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="7">07<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="8">08<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="9">09</div>
                                        <div class="imp minList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="10">10<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="11">11<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="12">12<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="13">13<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="14">14<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="15">15<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="16">16<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="17">17<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="18">18<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="19">19</div>
                                        <div class="imp minList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="20">20<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="21">21<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="22">22<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="23">23<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="24">24<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="25">25<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="26">26<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="27">27<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="28">28<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="29">29</div>
                                        <div class="imp minList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="30">30<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="31">31<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="32">32<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="33">33<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="34">34<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="35">35<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="36">36<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="37">37<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="38">38<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="39">39</div>
                                        <div class="imp minList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="40">40<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="41">41<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="42">42<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="43">43<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="44">44<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="45">45<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="46">46<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="47">47<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="48">48<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="49">49</div>
                                        <div class="imp minList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="50">50<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="51">51<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="52">52<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="53">53<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="54">54<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="55">55<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="56">56<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="57">57<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="58">58<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="59">59</div>
                                        <input type="hidden" id="minHidden" value="*">
                                    </div>
                                    <div class="tab-pane" id="Hourly">
                                        <div class="line"><input type="radio" value="1" name="hour">${'每小时 允许的通配符[, - * /]'.t()}</div>
                                        <div class="line"><input type="radio" value="2" name="hour">${'周期(小时)'.t()}<input type="text" id="hourStart_0" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">-<input type="text" id="hourEnd_0" value="2" style="width:35px; height:20px; text-align: center; margin: 0 3px;"></div>
                                        <div class="line"><input type="radio" value="3" name="hour">${'从'.t()}<input type="text" id="hourStart_1" value="0" style="width:35px; height:20px; text-align: center; margin: 0 3px;">小时开始,每<input type="text" id="hourEnd_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">小时执行一次</div>
                                        <div class="line"><input type="radio" value="4" name="hour" id="hour_appoint">${'指定'.t()}</div>
                                        <div class="imp hourList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="0">00<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="1">01<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="2">02<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="3">03<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="4">04<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="5">05</div>
                                        <div class="imp hourList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="6">06<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="7">07<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="8">08<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="9">09<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="10">10<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="11">11</div>
                                        <div class="imp hourList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="12">12<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="13">13<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="14">14<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="15">15<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="16">16<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="17">17</div>
                                        <div class="imp hourList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="18">18<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="19">19<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="20">20<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="21">21<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="22">22<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="23">23</div><input type="hidden" id="hourHidden">
                                        <input type="hidden" id="hourHidden">
                                    </div>
                                    <div class="tab-pane" id="Daily">
                                        <div class="line"><input type="radio" value="1" name="day">${'每天 允许的通配符[, - * / L W]'.t()}</div>
                                        <div class="line"><input type="radio" value="2" name="day">${'不指定'.t()}</div>
                                        <div class="line"><input type="radio" value="3" name="day">${'周期(日)'.t()}<input type="text" id="dayStart_0" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">-<input type="text" id="dayEnd_0" value="2" style="width:35px; height:20px; text-align: center; margin: 0 3px;"></div>
                                        <div class="line"><input type="radio" value="4" name="day">${'从'.t()}<input type="text" id="dayStart_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">日开始,每<input type="text" id="dayEnd_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">天执行一次</div>
                                        <div class="line"><input type="radio" value="5" name="day">${'每月'.t()}<input type="text" id="dayStart_2" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">号最近的那个工作日</div>
                                        <div class="line"><input type="radio" value="6" name="day">${'本月最后一天'.t()}</div>
                                        <div class="line"><input type="radio" value="7" name="day" id="day_appoint">${'指定'.t()}</div>
                                        <div class="imp dayList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="1">01<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="2">02<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="3">03<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="4">04<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="5">05<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="6">06<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="7">07<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="8">08<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="9">09<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="10">10</div>
                                        <div class="imp dayList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="11">11<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="12">12<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="13">13<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="14">14<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="15">15<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="16">16<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="17">17<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="18">18<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="19">19<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="20">20</div>
                                        <div class="imp dayList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="21">21<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="22">22<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="23">23<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="24">24<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="25">25<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="26">26<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="27">27<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="28">28<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="29">29<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="30">30</div>
                                        <div class="imp dayList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="31">31</div>
                                        <input type="hidden" id="dayHidden">
                                    </div>
                                    <div class="tab-pane" id="Monthly">
                                        <div class="line"><input type="radio" value="1" name="month">${'每月 允许的通配符[, - * /]'.t()}</div>
                                        <div class="line d-none"><input type="radio" value="2" name="month">${'不指定'.t()}</div>
                                        <div class="line"><input type="radio" value="3" name="month">${'周期(月)'.t()}<input type="text" id="monthStart_0" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">-<input type="text" id="monthEnd_0" value="2" style="width:35px; height:20px; text-align: center; margin: 0 3px;"></div>
                                        <div class="line"><input type="radio" value="4" name="month">${'从'.t()}<input type="text" id="monthStart_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">日开始,每<input type="text" id="monthEnd_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">月执行一次</div>
                                        <div class="line"><input type="radio" value="5" name="month" id="month_appoint">${'指定'.t()}</div>
                                        <div class="imp monthList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="1">01<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="2">02<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="3">03<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="4">04<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="5">05<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="6">06</div>
                                        <div class="imp monthList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="7">07<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="8">08<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="9">09<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="10">10<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="11">11<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="12">12</div><input type="hidden" id="monthHidden">
                                    </div>
                                    <div class="tab-pane" id="Weekly">
                                        <div class="line"><input type="radio" value="1" name="week">${'每周 允许的通配符[, - * / L #]'.t()}</div>
                                        <div class="line"><input type="radio" value="2" name="week">${'不指定'.t()}</div>
                                        <div class="line"><input type="radio" value="3" name="week">${'周'.t()}<input type="text" id="weekStart_0" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">-<input type="text" id="weekEnd_0" value="2" style="width:35px; height:20px; text-align: center; margin: 0 3px;"></div>
                                        <div class="line d-none"><input type="radio" value="4" name="week">第<input type="text" id="weekStart_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;">周的星期<input type="text" id="weekEnd_1" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;"></div>
                                        <div class="line"><input type="radio" value="5" name="week">${'本月最后一个周'.t()}<input type="text" id="weekStart_2" value="1" style="width:35px; height:20px; text-align: center; margin: 0 3px;"></div>
                                        <div class="line"><input type="radio" value="6" name="week" id="week_appoint">${'指定'.t()}</div>
                                        <div class="imp weekList"><input type="checkbox" disabled="disabled" style="margin-left: 5px" value="1">1<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="2">2<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="3">3<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="4">4<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="5">5<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="6">6<input type="checkbox" disabled="disabled" style="margin-left: 5px" value="7">7</div>
                                        <div style="width:340px">${'说明：1=星期天，2=星期一，3=星期二，4=星期三，5=星期四，6=星期五，7=星期六'.t()}</div>
                                        <input type="hidden" id="weekHidden">
                                    </div>
                                    <div class="tab-pane" id="Yearly">
                                        <div class="line"><input type="radio" value="1" name="year">${'不指定 允许的通配符[, - * /] 非必填'.t()}</div>
                                        <div class="line"><input type="radio" value="2" name="year" checked="checked">${'每年'.t()}</div>
                                        <div class="line"><input type="radio" value="3" name="year">${'周期(年)'.t()}<input type="text" id="yearStart_0" value="2025" style="width:45px; height:20px;">-<input type="text" id="yearEnd_0" value="2035" style="width:45px; height:20px;"></div>
                                        <input type="hidden" id="yearHidden">
                                    </div>
                                </div>
                            </div>
                            <div class="cron-preview">
                                <div class="cron-value"></div>
                                <div class="preview-title">${'最近5次运行时间'.t()}</div>
                                <div class="preview-value"></div>
                            </div>
                        </div>
                        <div class="d-flex justify-content-between pt-2 pl-3 pr-3 border-top">
                            <button type="button" class="btn btn-flat btn-default btn-cancel">${'取消'.t()}</button>
                            <button type="button" class="btn btn-flat btn-info btn-confirm">${'确认'.t()}</button>
                        </div>
                    </div>
                </div>`;
    },
    init: function () {
        let me = this;
        me.dom.html(me.getTpl()).on('click', '.btn-dropdown', function (e) {
            e.stopPropagation();
            me.dom.find('.dropdown-menu').toggleClass('show');
            me.cronParse(me.getValue());
            me.generate();
        }).on('click', '.btn-cancel', function (e) {
            me.dom.find('.dropdown-menu').removeClass('show');
        }).on('click', '.btn-confirm', function (e) {
            me.dom.find('.dropdown-menu').removeClass('show');
            me.setValue(me.dom.find('.cron-value').html());
        });
        me.minAppoint();
        me.hourAppoint();
        me.dayAppoint();
        me.monthAppoint();
        me.weekAppoint();
        me.dom.find('.tabs input').change(function () {
            me.generate();
        });
    },
    genMin() {
        let me = this;
        switch (me.dom.find("input:radio[name=min]:checked").val()) {
            case "1":
                me.everyTime("min");
                return me.cronResult();
            case "2":
                me.cycle("min");
                return me.cronResult();
            case "3":
                me.startOn("min");
                return me.cronResult();
            case "4":
                return me.cronResult();
        }
    },
    genHour() {
        let me = this;
        switch (me.dom.find("input:radio[name=hour]:checked").val()) {
            case "1":
                me.everyTime("hour");
                return me.cronResult();
            case "2":
                me.cycle("hour");
                return me.cronResult();
            case "3":
                me.startOn("hour");
                return me.cronResult();
            case "4":
                return me.cronResult();
        }
    },
    genDay() {
        let me = this;
        switch (me.dom.find("input:radio[name=day]:checked").val()) {
            case "1":
                me.everyTime("day");
                return me.cronResult();
            case "2":
                me.unAppoint("day");
                return me.cronResult()
            case "3":
                me.cycle("day");
                return me.cronResult();
            case "4":
                me.startOn("day");
                return me.cronResult();
            case "5":
                me.workDay("day");
                return me.cronResult();
            case "6":
                me.lastDay("day");
                return me.cronResult();
            case "7":
                return me.cronResult();
        }
    },
    genWeek() {
        let me = this;
        switch (me.dom.find("input:radio[name=week]:checked").val()) {
            case "1":
                me.everyTime("week");
                return me.cronResult();
            case "2":
                me.unAppoint("week");
                return me.cronResult()
            case "3":
                me.cycle("week");
                return me.cronResult();
            case "4":
                me.startOn("week");
                return me.cronResult();
            case "5":
                me.lastWeek("week");
                return me.cronResult();
            case "6":
                return me.cronResult();
        }
    },
    genMonth() {
        let me = this;
        switch (me.dom.find("input:radio[name=month]:checked").val()) {
            case "1":
                me.everyTime("month");
                return me.cronResult();
            case "2":
                me.unAppoint("month");
                return me.cronResult()
            case "3":
                me.cycle("month");
                return me.cronResult();
            case "4":
                me.startOn("month");
                return me.cronResult();
            case "5":
                return me.cronResult();
        }
    },
    genYear() {
        let me = this;
        switch (me.dom.find("input:radio[name=year]:checked").val()) {
            case "1":
                me.unAppoint("year");
                return me.cronResult();
            case "2":
                me.everyTime("year");
                return me.cronResult();
                break;
            case "3":
                me.cycle("year");
                return me.cronResult();
        }
    },
    generate() {
        let me = this;
        let activeTab = me.dom.find("ul a.active").prop("id");
        if (!activeTab) {
            return;
        }
        let value = "";
        if (activeTab == "MinutesTab") {
            value = me.genMin();
        } else if (activeTab == "HourlyTab") {
            value = me.genHour();
        } else if (activeTab == "DailyTab") {
            value = me.genDay();
        } else if (activeTab == "WeeklyTab") {
            value = me.genWeek();
        } else if (activeTab == "MonthlyTab") {
            value = me.genMonth();
        } else if (activeTab == "YearlyTab") {
            value = me.genYear();
        }
        me.dom.find('.cron-value').html(value);
        me.refreshRunTime(value);
    },
    refreshRunTime(cron) {
        let me = this;
        jmaa.rpc({
            model: "job.task",
            module: me.module,
            method: "testJobByCron",
            args: {
                cron,
            },
            onsuccess: function (r) {
                let cron = [];
                for (let t of r.data) {
                    cron.push(`<div>${t}</div>`);
                }
                me.dom.find(".preview-value").html(cron.join(''))
            }
        });
    },
    cronResult: function () {
        let me = this;
        let minute = me.dom.find("#minHidden").val();
        minute = minute == "" ? "*" : minute;
        let hour = me.dom.find("#hourHidden").val();
        hour = hour == "" ? "*" : hour;
        let day = me.dom.find("#dayHidden").val();
        day = day == "" ? "*" : day;
        let month = me.dom.find("#monthHidden").val();
        month = month == "" ? "*" : month;
        let week = me.dom.find("#weekHidden").val();
        week = week == "" ? "?" : week;
        let year = me.dom.find("#yearHidden").val();
        if (year) {
            return "0 " + minute + " " + hour + " " + day + " " + month + " " + week + " " + year;
        }
        return "0 " + minute + " " + hour + " " + day + " " + month + " " + week;
    },
    /**
     * 每周期
     */
    everyTime: function (dom) {
        let me = this;
        me.dom.find(`#${dom}Hidden`).val("*");
        me.clearCheckbox(dom);
    },
    /**
     * 不指定
     */
    unAppoint: function (dom) {
        let me = this;
        let val = "?";
        if (dom == "year") {
            val = "";
        }
        me.dom.find(`#${dom}Hidden`).val(val);
        me.clearCheckbox(dom);
    },
    /**
     * 周期
     */
    cycle: function (dom) {
        let me = this;
        let start = me.dom.find(`#${dom}Start_0`).val();
        let end = me.dom.find(`#${dom}End_0`).val();
        me.dom.find(`#${dom}Hidden`).val(start + "-" + end);
        me.clearCheckbox(dom);
    },
    /**
     * 从开
     */
    startOn: function (dom) {
        let me = this;
        let start = me.dom.find(`#${dom}Start_1`).val();
        let end = me.dom.find(`#${dom}End_1`).val();
        me.dom.find(`#${dom}Hidden`).val(start + "/" + end);
        me.clearCheckbox(dom);
    },
    /**
     * 最后一天
     */
    lastDay: function (dom) {
        let me = this;
        me.dom.find(`#${dom}Hidden`).val("L");
        me.clearCheckbox(dom);
    },
    /**
     * 每周的某一天
     */
    weekOfDay: function (dom) {
        let me = this;
        let start = $(`#${dom}Start_0`).val();
        let end = $(`#${dom}End_0`).val();
        me.dom.find(`#${dom}Hidden`).val(start + "#" + end);
        me.clearCheckbox(dom);
    },
    /**
     * 最后一周
     */
    lastWeek: function (dom) {
        let me = this;
        let start = $(`#${dom}Start_2`).val();
        me.dom.find(`#${dom}Hidden`).val(start + "L");
        me.clearCheckbox(dom);
    },
    /**
     * 工作日
     */
    workDay(dom) {
        let me = this;
        let start = me.dom.find(`#${dom}Start_2`).val();
        me.dom.find(`#${dom}Hidden`).val(start + "W");
        me.clearCheckbox(dom);
    },
    weekAppoint() {
        let me = this;
        let weekList = me.dom.find(".weekList").children();
        me.dom.find("#week_appoint").click(function () {
            if (this.checked) {
                if ($(weekList).filter(":checked").length == 0) {
                    $(weekList.eq(0)).attr("checked", true);
                }
                weekList.eq(0).change();
                me.initCheckBox("week");
            }
        });
        weekList.change(function () {
            let week_appoint = me.dom.find("#week_appoint").prop("checked");
            if (week_appoint) {
                let vals = [];
                weekList.each(function () {
                    if (this.checked) {
                        vals.push(this.value);
                    }
                });
                let val = "?";
                if (vals.length > 0 && vals.length < 7) {
                    val = vals.join(",");
                } else if (vals.length == 7) {
                    val = "*";
                }
                me.dom.find("#weekHidden").val(val);
            }
        });
    },
    monthAppoint() {
        let me = this;
        let monthList = me.dom.find(".monthList").children();
        me.dom.find("#month_appoint").click(function () {
            if (this.checked) {
                if ($(monthList).filter(":checked").length == 0) {
                    $(monthList.eq(0)).attr("checked", true);
                }
                monthList.eq(0).change();
                me.initCheckBox("month");
            }
        });
        monthList.change(function () {
            let month_appoint = me.dom.find("#month_appoint").prop("checked");
            if (month_appoint) {
                let vals = [];
                monthList.each(function () {
                    if (this.checked) {
                        vals.push(this.value);
                    }
                });
                let val = "?";
                if (vals.length > 0 && vals.length < 12) {
                    val = vals.join(",");
                } else if (vals.length == 12) {
                    val = "*";
                }
                me.dom.find("#monthHidden").val(val);
            }
        });
    },
    dayAppoint() {
        let me = this;
        let dayList = me.dom.find(".dayList").children();
        me.dom.find("#day_appoint").click(function () {
            if (this.checked) {
                if ($(dayList).filter(":checked").length == 0) {
                    $(dayList.eq(0)).attr("checked", true);
                }
                dayList.eq(0).change();
                me.initCheckBox("day");
            }
        });
        dayList.change(function () {
            let day_appoint = me.dom.find("#day_appoint").prop("checked");
            if (day_appoint) {
                let vals = [];
                dayList.each(function () {
                    if (this.checked) {
                        vals.push(this.value);
                    }
                });
                let val = "?";
                if (vals.length > 0 && vals.length < 31) {
                    val = vals.join(",");
                } else if (vals.length == 31) {
                    val = "*";
                }
                me.dom.find("#dayHidden").val(val);
            }
        });
    },
    hourAppoint() {
        let me = this;
        let hourList = me.dom.find(".hourList").children();
        me.dom.find("#hour_appoint").click(function () {
            if (this.checked) {
                if ($(hourList).filter(":checked").length == 0) {
                    $(hourList.eq(0)).attr("checked", true);
                }
                hourList.eq(0).change();
                me.initCheckBox("hour");
            }
        });
        hourList.change(function () {
            let hour_appoint = me.dom.find("#hour_appoint").prop("checked");
            if (hour_appoint) {
                let vals = [];
                hourList.each(function () {
                    if (this.checked) {
                        vals.push(this.value);
                    }
                });
                let val = "?";
                if (vals.length > 0 && vals.length < 24) {
                    val = vals.join(",");
                } else if (vals.length == 24) {
                    val = "*";
                }
                me.dom.find("#hourHidden").val(val);
            }
        });
    },
    minAppoint() {
        let me = this;
        let minList = me.dom.find(".minList").children();
        me.dom.find("#min_appoint").click(function () {
            if (this.checked) {
                if ($(minList).filter(":checked").length == 0) {
                    $(minList.eq(0)).attr("checked", true);
                }
                minList.eq(0).change();
                me.initCheckBox("min");
            }
        });
        minList.change(function () {
            let min_appoint = $("#min_appoint").prop("checked");
            if (min_appoint) {
                let vals = [];
                minList.each(function () {
                    if (this.checked) {
                        vals.push(this.value);
                    }
                });
                let val = "?";
                if (vals.length > 0 && vals.length < 60) {
                    val = vals.join(",");
                } else if (vals.length == 60) {
                    val = "*";
                }
                me.dom.find("#minHidden").val(val);
            }
        });
    },
    cronParse(cronExpress) {
        let me = this;
        if (cronExpress) {
            let regs = cronExpress.split(' ');
            me.dom.find("#minHidden").val(regs[1]);
            me.dom.find("#hourHidden").val(regs[2]);
            me.dom.find("#dayHidden").val(regs[3]);
            me.dom.find("#monthHidden").val(regs[4]);
            me.dom.find("#weekHidden").val(regs[5]);
            me.initObj(regs[1], "min");
            me.initObj(regs[2], "hour");
            me.initDay(regs[3]);
            me.initMonth(regs[4]);
            me.initWeek(regs[5]);
            if (regs.length > 6) {
                me.dom.find("#yearHidden").val(regs[6]);
                me.initYear(regs[6]);
            }
        }
    },
    initYear(strVal) {
        let me = this;
        let ary = null;
        let objRadio = me.dom.find("input[name='year'");
        if (strVal == "*") {
            objRadio.eq(1).prop("checked", "checked");
        } else if (strVal.split('-').length > 1) {
            ary = strVal.split('-');
            objRadio.eq(2).prop("checked", "checked");
            me.dom.find("#yearStart_0").val(ary[0]);
            me.dom.find("#yearEnd_0").val(ary[1]);
        }
    },
    initWeek(strVal) {
        let me = this;
        let ary = null;
        let objRadio = me.dom.find("input[name=week]");
        if (strVal == "*") {
            objRadio.eq(0).prop("checked", "checked");
        } else if (strVal == "?") {
            objRadio.eq(1).prop("checked", "checked");
        } else if (strVal.split('/').length > 1) {
            ary = strVal.split('/');
            objRadio.eq(2).prop("checked", "checked");
            me.dom.find("#weekStart_0").val(ary[0]);
            me.dom.find("#weekEnd_0").val(ary[1]);
        } else if (strVal.split('-').length > 1) {
            ary = strVal.split('-');
            objRadio.eq(3).prop("checked", "checked");
            me.dom.find("#weekStart_1").val(ary[0]);
            me.dom.find("#weekEnd_1").val(ary[1]);
        } else if (strVal.split('L').length > 1) {
            ary = strVal.split('L');
            objRadio.eq(4).prop("checked", "checked");
            me.dom.find("#weekStart_2").val(ary[0]);
        } else {
            objRadio.eq(5).prop("checked", "checked");
            ary = strVal.split(",");
            for (var i = 0; i < ary.length; i++) {
                me.dom.find(`.weekList input[value=${ary[i]}]`).prop("checked", "checked");
            }
            me.initCheckBox("week");
        }
    },
    initMonth(strVal) {
        let me = this;
        let ary = null;
        let objRadio = me.dom.find("input[name=month]");
        if (strVal == "*") {
            objRadio.eq(0).prop("checked", "checked");
        } else if (strVal == "?") {
            objRadio.eq(1).prop("checked", "checked");
        } else if (strVal.split('-').length > 1) {
            ary = strVal.split('-');
            objRadio.eq(2).prop("checked", "checked");
            me.dom.find("#monthStart_0").val(ary[0]);
            me.dom.find("#monthEnd_0").val(ary[1]);
        } else if (strVal.split('/').length > 1) {
            ary = strVal.split('/');
            objRadio.eq(3).prop("checked", "checked");
            me.dom.find("#monthStart_1").val(ary[0]);
            me.dom.find("#monthEnd_1").val(ary[1]);

        } else {
            objRadio.eq(4).prop("checked", "checked");
            ary = strVal.split(",");
            for (var i = 0; i < ary.length; i++) {
                me.dom.find(`.monthList input[value=${ary[i]}]`).prop("checked", "checked");
            }
            me.initCheckBox("month");
        }
    },
    initDay(strVal) {
        let me = this;
        let ary = null;
        let objRadio = me.dom.find("input[name=day]");
        if (strVal == "*") {
            objRadio.eq(0).prop("checked", "checked");
        } else if (strVal == "?") {
            objRadio.eq(1).prop("checked", "checked");
        } else if (strVal.split('-').length > 1) {
            ary = strVal.split('-');
            objRadio.eq(2).prop("checked", "checked");
            me.dom.find("#dayStart_0").val(ary[0]);
            me.dom.find("#dayEnd_0").val(ary[1]);
        } else if (strVal.split('/').length > 1) {
            ary = strVal.split('/');
            objRadio.eq(3).prop("checked", "checked");
            me.dom.find("#dayStart_1").val(ary[0]);
            me.dom.find("#dayEnd_1").val(ary[1]);
        } else if (strVal.split('W').length > 1) {
            ary = strVal.split('W');
            objRadio.eq(4).prop("checked", "checked");
            me.dom.find("#dayStart_2").val(ary[0]);
        } else if (strVal == "L") {
            objRadio.eq(5).prop("checked", "checked");
        } else {
            objRadio.eq(6).prop("checked", "checked");
            ary = strVal.split(",");
            for (var i = 0; i < ary.length; i++) {
                me.dom.find(`.dayList input[value=${ary[i]}]`).prop("checked", "checked");
            }
            me.initCheckBox("day");
        }
    },
    initObj(strVal, strid) {
        let me = this;
        let ary = null;
        let objRadio = me.dom.find(`input[name=${strid}]`);
        if (strVal == "*") {
            objRadio.eq(0).prop("checked", "checked");
        } else if (strVal.split('-').length > 1) {
            ary = strVal.split('-');
            objRadio.eq(1).prop("checked", "checked");
            me.dom.find(`#${strid}Start_0`).val(ary[0]);
            me.dom.find(`#${strid}End_0`).val(ary[1]);
        } else if (strVal.split('/').length > 1) {
            ary = strVal.split('/');
            objRadio.eq(2).prop("checked", "checked");
            me.dom.find(`#${strid}Start_1`).val(ary[0]);
            me.dom.find(`#${strid}End_1`).val(ary[1]);
        } else {
            objRadio.eq(3).prop("checked", "checked");
            if (strVal != "?") {
                ary = strVal.split(",");
                for (var i = 0; i < ary.length; i++) {
                    me.dom.find(`.${strid}List input[value=${ary[i]}]`).prop("checked", "checked");
                }
                me.initCheckBox(strid);
            }
        }
    },
    initCheckBox(dom) {
        let me = this;
        let list = me.dom.find(`.${dom}List`).children();
        if (list.length > 0) {
            $.each(list, function (index) {
                $(this).removeAttr("disabled");
            });
        }
    },
    clearCheckbox(dom) {
        let me = this;
        let list = me.dom.find(`.${dom}List`).children();
        if (list.length > 0) {
            $.each(list, function (index) {
                $(this).prop('checked', '')
                $(this).attr("disabled", "disabled");
            });
        }
    },
    onValueChange: function (handler) {
        let me = this;
        me.dom.find('input.form-control').on('change', function (e) {
            handler(e, me);
        });
    },
    getValue: function () {
        let me = this;
        return me.dom.find('input.form-control').val().trim();
    },
    setValue: function (v) {
        let me = this;
        let value = me.getValue();
        if (v != value) {
            me.dom.find('input.form-control').val(v).trigger('change');
        }
    },
});

jmaa.editor('company_editor', {
    extends: "editors.many2one",
    searchRelated(callback) {
        let me = this;
        jmaa.rpc({
            model: "job.task",
            module: me.module,
            method: "searchCompany",
            args: {
                executeUser: me.owner.editors.execute_user_id.getRawValue()
            },
            context: {
                usePresent: true,
                active_test: me.activeTest,
            },
            onsuccess(r) {
                callback(r);
            }
        });
    },
});

jmaa.column('cron_column', {
    render: function () {
        return function (data, type, row) {
            if (!data) {
                data = '';
            }
            return `<span class="char-column" title="${row.next_exec_time}">${data}</span>`;
        }
    }
});
