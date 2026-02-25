function formatDate(dateTimeStamp) {
    if (dateTimeStamp == undefined) {
        return '';
    } else {
        let minute = 1000 * 60;
        let hour = minute * 60;
        let day = hour * 24;
        let halfamonth = day * 15;
        let month = day * 30;
        dateTimeStamp = dateTimeStamp.replace(/\-/g, "/");
        let sTime = new Date(dateTimeStamp).getTime();//把时间pretime的值转为时间戳
        let now = new Date().getTime();//获取当前时间的时间戳
        let diffValue = now - sTime;
        if (diffValue < 0) {
            return dateTimeStamp;
        }
        let monthC = diffValue / month;
        let weekC = diffValue / (7 * day);
        let dayC = diffValue / day;
        let hourC = diffValue / hour;
        let minC = diffValue / minute;

        if (monthC >= 1) {
            return parseInt(monthC) + "个月前".t();
        } else if (weekC >= 1) {
            return parseInt(weekC) + "周前".t();
        } else if (dayC >= 1) {
            return parseInt(dayC) + "天前".t();
        } else if (hourC >= 1) {
            return parseInt(hourC) + "小时前".t();
        } else if (minC >= 1) {
            return parseInt(minC) + "分钟前".t();
        } else {
            return "刚刚".t();
        }
    }
}
