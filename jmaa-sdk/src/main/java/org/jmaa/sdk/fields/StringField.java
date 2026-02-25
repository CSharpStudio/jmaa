package org.jmaa.sdk.fields;

import java.util.Map;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.MetaModel;

/**
 * 字符串
 *
 * @author Eric Liang
 */
public class StringField<T extends BaseField<T>> extends BaseField<T> {
    @Related
    Boolean translate;

    public boolean isTranslate() {
        if (translate == null) {
            return false;
        }
        return translate;
    }

    @SuppressWarnings("unchecked")
    public T translate() {
        args.put("translate", true);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T translate(boolean translate) {
        args.put("translate", translate);
        return (T) this;
    }

    @Override
    protected Map<String, Object> getAttrs(MetaModel model, String name) {
        Map<String, Object> attrs = super.getAttrs(model, name);
        Boolean tran = (Boolean) attrs.getOrDefault("translate", false);
        // do not prefetch complex translated fields by default
        if (tran) {
            attrs.put("prefetch", attrs.getOrDefault("prefetch", false));
        }
        return attrs;
    }

    @Override
    public Records write(Records records, Object value) {
        // TODO
        return super.write(records, value);
    }

    @Override
    public Object convertToRead(Object value, Records rec, boolean usePresent) {
        return convertToRecord(value, rec);
    }

    @Override
    public Object convertToRecord(Object value, Records rec) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
