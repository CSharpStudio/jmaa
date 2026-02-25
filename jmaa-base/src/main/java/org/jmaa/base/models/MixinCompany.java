package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.apache.commons.collections4.SetUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * 公司组织数据隔离
 *
 * @author Eric Liang
 */
@Model.Meta(name = "mixin.company", label = "区分公司的模型")
public class MixinCompany extends AbstractModel {
    static Field company_id = Field.Many2one("res.company").label("公司").required(true)
            .ondelete(DeleteMode.Restrict).defaultValue(Default.method("companyDefault"))
            .lookup("searchCompany");

    public String companyDefault(Records rec) {
        try {
            return rec.getEnv().getCompany().getId();
        } catch (Exception e) {
            return null;
        }
    }

    public Criteria searchCompany(Records rec) {
        return Criteria.in("id", rec.getEnv().getUser().getRec("company_ids").getIds());
    }

    @Override
    public Records find(Records rec, Criteria criteria, Integer offset, Integer limit, String order) {
        addCompanyCriteria(rec, criteria);
        return (Records) rec.callSuper(MixinCompany.class, "find", criteria, offset, limit, order);
    }

    void addCompanyCriteria(Records rec, Criteria criteria) {
        boolean companyLess = Utils.toBoolean(rec.getEnv().getContext().get("#companyLess"));
        if (!companyLess && rec.getEnv().getRegistry().isLoaded()) {
            if (criteria.hasField("company_id")) {
                //当有公司条件时，限制用户有权限的公司
                criteria.and(Criteria.in("company_id", Arrays.asList(rec.getEnv().getUser().getRec("company_ids").getIds())));
            } else {
                //没指定公司时，限制在当前环境中选择的公司
                criteria.and(Criteria.in("company_id", Arrays.asList(rec.getEnv().getCompanies().getIds())));
            }
        }
    }

    @Override
    public long count(Records rec, Criteria criteria) {
        addCompanyCriteria(rec, criteria);
        return (long) rec.callSuper(MixinCompany.class, "count", criteria);
    }

    @Model.Constrains("company_id")
    public void companyConstrains(Records records) {
        boolean companyCheck = Utils.toBoolean(records.getEnv().getContext().get("#companyCheck"), true);
        if (companyCheck) {
            Set<String> userCompanyIds = SetUtils.hashSet(records.getEnv().getUser().getRec("company_ids").getIds());
            for (Records record : records) {
                Records company = record.getRec("company_id");
                if (company.any() && !userCompanyIds.contains(company.getId())) {
                    throw new ValidationException(record.l10n("当前用户没有组织[%s]的权限", company.get("present")));
                }
            }
        }
    }
}
