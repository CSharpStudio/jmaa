package jmaa.modules.iqc.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = {"res.config"})
public class ResConfig extends ValueModel {
    static Field iqc_auto_approve = Field.Boolean().label("来料检验自动审核").defaultValue(true);
}
