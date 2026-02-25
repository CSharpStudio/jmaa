package org.jmaa.sdk.util;

import java.util.HashMap;

/**
 * 文件定义
 */
public class FileInfo extends HashMap<String, Object> {
    public FileInfo() {

    }

    public String getId() {
        return (String) get("id");
    }

    public FileInfo setId(String id) {
        put("id", id);
        return this;
    }

    public String getName() {
        return (String) get("name");
    }

    public FileInfo setName(String name) {
        put("name", name);
        return this;
    }

    public String getType() {
        return (String) get("type");
    }

    public FileInfo setType(String type) {
        put("type", type);
        return this;
    }

    public Integer getSize() {
        return (Integer) get("size");
    }

    public FileInfo setSize(Integer size) {
        put("size", size);
        return this;
    }

    public byte[] getData() {
        return (byte[]) get("data");
    }

    public FileInfo setData(byte[] data) {
        put("data", data);
        put("size", data.length);
        return this;
    }
}
