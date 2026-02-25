package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

@Model.Meta(name = "mixin.ac_re", label = "接收数/拒收数")
public class AcReMixin extends AbstractModel {
    static Field ac = Field.Integer().label("Ac").min(0);
    static Field re = Field.Integer().label("Re").compute("computeRe");
    public Integer computeRe(Records record) {
        Integer ac = record.getNullableInteger("ac");
        if (ac == null) {
            return null;
        }
        return ac + 1;
    }
    @Model.ActionMethod
    public Action acChangeAction(Records record) {
        AttrAction action = new AttrAction();
        Integer ac = record.getNullableInteger("ac");
        action.setValue("re", ac == null ? 0 : ac + 1);
        return action;
    }
}
