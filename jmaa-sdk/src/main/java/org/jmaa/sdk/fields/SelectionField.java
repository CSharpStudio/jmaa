package org.jmaa.sdk.fields;

import java.util.HashMap;
import java.util.Map;

import org.jmaa.sdk.SelectionValue;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Selection;

/**
 * 选择
 *
 * @author Eric Liang
 */
public class SelectionField extends BaseField<SelectionField> {
    @Related
    int length = 240;
    @JsonIgnore
    Selection selection;
    @JsonIgnore
    Map<String, String> options;
    @JsonIgnore
    boolean isCatalog;

    public SelectionField() {
        type = Constants.SELECTION;
        columnType = ColumnType.VarChar;
    }

    @Override
    public String getDbColumnType(SqlDialect sqlDialect) {
        return sqlDialect.getColumnType(columnType, length, null);
    }

    public boolean isStatic() {
        return selection.isStatic();
    }

    /**
     * DB存储长度
     *
     * @param length
     * @return
     */
    public SelectionField length(Integer length) {
        args.put("length", length);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getOptions(Records rec) {
        if (!rec.getEnv().getRegistry().isLoaded()) {
            return selection.get(rec);
        }
        if (options == null) {
            if (selection.isStatic()) {
                options = (Map<String, String>) rec.getEnv().get("ir.model.field.selection").call("getSelection", getModelName(), getName());
            } else {
                options = selection.get(rec);
            }
        }
        return options;
    }

    public SelectionField selection(Selection selection) {
        args.put("selection", selection);
        return this;
    }

    public SelectionField selection(Map<String, String> selection) {
        args.put("selection", Selection.value(selection));
        return this;
    }

    public SelectionField selection(SelectionValue selection) {
        args.put("selection", Selection.value(selection.getMap()));
        return this;
    }

    public SelectionField addSelection(Map<String, String> toAdd) {
        args.put("selection_add", toAdd);
        return this;
    }

    /**
     * 是否使用快码维护选项
     *
     * @return
     */
    public SelectionField useCatalog() {
        args.put("isCatalog", true);
        return this;
    }

    /**
     * 是否使用快码维护选项
     *
     * @param isCatalog
     * @return
     */
    public SelectionField useCatalog(boolean isCatalog) {
        args.put("isCatalog", isCatalog);
        return this;
    }

    public boolean isCatalog() {
        return isCatalog;
    }

    @Override
    protected Map<String, Object> getAttrs(MetaModel model, String name) {
        Map<String, Object> attrs = super.getAttrs(model, name);
        attrs.remove("selection_add");
        return attrs;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void setupAttrs(MetaModel model, String name) {
        super.setupAttrs(model, name);
        Map<String, String> toAdd = new HashMap<>(16);
        for (MetaField field : resolveMro(model, name)) {
            if (field instanceof SelectionField) {
                SelectionField sf = (SelectionField) field;
                Map<String, String> newValues = (Map<String, String>) sf.args.get("selection_add");
                if (newValues != null) {
                    toAdd.putAll(newValues);
                }
            }
        }
        if (selection == null) {
            selection = Selection.value(toAdd);
        } else {
            selection.add(toAdd);
        }
    }

    @Override
    public Object convertToColumn(Object value, Records record, boolean validate) {
        if (value instanceof SelectionValue) {
            value = value.toString();
        }
        return super.convertToColumn(value, record, validate);
    }

    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (value instanceof SelectionValue) {
            value = value.toString();
        }
        return super.convertToCache(value, rec, validate);
    }

    @Override
    public Object convertToPresent(Object value, Records rec) {
        return getOptions(rec).get(value);
    }
}
