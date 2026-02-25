package jmaa.modules.board.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "board.template", label = "看板模板")
public class BoardTemplate extends Model {
    static Field name = Field.Char().label("标题").required();
    static Field image = Field.Image().label("缩略图");
    static Field remark = Field.Char().label("备注");
    static Field content = Field.Text().label("画布");
}
