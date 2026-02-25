package org.jmaa.sdk.fields;

import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;

/**
 * 超文本
 *
 * @author Eric Liang
 */
public class HtmlField extends StringField<HtmlField> {
    public HtmlField() {
        type = Constants.HTML;
        columnType = ColumnType.Text;
    }
}
