package org.jmaa.sdk.fields;

import java.util.Map;

import org.jmaa.sdk.exceptions.TypeException;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.DbColumn;
import org.jmaa.sdk.exceptions.ValueException;

/**
 * 主键
 *
 * @author Eric Liang
 */
public class IdField extends BaseField<IdField> {
    public IdField() {
        type = Constants.CHAR;
        label = "ID";
        readonly = true;
        prefetch = false;
        columnType = ColumnType.VarChar;
    }

    @Override
    protected void updateDb(Records model, Map<String, DbColumn> columns) {
        // do nothing
    }

    @Override
    public Object get(Records record) {
        if (!record.any()) {
            return "";
        }
        Object[] ids = record.getIds();
        if (ids.length == 1) {
            return ids[0];
        }
        throw new ValueException(String.format("期望单个值%s", record));
    }

    @Override
    public void set(Records records, Object value) {
        throw new TypeException("ID字段不能赋值");
    }
}
