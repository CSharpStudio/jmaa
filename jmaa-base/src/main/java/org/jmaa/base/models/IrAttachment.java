package org.jmaa.base.models;

import java.util.List;
import java.util.Map;

import org.jmaa.sdk.*;
import org.jmaa.base.utils.FileServiceFactory;
import org.jmaa.base.utils.UploadFile;
import org.jmaa.base.utils.UploadConfig;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.fields.BinaryBaseField;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

/**
 * 附件
 *
 * @author Eric Liang
 */
@Model.Meta(name = "ir.attachment", label = "附件", order = "id")
public class IrAttachment extends Model {
    static Field name = Field.Char().label("名称").index(true).required(true);
    static Field res_name = Field.Char().label("资源名称")
            .compute(Callable.method("computeResName"));
    static Field res_model = Field.Char().label("资源模型").readonly(true);
    static Field res_field = Field.Char().label("资源字段").readonly(true);
    static Field res_id = Field.Char().label("资源ID").readonly(true);
    static Field company_id = Field.Many2one("res.company").label("公司").defaultValue(Default.method("companyDefault"));
    static Field is_public = Field.Boolean().label("公共文档");
    static Field access_token = Field.Char().label("访问TOKEN");
    static Field raw = Field.Binary().label("文件内容(raw)").compute("computeRaw");
    static Field file_data = Field.Char().label("文件内容(base64)").compute("computeFileData");
    static Field file_name = Field.Char().label("文件名");
    static Field file_path = Field.Char().label("文件路径");
    static Field file_size = Field.Integer().label("文件大小").readonly(true);
    static Field checksum = Field.Char().label("摘要").length(40).readonly(true);
    static Field file_type = Field.Char().label("文件类型").readonly(true);
    static Field mimetype = Field.Char().label("MIME类型").readonly(true);
    static Field storage = Field.Char().label("存储方式").readonly(true);

    public String computeResName(Records rec) {
        String resName = "";
        String resModel = rec.getString("res_model");
        String resId = rec.getString("res_field");
        if (StringUtils.isNotEmpty(resModel) && StringUtils.isNotEmpty(resId)) {
            Records record = rec.getEnv().get(resModel).browse(resId);
            resName = (String) record.get("present");
        }
        return resName;
    }

    /**
     * 获取默认公司
     *
     * @return
     */
    public String companyDefault(Records rec) {
        return rec.getEnv().getCompany().getId();
    }

    public Object computeFileData(Records rec) {
        byte[] data = getFileData(rec);
        return java.util.Base64.getEncoder().encode(data);
    }

    public byte[] getFileData(Records rec) {
        UploadFile entity = new UploadFile(rec.getString("file_path"));
        String type = rec.getString("storage");
        return FileServiceFactory.build(type).getFileData(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        UploadConfig uploadConfig = SpringUtils.getBean(UploadConfig.class);
        // 多个附件
        for (Map<String, Object> values : valuesList) {
            // base64数据
            byte[] fileBytes = getFileBytes(values.remove("file_data"));
            if (fileBytes == null) {
                throw new ValidationException(rec.l10n("文件内容不能为空"));
            }
            values.put("file_size", fileBytes.length);
            values.put("checksum", IoUtils.computeChecksum(fileBytes));
            values.put("storage", uploadConfig.getConfig());
            values.put("file_path", fileWrite(rec, uploadConfig, (String) values.get("file_name"), (String) values.get("res_model"), fileBytes));
        }
        return (Records) rec.callSuper(IrAttachment.class, "createBatch", valuesList);
    }

    static byte[] getFileBytes(Object fileData) {
        byte[] fileBytes = null;
        if (fileData instanceof byte[]) {
            fileBytes = (byte[]) fileData;
        } else if (fileData instanceof String) {
            String base64 = (String) fileData;
            int idx = base64.indexOf("base64,");
            if (idx > 0) {
                base64 = base64.substring(idx + 7);
            }
            fileBytes = java.util.Base64.getDecoder().decode(base64);
        }
        return fileBytes;
    }

    @Override
    public boolean delete(Records rec) {
        fileDelete(rec);
        return (boolean) rec.callSuper(IrAttachment.class, "delete");
    }

    public void fileDelete(Records records) {
        for (Records rec : records) {
            String type = rec.getString("storage");
            UploadFile entity = new UploadFile(rec.getString("file_path"));
            FileServiceFactory.build(type).deleteFile(entity);
        }
    }

    /**
     * 写文件
     *
     * @param rec
     * @param data
     * @return
     */
    public String fileWrite(Records rec, UploadConfig config, String fileName, String folder, byte[] data) {
        UploadFile entity = new UploadFile(fileName, folder, data);
        return FileServiceFactory.build(config).uploadFile(entity);
    }

    public byte[] getFileDataByRes(Records rec, String resModel, String resField, String resId) {
        Records record = rec.getEnv().get(resModel, resId);
        MetaModel meta = record.getMeta();
        BinaryBaseField bf = (BinaryBaseField) meta.getField(resField);
        if (bf.isAttachment()) {
            Records attachment = rec.find(Criteria.equal("res_model", resModel)
                    .and(Criteria.equal("res_field", resField))
                    .and(Criteria.equal("res_id", resId)), 0, 1, null);
            if (attachment.any()) {
                return getFileData(attachment);
            }
        } else {
            List<Map<String, Object>> list = (List<Map<String, Object>>) record.get(resField);
            if (list.size() > 0) {
                Map<String, Object> data = list.get(0);
                return (byte[]) data.get("data");
            }
        }
        return null;
    }

    public Map<String, Object> upload(Records records, String fileName, byte[] data, String resModel, String resField, String resId) {
        Records attachment = records.create(new KvMap() {{
            put("file_name", fileName);
            put("name", fileName);
            put("file_data", data);
            put("file_type", PathUtils.getFileExtension(fileName));
            put("res_model", resModel);
            put("res_field", resField);
            put("res_id", resId);
        }});
        return new KvMap() {{
            put("name", fileName);
            put("id", attachment.getId());
            put("type", attachment.get("file_type"));
            put("size", attachment.get("file_size"));
        }};
    }
}
