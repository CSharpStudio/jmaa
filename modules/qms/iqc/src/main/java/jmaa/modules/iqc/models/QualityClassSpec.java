package jmaa.modules.iqc.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "qsd.quality_class_spec")
public class QualityClassSpec extends Model {
    static Field type = Field.Selection().addSelection(new Options() {{
        put("iqc.sheet", "来料检验");
    }});
}
