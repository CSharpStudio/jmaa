package org.jmaa.sdk.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmaa.sdk.tools.StringUtils;

/**
 * @author Eric Liang
 */
public class CustomModel extends BaseModel {
    String name;
    String inherit;
    String label;
    String description;
    String order;
    String present;
    String table;

    public CustomModel(String name, String inherit, String label, String description, String order,
            String present, String table, Boolean isTransient) {
        this.name = name;
        this.inherit = inherit;
        this.label = label;
        this.description = description;
        this.order = order;
        this.present = present;
        this.table = table;
        this.isTransient = isTransient != null && isTransient;
        isAbstract = false;
        isAuto = true;
        custom = true;
    }

    private CustomModelRefactor modelRefactor;

    @Override
    public ModelRefactor getRefactor() {
        if (modelRefactor == null) {
            modelRefactor = new CustomModelRefactor();
        }
        return modelRefactor;
    }

    public class CustomModelRefactor extends ModelRefactor {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<String> getInherit() {
            if (StringUtils.isNotBlank(inherit)) {
                return new ArrayList<>(Arrays.asList(inherit.split(",")));
            }
            return new ArrayList<>();
        }

        @Override
        public List<String> getInheritIf() {
            return new ArrayList<>();
        }
        @Override
        public Map<String, Object> getArgs() {
            Map<String, Object> map = new HashMap<>(16);
            if (StringUtils.isNotBlank(label)) {
                map.put(Constants.LABEL, label);
            }
            if (StringUtils.isNotBlank(description)) {
                map.put(Constants.DESCRIPTION, description);
            }
            if (StringUtils.isNotBlank(order)) {
                map.put(Constants.ORDER, order);
            }
            if (StringUtils.isNotBlank(present)) {
                map.put(Constants.PRESENT, present.split(","));
            }
            if (StringUtils.isNotBlank(table)) {
                map.put(Constants.TABLE, table);
            }
            return map;
        }
    }
}
