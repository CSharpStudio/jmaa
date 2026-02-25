package org.jmaa.sdk.fields;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.Criteria;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.util.Cache;
import org.jmaa.sdk.util.FileInfo;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.Tuple;

/**
 * 二进制基类
 *
 * @author Eric Liang
 */
public class BinaryBaseField<T extends BaseField<T>> extends BaseField<T> {
    @Related
    Boolean attachment = true;
    @Related
    int limit = 1;
    @Related
    String resModel;

    public BinaryBaseField() {
        prefetch = false;
        sortable = false;
    }

    @SuppressWarnings("unchecked")
    public T attachment() {
        args.put("attachment", true);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T attachment(boolean attachment) {
        args.put("attachment", attachment);
        return (T) this;
    }

    /**
     * 最大上传文件数量
     *
     * @param limit
     * @return
     */
    public T limit(int limit) {
        args.put("limit", limit);
        return (T) this;
    }

    public T resModel(String model) {
        args.put("resModel", model);
        return (T) this;
    }

    public String getResModel() {
        return Utils.isNotEmpty(resModel) ? resModel : getModelName();
    }

    public Boolean isAttachment() {
        return attachment;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public ColumnType getColumnType() {
        return attachment ? ColumnType.None : ColumnType.Binary;
    }

    @Override
    protected Map<String, Object> getAttrs(MetaModel model, String name) {
        Map<String, Object> attrs = super.getAttrs(model, name);
        if (!(Boolean) attrs.getOrDefault(Constants.STORE, true)) {
            attrs.put("attachment", false);
        }
        return attrs;
    }

    @Override
    public Object convertToColumn(Object value, Records record, boolean validate) {
        if (Utils.isEmpty(value)) {
            if (validate && isRequired()) {
                throw new ValidationException(record.l10n("%s 不能为空", getLabel()));
            }
            return null;
        }
        if (value instanceof List) {
            List<Map> list = (List) value;
            byte[] bytes = new byte[0];
            for (Map item : list) {
                if (!item.containsKey("delete")) {
                    bytes = ArrayUtils.addAll(bytes, getBinary(item, record, validate));
                }
            }
            return bytes;
        }
        if (value instanceof Map) {
            Map item = (Map) value;
            if (!item.containsKey("delete")) {
                return getBinary(item, record, validate);
            }
        }
        if (value instanceof byte[]) {
            return value;
        }
        if (validate) {
            throw new ValidationException(record.l10n("%s不支持转换成byte[]", value));
        }
        return null;
    }

    byte[] getBinary(Map<String, Object> item, Records record, boolean validate) {
        String name = (String) item.get("name");
        Object data = item.get("data");
        if (Utils.isEmpty(name) && validate) {
            throw new ValidationException(record.l10n("文件名称不能为空"));
        }
        if (Utils.isEmpty(data) && validate) {
            throw new ValidationException(record.l10n("文件内容不能为空"));
        }
        if (Utils.isEmpty(name) || Utils.isEmpty(data)) {
            return new byte[0];
        }
        byte[] array = name.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = ObjectUtils.toByteArray(array.length);
        bytes = ArrayUtils.addAll(bytes, array);
        byte[] content;
        if (data instanceof String) {
            String base64 = (String) data;
            int idx = base64.indexOf("base64,");
            if (idx > 0) {
                base64 = base64.substring(idx + 7);
            }
            content = Base64.getDecoder().decode(base64);
        } else {
            content = (byte[]) data;
        }
        bytes = ArrayUtils.addAll(bytes, ObjectUtils.toByteArray(content.length));
        bytes = ArrayUtils.addAll(bytes, content);
        return bytes;
    }

    Map<String, Object> getFile(File file) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", file.getName());
        map.put("type", PathUtils.getFileExtension(file.getName()));
        byte[] content = IoUtils.getFileBytes(file);
        map.put("size", content.length);
        map.put("data", content);
        return map;
    }

    @Override
    public Object convertToCache(Object value, Records rec, boolean validate) {
        if (Utils.isEmpty(value)) {
            if (validate && isRequired()) {
                throw new ValidationException(rec.l10n("%s 不能为空", getLabel()));
            }
            return null;
        }
        if (value instanceof File) {
            File file = (File) value;
            return getFile(file);
        }
        if (value instanceof File[]) {
            File[] files = (File[]) value;
            List<Map<String, Object>> list = new ArrayList<>(files.length);
            for (File file : files) {
                list.add(getFile(file));
            }
            return list;
        }
        if (value instanceof List || value instanceof Map) {
            return value;
        }
        if (validate) {
            throw new ValidationException(rec.l10n("%s不支持转换成文件信息", value));
        }
        return value;
    }

    @Override
    public Object convertToRecord(Object value, Records rec) {
        if (value == null) {
            return isRequired() ? Collections.emptyList() : null;
        }
        if (value instanceof String) {
            value = Base64.getDecoder().decode((String) value);
        }
        if (value instanceof byte[]) {
            byte[] data = (byte[]) value;
            List<FileInfo> files = new ArrayList<>();
            try {
                int index = 0;
                while (index < data.length) {
                    int length = ObjectUtils.toInt(ArrayUtils.subarray(data, index, index + 4));
                    index += 4;
                    String name = new String(ArrayUtils.subarray(data, index, index + length), StandardCharsets.UTF_8);
                    index += length;
                    length = ObjectUtils.toInt(ArrayUtils.subarray(data, index, index + 4));
                    index += 4;
                    byte[] content = ArrayUtils.subarray(data, index, index + length);
                    index += length;
                    FileInfo file = new FileInfo() {{
                        setName(name);
                        setData(content);
                        setType(PathUtils.getFileExtension(name));
                    }};
                    files.add(file);
                }
            } catch (Exception e) {
                FileInfo file = new FileInfo() {{
                    put("error", "文件读取失败");
                    put("debug", ThrowableUtils.getDebug(e));
                }};
                files.add(file);
            }
            return files;
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(Records records) {
        Map<String, Object> data = new HashMap<>();
        Records attachments = records.getEnv().get("ir.attachment").find(
            Criteria.equal("res_model", getResModel())
                .and(Criteria.equal("res_field", this.getName()))
                .and(Criteria.in("res_id", Arrays.asList(records.getIds()))));
        for (Records item : attachments) {
            FileInfo file = new FileInfo() {{
                setId(item.getId());
                setName(item.getString("file_name"));
                setSize(item.getInteger("file_size"));
                setType(item.getString("file_type"));
            }};
            String resId = item.getString("res_id");
            List<Map<String, Object>> files = (List<Map<String, Object>>) data.get(resId);
            if (files == null) {
                files = new ArrayList<>();
                data.put(resId, files);
            }
            files.add(file);
        }
        Cache cache = records.getEnv().getCache();
        for (Records rec : records) {
            Object cacheData = data.get(rec.getId());
            cache.set(rec, this, cacheData);
        }
    }

    @Override
    public void create(List<Tuple<Records, Object>> recordValues) {
        if (recordValues == null) {
            return;
        }
        for (Tuple<Records, Object> tuple : recordValues) {
            write(tuple.getItem1(), tuple.getItem2());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Records write(Records records, Object value) {
        if (!attachment) {
            return super.write(records, value);
        }
        List<Map<String, Object>> dataList = null;
        if (value instanceof List) {
            dataList = (List<Map<String, Object>>) value;
        } else if (value instanceof Map) {
            dataList = new ArrayList<>();
            dataList.add((Map<String, Object>) value);
        }
        if (dataList == null) {
            return records;
        }
        Records attachment = records.getEnv().get("ir.attachment");
        List<String> toDelete = new ArrayList<>();
        List<String> toUpdate = new ArrayList<>();
        List<Map<String, Object>> toCreate = new ArrayList<>();
        for (Map<String, Object> data : dataList) {
            String toDeleteId = (String) data.get("delete");
            if (StringUtils.isNotEmpty(toDeleteId)) {
                toDelete.add(toDeleteId);
                continue;
            }
            String dataId = (String) data.get("dataId");
            if (StringUtils.isNotEmpty(dataId)) {
                toUpdate.add(dataId);
                continue;
            }
            for (Records rec : records) {
                Map<String, Object> createValues = new HashMap<>();
                createValues.put("name", data.get("name"));
                createValues.put("res_model", getResModel());
                createValues.put("res_field", getName());
                createValues.put("res_id", rec.getId());
                createValues.put("file_type", data.get("type"));
                createValues.put("file_name", data.get("name"));
                createValues.put("file_data", data.get("data"));
                toCreate.add(createValues);
            }
        }
        if (toDelete.size() > 0) {
            attachment.browse(toDelete).delete();
        }
        if (toCreate.size() > 0) {
            attachment.createBatch(toCreate);
        }
        if (toUpdate.size() > 0) {
            for (Records record : records) {
                attachment.browse(toUpdate).update(new KvMap() {{
                    put("res_id", record.getId());
                    put("res_field", getName());
                    put("res_model", getResModel());
                }});
            }
        }
        return records;
    }
}
