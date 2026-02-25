package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(inherit = "lbl.code_parse")
public class CodeParse extends ValueModel {
    public void matchGenerate(Records records, Map<String, Object> valueMap) {
        Records material = records.getEnv().get("md.material");
        Records supplier = records.getEnv().get("md.supplier");
        if (Utils.isNotEmpty(valueMap.get("materialCode"))) {
            material = material.find(Criteria.equal("code", Utils.toString(valueMap.get("materialCode"))));
        } else {
            String supplierMaterialCode = Utils.toString(valueMap.get("supplierMaterial"));
            if (Utils.isBlank(supplierMaterialCode)) {
                return;
            }
            Records supplierMaterial = records.getEnv().get("wms.supplier_material").find(Criteria.equal("supplier_material_code", supplierMaterialCode));
            if (!supplierMaterial.any()) {
                throw new ValidationException(records.l10n("未匹配到供应商物料[%s]，请检查相关配置", supplierMaterialCode));
            }
            supplierMaterial= supplierMaterial.first();
            material = supplierMaterial.getRec("material_id");
            supplier = supplierMaterial.getRec("supplier_id");
            valueMap.put("materialCode", material.getString("code"));
        }
        String sn = Utils.toString(valueMap.get("sn"));
        String lotNumCode = Utils.toString(valueMap.get("lotNum"));
        if ("sn".equals(material.get("stock_rule"))) {
            if (Utils.isBlank(sn)) {
                throw new ValidationException(records.l10n("条码中未识别到序列号,请检查编码配置"));
            }
            if (Utils.isBlank(lotNumCode)) {
                throw new ValidationException(records.l10n("条码中未识别到批次号,请检查编码配置"));
            }
            Records lotNum = records.getEnv().get("lbl.lot_num").find(Criteria.equal("code", lotNumCode).and(Criteria.equal("material_id",material.getId())));
            Records materialLabel = records.getEnv().get("lbl.material_label").find(Criteria.equal("sn", sn));
            if (materialLabel.any()) {
                if (!Utils.equals(materialLabel.getRec("material_id").getId(), material.getId())) {
                    throw new ValidationException(records.l10n("条码解析过程存在冲突,序列号[%s]已存在,但物料不匹配,无法生成标签记录,请打印新标签", sn));
                }
                if (!lotNumCode.equals(materialLabel.getString("lot_num"))) {
                    throw new ValidationException(records.l10n("条码解析过程存在冲突,序列号[%s]已存在,但批次号不匹配,无法生成标签记录,请打印新标签", sn));
                }
                if (lotNum.any()) {
                    if (!Utils.equals(lotNum.getRec("material_id").getId(), material.getId())) {
                        throw new ValidationException(records.l10n("条码解析过程存在冲突,批次号[%s]已存在,但物料不匹配,无法生成标签记录,请打印新标签", lotNumCode));
                    }
                } else {
                    // 标签有了,批次没有
                    throw new ValidationException(records.l10n("条码解析过程存在冲突,序列号[%s]已存在,但批次号不匹配,无法生成标签记录,请打印新标签", sn));
                }
            } else {
                Map<String, Object> detail = new HashMap<>();
                detail.put("sn", sn);
                detail.put("lot_num", lotNumCode);
                detail.put("material_id", material.getId());
                detail.put("qty", valueMap.get("qty"));
                detail.put("original_qty", valueMap.get("qty"));
                materialLabel.create(detail);
                if (!lotNum.any()) {
                    Map<String, Object> lotMap = new HashMap<>();
                    lotMap.put("code", lotNumCode);
                    lotMap.put("material_id", material.getId());
                    lotMap.put("supplier_id", supplier.getId());
                    records.getEnv().get("lbl.lot_num").create(lotMap);
                }
            }
        } else if ("lot".equals(material.get("stock_rule"))) {
            if (Utils.isBlank(lotNumCode)) {
                throw new ValidationException(records.l10n("条码中未识别到批次号,请检查编码配置"));
            }
            Records lotNum = records.getEnv().get("lbl.lot_num").find(Criteria.equal("code", lotNumCode).and(Criteria.equal("material_id",material.getId())));
            if (lotNum.any()) {
                if (!Utils.equals(lotNum.getRec("material_id").getId(), material.getId())) {
                    throw new ValidationException(records.l10n("条码解析过程存在冲突,批次号[%s]已存在,但物料不匹配,无法生成标签记录,请打印新标签", lotNumCode));
                }
            } else {
                Map<String, Object> detail = new HashMap<>();
                detail.put("code", lotNumCode);
                detail.put("material_id", material.getId());
                detail.put("supplier_id", supplier.getId());
                lotNum.create(detail);
            }
        }
    }
}
