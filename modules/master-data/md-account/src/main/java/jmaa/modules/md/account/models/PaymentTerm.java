package jmaa.modules.md.account.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.payment_term", label = "结算方式")
public class PaymentTerm extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field type = Field.Selection(new Options(){{
        put("exchange", "汇兑");
        put("bill-exchange", "汇票");
        put("cash", "现金");
        put("wechat", "微信");
        put("alipay", "支付宝");
        put("bank-bill", "银行承兑汇票");
        put("check", "支票");
    }}).label("结算方式").required();
    static Field business = Field.Selection(new Options(){{
        put("bank", "银行业务");
        put("bill", "票据业务");
        put("internal", "内部结算");
    }}).label("业务分类").required();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);
}
