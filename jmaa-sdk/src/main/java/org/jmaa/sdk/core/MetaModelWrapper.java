package org.jmaa.sdk.core;

import org.jmaa.sdk.exceptions.ModelException;
import org.jmaa.sdk.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Eric Liang
 */
public class MetaModelWrapper {
    private Logger logger = LoggerFactory.getLogger(MetaModelWrapper.class);
    MetaModel metaModel;

    public MetaModelWrapper(MetaModel meta) {
        metaModel = meta;
    }

    public MetaModel getModel() {
        return metaModel;
    }

    public MetaModelWrapper setAuto(boolean auto) {
        metaModel.auto = auto;
        return this;
    }

    public MetaModelWrapper setAbstract(boolean isAbstract) {
        metaModel.isAbstract = isAbstract;
        return this;
    }

    public MetaModelWrapper setTransient(boolean isTransient) {
        metaModel.isTransient = isTransient;
        return this;
    }

    public MetaModelWrapper setName(String name) {
        metaModel.name = name;
        return this;
    }

    public MetaModelWrapper setLabel(String label) {
        metaModel.label = label;
        return this;
    }

    public MetaModelWrapper setDescription(String description) {
        metaModel.description = description;
        return this;
    }

    public MetaModelWrapper setAuthModel(String authModel) {
        metaModel.authModel = authModel;
        return this;
    }

    public MetaModelWrapper setTable(String table) {
        metaModel.table = table;
        return this;
    }

    public MetaModelWrapper setPresent(String[] present) {
        metaModel.present = present;
        return this;
    }

    public MetaModelWrapper setPresentFormat(String presentFormat) {
        metaModel.presentFormat = presentFormat;
        return this;
    }

    public MetaModelWrapper setOrder(String order) {
        metaModel.order = order;
        return this;
    }

    public MetaModelWrapper setLogAccess(boolean logAccess) {
        metaModel.logAccess = logAccess;
        return this;
    }

    public MetaModelWrapper setModule(String module) {
        metaModel.module = module;
        return this;
    }

    public MetaModelWrapper setCustom(boolean custom) {
        metaModel.custom = custom;
        return this;
    }

    public MetaModelWrapper putField(String name, MetaField field) {
        metaModel.fields.put(name, field);
        return this;
    }

    public MetaModelWrapper addField(String name, MetaField field) {
        metaModel.addField(name, field);
        return this;
    }

    public MetaModelWrapper addMagicFields() {
        metaModel.addMagicFields();
        return this;
    }

    /**
     * 重新计算MRO
     *
     * @return
     */
    public MetaModelWrapper resetMro() {
        metaModel.mro = Mro.calculate(metaModel, metaModel.bases);
        return this;
    }

    /**
     * 设置基模型
     *
     * @param bases
     */
    public MetaModelWrapper setBases(List<MetaModel> bases) {
        metaModel.bases = bases;
        metaModel.mro = Mro.calculate(metaModel, bases);
        return this;
    }

    public MetaModelWrapper setAuthFields(List<String> fields) {
        metaModel.authFields = fields;
        return this;
    }

    public MetaModelWrapper setRegistry(Registry registry) {
        metaModel.registry = registry;
        return this;
    }

    public MetaModelWrapper init() {
        metaModel.methods = new HashMap<>(16);
        metaModel.services = new HashMap<>(16);
        metaModel.uniques = new HashMap<>(16);
        metaModel.constrains = new HashMap<>(16);
        metaModel.onChanges = new HashMap<>(16);
        metaModel.onDelete = new ArrayList<>();
        metaModel.actions = new HashSet<>();
        return this;
    }

    public MetaModelWrapper addOnDelete(String method, boolean atUninstall) {
        metaModel.onDelete.add(new Tuple<>(method, atUninstall));
        return this;
    }

    public MetaModelWrapper addOnChange(String field, String method) {
        Set<String> methods = metaModel.onChanges.get(field);
        if (methods == null) {
            methods = new LinkedHashSet<>();
            metaModel.onChanges.put(field, methods);
        }
        methods.add(method);
        return this;
    }

    public MetaModelWrapper addConstrains(String field, String method) {
        Set<String> methods = metaModel.constrains.get(field);
        if (methods == null) {
            methods = new LinkedHashSet<>();
            metaModel.constrains.put(field, methods);
        }
        methods.add(method);
        return this;
    }

    public MetaModelWrapper addUnique(String name, UniqueConstraint unique) {
        metaModel.uniques.put(name, unique);
        return this;
    }

    public MetaModelWrapper addMethod(String methodName, MetaMethod method) {
        List<MetaMethod> methods = metaModel.methods.get(methodName);
        if (methods == null) {
            methods = new ArrayList<>();
            metaModel.methods.put(methodName, methods);
        }
        if (methods.size() > 0) {
            checkMethodArgs(methods.get(0), method);
        }
        methods.add(0, method);
        return this;
    }

    public MetaModelWrapper addFieldInverse(String field, String inverseName) {
        List<String> list = metaModel.fieldInverses.computeIfAbsent(field, k -> new ArrayList<String>());
        list.add(inverseName);
        return this;
    }

    void checkMethodArgs(MetaMethod m1, MetaMethod m2) {
        if (!m1.isParameterMatch(m2.getMethod().getParameterTypes())) {
            throw new ModelException(String.format("模型不支持方法重载：类%s的方法%s参数与模型%s的方法参数不一致",
                m2.getDeclaringClass().getName(), m2.getMethod().getName(), metaModel.getName()));
        }
    }

    public MetaModelWrapper setArgs(Map<String, Object> args) {
        metaModel.args = args;
        return this;
    }

    public void autoInit(Environment env) {
        metaModel.autoInit(env);
    }
}
