package jmaa.modules.studio.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.util.KvMap;
import org.apache.commons.collections4.SetUtils;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(name = "dev.studio", label = "开发平台")
@Model.Service(remove = "@all")
public class Studio extends ValueModel {
    @Model.ServiceMethod(label = "设计", ids = false, doc = "读取模型设计数据")
    public Object design(Records record, String model) {
        Environment env = record.getEnv();
        Records irModel = env.get("ir.model").find(Criteria.equal("model", model));
        if (!irModel.any()) {
            throw new ValidationException(record.l10n("模型[%s]不存在", model));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", irModel.getId());
        Records studio = env.get("ir.module").find(Criteria.equal("name", "studio"));
        Cursor cr = env.getCursor();
        cr.execute("select distinct type from ir_ui_view where mode='primary' and model=%s and active=%s and "
            + cr.getSqlDialect().quote("key") + " is null", Utils.asList(model, true));
        List<String> types = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
        result.put("views", types);
        result.put("studio", studio.getPresent());
        return result;
    }

    @Model.ServiceMethod(auth = "design", ids = false, label = "加载模型元数据")
    public Object readModel(Records record, String modelId, List<String> fields) {
        return record.getEnv().get("ir.model", modelId).read(fields);
    }

    @Model.ServiceMethod(auth = "design", ids = false, label = "加载模型示例数据")
    public Object searchModelDemo(Records record, String model, List<String> fields, Integer limit) {
        int size = Math.min(Utils.toInt(limit), 5);
        return record.getEnv().get(model).search(fields, new Criteria(), 0, size, null);
    }

    @ServiceMethod(auth = "design", label = "查询视图", ids = false)
    public Object searchView(Records record, Criteria criteria, Collection<String> fields, Integer offset, Integer limit, String order) {
        return record.getEnv().get("ir.ui.view").searchLimit(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(auth = "design", label = "编辑视图", ids = false)
    public Object updateView(Records records, String viewId, Map<String, Object> values) {
        records.getEnv().get("ir.ui.view", viewId).update(values);
        return Action.success();
    }

    @ServiceMethod(auth = "design", label = "创建视图", ids = false)
    public Object createView(Records records, Map<String, Object> values) {
        Records view = records.getEnv().get("ir.ui.view").create(values);
        return view.getId();
    }

    @ServiceMethod(auth = "design", label = "删除视图", ids = false)
    public Object deleteView(Records records, String viewId) {
        records.getEnv().get("ir.ui.view", viewId).delete();
        return Action.success();
    }

    @ServiceMethod(auth = "design", label = "激活视图", ids = false)
    public Object activeView(Records records, String model, String type) {
        Records studio = records.getEnv().get("ir.module").find(Criteria.equal("name", "studio"));
        Records view = records.getEnv().get("ir.ui.view").find(Criteria.equal("model", model)
            .and("type", "=", type).and("key", "=", null).and("mode", "=", "primary"));
        if (view.any()) {
            view.set("active", true);
        } else {
            String toolbar = "form".equals(type) || "grid".equals(type) || "card".equals(type) ?
                "<toolbar buttons='default'></toolbar>" : "";
            String arch = String.format("<%s>%s</%s>", type, toolbar, type);
            view.create(new KvMap()
                .set("model", model)
                .set("type", type)
                .set("mode", "primary")
                .set("module_id", studio.getId())
                .set("name", String.format("%s-%s-studio", model, type))
                .set("arch", arch));
        }
        return view.getId();
    }

    @ServiceMethod(auth = "design", label = "禁用视图", ids = false)
    public Object inactiveView(Records records, String model, String type) {
        Records view = records.getEnv().get("ir.ui.view").find(Criteria.equal("model", model)
            .and("type", "=", type).and("key", "=", null).and("mode", "=", "primary"));
        view.set("active", false);
        return Action.success();
    }

    @ServiceMethod(auth = "design", label = "恢复视图", ids = false)
    public Object restoreView(Records records, String model, String type) {
        Records studio = records.getEnv().get("ir.module").find(Criteria.equal("name", "studio"));
        Records view = records.getEnv().get("ir.ui.view").find(Criteria.equal("model", model)
            .and("type", "=", type).and("module_id", "=", studio.getId()));
        view.set("active", false);
        return Action.success();
    }

    @ServiceMethod(auth = "design", label = "查询字段", ids = false)
    public Object searchField(Records record, Criteria criteria, Collection<String> fields, Integer offset, Integer limit, String order) {
        return record.getEnv().get("ir.model.field").searchLimit(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(auth = "design", label = "删除字段", ids = false)
    public Object deleteField(Records record, List<String> fieldIds) {
        Records fields = record.getEnv().get("ir.model.field", fieldIds);
        for (Records field : fields) {
            String model = field.getRec("model_id").getString("model");
            MetaModel meta = record.getEnv().getRegistry().get(model);
            meta.getFields().remove(field.getString("name"));
        }
        fields.delete();
        return Action.success();
    }

    @Model.ServiceMethod(auth = "design", ids = false, label = "加载模型字段元数据")
    public Object updateModel(Records record, String modelId, Map<String, Object> values) {
        record.getEnv().get("ir.model", modelId).update(values);
        return Action.success();
    }

    @Model.ServiceMethod(auth = "design", ids = false, label = "加载模型字段元数据")
    public Object loadModelFields(Records record, @Doc(doc = "模型名称") String model) {
        KvMap result = new KvMap();
        result.put("fields", getFields(record.getEnv(), model));
        result.put("present", record.getEnv().get(model).getMeta().getPresent());
        return result;
    }

    Map<String, Map<String, Object>> getFields(Environment env, String model) {
        Records rec = env.get(model);
        ObjectMapper m = new ObjectMapper();
        Set<Map.Entry<String, MetaField>> fields = rec.getMeta().getFields().entrySet();
        Map<String, Map<String, Object>> result = new HashMap<>(fields.size());
        for (Map.Entry<String, MetaField> e : fields) {
            try {
                MetaField field = e.getValue();
                Map<String, Object> data = (Map<String, Object>) m.treeToValue(m.valueToTree(field), Map.class);
                data.put("defaultValue", field.getDefault(rec));
                if (field instanceof SelectionField) {
                    MetaField related = field.getRelatedField();
                    if (related != null) {
                        data.put("options", ((SelectionField) related).getOptions(rec));
                    } else {
                        data.put("options", ((SelectionField) field).getOptions(rec));
                    }
                }
                result.put(field.getName(), data);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Records fieldSet = env.get("ir.model.field").find(Criteria.equal("origin", "manual")
            .and("model_id.model", "=", model));
        for (Records row : fieldSet) {
            String name = row.getString("name");
            if (!result.containsKey(name)) {
                Map<String, Object> meta = row.read(Utils.asList("name", "field_type", "relation", "label", "help")).get(0);
                String type = (String) meta.remove("field_type");
                meta.put("type", type);
                result.put(name, meta);
            }
        }
        return result;
    }

    @Model.ServiceMethod(auth = "design", ids = false, label = "创建模型")
    public Object createModel(Records record, String name, String model, List<String> features) {
        Environment env = record.getEnv();
        Records studio = env.get("ir.module").find(Criteria.equal("name", "studio"));
        Records irModel = env.get("ir.model").create(new KvMap().set("model", model)
            .set("name", name)
            .set("module_id", studio.getId()));
        irModel.call("addFeatures", features);
        Cursor cr = env.getCursor();
        cr.execute("SELECT * FROM ir_model WHERE id=%s", Arrays.asList(irModel.getId()));
        Map<String, Object> data = cr.fetchMapAll().get(0);
        ModelBuilder builder = Loader.get(ModelBuilder.class);
        irModel.call("addManualModel", builder, data);
        cr.execute("SELECT * FROM ir_model_field WHERE model_id=%s", Utils.asList(irModel.getId()));
        List<Map<String, Object>> list = cr.fetchMapAll();
        MetaModel meta = env.getRegistry().get(model);
        MetaModelWrapper wrapper = new MetaModelWrapper(meta);
        wrapper.addMagicFields();
        Records irField = env.get("ir.model.field");
        for (Map<String, Object> row : list) {
            irField.call("addManualField", wrapper, row);
        }
        wrapper.autoInit(env);
        initViews(record, irModel);
        env.get("rbac.permission").call("refreshByModels", Utils.asList(model));
        return record.getId();
    }

    @ServiceMethod(auth = "design", label = "模型添加字段", doc = "给指定的模型添加新字段", ids = false)
    public Object addNewField(Records record, String model, Map<String, Object> field, Map<String, Object> uiView) {
        Environment env = record.getEnv();
        Records irModel = env.get("ir.model").find(Criteria.equal("model", model));
        field.put("model_id", irModel.getId());
        String type = (String) field.get("field_type");
        if ("many2many".equals(type)) {
            field.put("column1", "x_id");
            field.put("column2", "y_id");
            String relation = (String) field.get("relation");
            field.put("relation_table", "rel_" + model.replaceAll("\\.", "_") + "_" + relation.replaceAll("\\.", "_"));
        }
        Records irField = env.get("ir.model.field");
        irField = irField.create(field);
        String viewId = (String) uiView.remove("id");
        if (Utils.isEmpty(viewId)) {
            env.get("ir.ui.view").create(uiView);
        } else {
            env.get("ir.ui.view", viewId).update(uiView);
        }
        MetaModel meta = env.getRegistry().get(model);
        MetaModelWrapper wrapper = new MetaModelWrapper(meta);
        Cursor cr = env.getCursor();
        cr.execute("SELECT * FROM ir_model_field WHERE id=%s", Utils.asList(irField.getId()));
        List<Map<String, Object>> list = cr.fetchMapAll();
        irField.call("addManualField", wrapper, list.get(0));
        wrapper.autoInit(env);
        return Action.success();
    }

    public void initViews(Records record, Records irModel) {
        Records view = record.getEnv().get("ir.ui.view");
        for (Records r : irModel) {
            String name = r.getString("name");
            String model = r.getString("model");
            String moduleId = r.getRec("module_id").getId();
            Criteria criteria = Criteria.equal("model", model).and(Criteria.equal("mode", "primary"));
            Records views = view.find(criteria);
            List<Map<String, Object>> toCreate = new ArrayList<>();
            toCreate.add(new KvMap()
                .set("name", name + "-表格")
                .set("model", model)
                .set("type", "grid")
                .set("mode", "primary")
                .set("arch", getGridArch(r))
                .set("module_id", moduleId));
            toCreate.add(new KvMap()
                .set("name", name + "-表单")
                .set("model", model)
                .set("type", "form")
                .set("mode", "primary")
                .set("arch", getFormArch(r))
                .set("module_id", moduleId));
            toCreate.add(new KvMap()
                .set("name", name + "-查询")
                .set("model", model)
                .set("type", "search")
                .set("mode", "primary")
                .set("arch", getSearchArch(r))
                .set("module_id", moduleId));
            views.createBatch(toCreate);
        }
    }

    static Set<String> autoFields = SetUtils.hashSet("id", "present", "create_uid", "create_date", "update_uid", "update_date");

    String getSearchArch(Records model) {
        String arch = "<search>\n";
        Records fields = model.getRec("field_ids");
        for (Records field : fields) {
            String name = field.getString("name");
            if (autoFields.contains(name)) {
                continue;
            }
            String fieldType = (String) field.get("field_type");
            if (!fieldType.endsWith("many")) {
                arch += "<field name='" + name + "'></field>\n";
            }
        }
        arch += "</search>";
        return arch;
    }

    String getGridArch(Records model) {
        String arch = "<grid>\n<toolbar buttons='default'></toolbar>\n";
        Records fields = model.getRec("field_ids");
        for (Records field : fields) {
            String name = field.getString("name");
            if (autoFields.contains(name)) {
                continue;
            }
            String fieldType = (String) field.get("field_type");
            if (!fieldType.endsWith("many")) {
                arch += "<field name='" + name + "'></field>\n";
            }
        }
        arch += "</grid>";
        return arch;
    }

    String getFormArch(Records model) {
        String arch = "<form>\n<toolbar buttons='default'></toolbar>\n";
        Records fields = model.getRec("field_ids");
        for (Records field : fields) {
            String name = field.getString("name");
            if (autoFields.contains(name)) {
                continue;
            }
            String fieldType = (String) field.get("field_type");
            if (!fieldType.endsWith("many")) {
                arch += "<field name='" + name + "'></field>\n";
            } else {
                arch += "<field name='" + name + "' editor='many2many-tags'></field>\n";
            }
        }
        arch += "</form>";
        return arch;
    }
}
