package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.UniqueConstraint;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.fields.Many2oneField;
import org.jmaa.sdk.fields.Many2oneReferenceField;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.tools.StringUtils;
import org.apache.commons.collections4.SetUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 公司组织数据隔离
 *
 * @author Eric Liang
 */
@Model.Meta(name = "mixin.companies", label = "多组织的模型")
public class MixinCompanies extends AbstractModel {
    /**
     * 组织多对多字段，需要在子类重写，指定中间表名，关联字段1，关联字段2
     */
    static Field company_ids = Field.Many2many("res.company", "", "", "").label("公司")
        .lookup("searchCompany").required();


    @Override
    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        Map<String, Object> result = (Map<String, Object>) callSuper(rec, values);
        if (Utils.isEmpty(result.get("company_ids"))) {
            result.put("company_ids", Collections.singletonList(Arrays.asList(4, rec.getEnv().getCompany().getId(), 0)));
        }
        return result;
    }

    /**
     * 过滤查询，自动增加组织条件
     * 1.如果criteria参数中包含company_ids条件，要防止超出用户所有组织
     * 2.如果criteria参数中包不含company_ids条件，使用用户指定的组织
     * 3.特殊情况要禁用组织过滤，例如调度任务，使用环境上下文参数#companyLess
     */
    @Override
    public Records find(Records rec, Criteria criteria, Integer offset, Integer limit, String order) {
        boolean companyLess = Utils.toBoolean(rec.getEnv().getContext().getOrDefault("#companyLess", false), false);
        if (!companyLess) {
            if (criteria.hasField("company_ids")) {
                criteria.and(Criteria.in("company_ids", rec.getEnv().getUser().getRec("company_ids").getIds()));
            } else {
                criteria.and(Criteria.in("company_ids", rec.getEnv().getCompanies().getIds()).or(Criteria.equal("company_ids", null)));
            }
        }
        return (Records) rec.callSuper(MixinCompanies.class, "find", criteria, offset, limit, order);
    }

    /**
     * 统计数量，自动增加组织条件
     */
    @Override
    public long count(Records rec, Criteria criteria) {
        boolean companyLess = Utils.toBoolean(rec.getEnv().getContext().getOrDefault("#companyLess", false), false);
        if (!companyLess) {
            if (rec.getEnv().getRegistry().isLoaded()) {
                if (criteria.hasField("company_ids")) {
                    criteria.and(Criteria.in("company_ids", rec.getEnv().getUser().getRec("company_ids").getIds()).or(Criteria.equal("company_ids", null)));
                } else {
                    criteria.and(Criteria.in("company_ids", rec.getEnv().getCompanies().getIds()).or(Criteria.equal("company_ids", null)));
                }
            }
        }
        return (long) rec.callSuper(MixinCompanies.class, "count", criteria);
    }


    /**
     * 数据源过滤条件，用户有权限的组织
     */
    public Criteria searchCompany(Records rec) {
        boolean companyLess = Utils.toBoolean(rec.getEnv().getContext().getOrDefault("#companyLess", false), false);
        if (!companyLess) {
            return Criteria.in("id", rec.getEnv().getUser().getRec("company_ids").getIds());
        }
        return new Criteria();
    }

    /**
     * 验证唯一性，要先禁用组织，如果查到已存在数据，提示已关联的组织
     */
    @Override
    public void validateUniqueConstraint(Records record, UniqueConstraint constraint, Map<String, Object> values, boolean create) {
        Criteria criteria = new Criteria();
        Map<String, Object> uniqueValues = new HashMap<>();
        for (String field : constraint.getFields()) {
            Object value;
            if (!create && !values.containsKey(field)) {
                value = record.get(field);
                if (value instanceof Records) {
                    MetaField f = record.getMeta().getField(field);
                    if (f instanceof Many2oneField) {
                        value = ((Records) value).getId();
                    }
                }
            } else {
                value = values.get(field);
            }
            uniqueValues.put(field, value);
            if (Utils.isNotEmpty(value)) {
                criteria.and(Criteria.equal(field, value));
            }
        }
        if (criteria.size() > 0) {
            if (!create) {
                criteria.and(Criteria.binary("id", "!=", record.getId()));
            }
            Records exists = record.withContext("#companyLess", true).find(criteria);
            if (exists.any()) {
                String companies = exists.getRec("company_ids").stream().map(row -> row.getString("present")).collect(Collectors.joining("，"));
                String message = constraint.getMessage();
                List<String> errors = new ArrayList<>();
                for (String field : constraint.getFields()) {
                    MetaField mf = record.getMeta().getField(field);
                    String fieldLabel = mf.getLabel();
                    Object fieldValue = uniqueValues.get(field);
                    if (ObjectUtils.isNotEmpty(fieldValue)) {
                        if (mf instanceof Many2oneField) {
                            fieldValue = ((Records) mf.convertToRecord(fieldValue, record)).get("present");
                        } else if (mf instanceof SelectionField) {
                            SelectionField sf = (SelectionField) mf;
                            fieldValue = sf.getOptions(record).get(fieldValue);
                        } else if (mf instanceof Many2oneReferenceField) {
                            Many2oneReferenceField rf = (Many2oneReferenceField) mf;
                            String key = rf.getModelField();
                            String model = (String) values.get(key);
                            if (Utils.isNotEmpty(model)) {
                                fieldValue = record.getEnv().get(model, (String) fieldValue).get("present");
                            } else if (!create) {
                                model = record.getString(key);
                                fieldValue = record.getEnv().get(model, (String) fieldValue).get("present");
                            }
                        }
                    }
                    errors.add(record.l10n(fieldLabel) + String.format("[%s]", fieldValue));
                }
                if (StringUtils.isNotEmpty(message)) {
                    String msg = record.l10n("组织[%s] %s：", companies, StringUtils.join(errors)) + record.l10n(message);
                    throw new ValidationException(msg);
                }
                throw new ValidationException(record.l10n("%s 已存在于组织[%s]，不能重复", StringUtils.join(errors), companies));
            }
        }
    }

    @Override
    public Map<String, Integer> createOrUpdate(Records record, List<Map<String, Object>> values) {
        Set<String> userCompanyIds = SetUtils.hashSet(record.getEnv().getUser().getRec("company_ids").getIds());
        for (Map<String, Object> value : values) {
            Object val = value.get("company_ids");
            if (val instanceof List) {
                List list = (List) val;
                for (Object row : list) {
                    if (row instanceof List) {
                        List cmd = (List) row;
                        if (cmd.size() == 3 && cmd.get(0).equals(6)) {
                            List<String> ids = (List<String>) cmd.get(2);
                            for (String cid : ids) {
                                if (StringUtils.isNotEmpty(cid) && !userCompanyIds.contains(cid)) {
                                    throw new ValidationException(record.l10n("当前用户没有公司[%s]的权限", record.getEnv().get("res.company", cid).get("present")));
                                }
                            }
                        }
                    }
                }
            }
        }
        return (Map<String, Integer>) callSuper(record, values);
    }
}
