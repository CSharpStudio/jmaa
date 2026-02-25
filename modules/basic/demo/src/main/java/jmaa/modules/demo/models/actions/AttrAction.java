package jmaa.modules.demo.models.actions;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "demo.attr_action", label = "属性动作")
public class AttrAction extends Model {
    static Field value = Field.Float().label("数值").defaultValue(5).min(0d).max(5d);
    static Field decimals = Field.Integer().label("小数位").defaultValue(2);
    static Field min = Field.Float().label("最小值").defaultValue(0);
    static Field max = Field.Float().label("最大值").defaultValue(5);
    static Field readonly = Field.Boolean().label("只读");
    static Field visible = Field.Boolean().label("显示").defaultValue(true);
    static Field required = Field.Boolean().label("必填");

    /**
     * 控制值的小数位数
     *
     * @param record
     * @return
     */
    @ActionMethod
    public Action onDecimalsChange(Records record) {
        return Action.attr().setAttr("value", "data-decimals", record.getInteger(decimals));
    }

    /**
     * 控件值的最小值
     *
     * @param record
     * @return
     */
    @ActionMethod
    public Action onMinChange(Records record) {
        return Action.attr().setAttr("value", "min", record.getDouble(min));
    }


    /**
     * 控件值的最大值
     *
     * @param record
     * @return
     */
    @ActionMethod
    public Action onMaxChange(Records record) {
        return Action.attr().setAttr("value", "max", record.getDouble(max));
    }

    /**
     * 控件值的只读
     *
     * @param record
     * @return
     */
    @ActionMethod
    public Action onReadonlyChange(Records record) {
        return Action.attr().setReadonly("value", record.getBoolean(readonly));
    }

    /**
     * 控件值的显示
     *
     * @param record
     * @return
     */
    @ActionMethod
    public Action onVisibleChange(Records record) {
        return Action.attr().setVisible("value", record.getBoolean(visible));
    }

    /**
     * 控件值的必填
     *
     * @param record
     * @return
     */
    @ActionMethod
    public Action onRequiredChange(Records record) {
        return Action.attr().setRequired("value", record.getBoolean(required));
    }
}
