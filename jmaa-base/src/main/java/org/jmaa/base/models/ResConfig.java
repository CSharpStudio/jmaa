package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.DbColumn;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.tools.IdWorker;
import org.jmaa.sdk.util.Cache;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 系统配置项
 *
 * @author Eric Liang
 */
@Model.Meta(name = "res.config", label = "配置")
public class ResConfig extends ValueModel {
    static final String RECORD_ID = "res-config";

    static Field pwd_complexity = Field.Boolean().label("密码必须符合复杂性要求")
        .help("在更改或创建密码时执行复杂性要求: 至少包含大写字母、小写字母、数字、特殊字符四类中的三类");
    static Field pwd_min_length = Field.Integer().label("密码长度最小值").defaultValue(6);
    static Field default_pwd = Field.Char().label("默认密码");
    static Field pwd_validity = Field.Integer().label("密码有效期(天)").defaultValue(45);
    static Field login_try_times = Field.Integer().label("允许错误次数").defaultValue(5);
    static Field login_lock_time = Field.Float().label("锁定时长(秒)").defaultValue(300);

    /**
     * 更新时判断id是否存在，因为配置是自定义保存逻辑，所以这里不处理，直接返回
     */
    @Override
    public Records exists(Records rec) {
        return rec;
    }

    /**
     * 重写init方法创建表，配置表使用name-value的结构保存数据
     */
    @Override
    public void init(Records rec) {
        Cursor cr = rec.getEnv().getCursor();
        SqlDialect sd = cr.getSqlDialect();
        MetaModel meta = rec.getMeta();
        String table = meta.getTable();
        Map<String, DbColumn> columns = new HashMap<>();
        columns.put("name", new DbColumn("name", sd.getColumnType(ColumnType.VarChar, 200, null), 200, false));
        columns.put("value", new DbColumn("value", sd.getColumnType(ColumnType.VarChar, 800, null), 800, true));
        columns.put("update_uid",
            new DbColumn("update_uid", sd.getColumnType(ColumnType.VarChar, 13, null), 13, false));
        columns.put("update_date", new DbColumn("update_date", sd.getColumnType(ColumnType.DateTime), null, false));
        if (!sd.tableExists(cr, table)) {
            sd.createModelTable(cr, table, meta.getLabel());
            for (Entry<String, DbColumn> e : columns.entrySet()) {
                DbColumn column = e.getValue();
                sd.createColumn(cr, table, column.getColumn(), column.getType(), null, column.isNullable());
            }
            //默认密码不配置在field上的defaultValue，避免loadView泄露
            cr.execute("INSERT INTO res_config(id, name, value, update_uid, update_date) values (%s, %s, %s, %s, "
                    + cr.getSqlDialect().getNowUtc() + ")",
                Arrays.asList(IdWorker.nextId(), "default_pwd", "888888", rec.getEnv().getUserId()));
        } else {
            Map<String, DbColumn> cols = sd.tableColumns(cr, table);
            for (Entry<String, DbColumn> e : columns.entrySet()) {
                DbColumn column = e.getValue();
                if (!cols.containsKey(column.getColumn())) {
                    sd.createColumn(cr, table, column.getColumn(), column.getType(), null, column.isNullable());
                }
            }
        }
    }

    /**
     * 配置不需要创建，直接返回id为res-config的数据集
     */
    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        return rec.browse(RECORD_ID);
    }

    /**
     * 直接返回id为res-config的数据集
     */
    @Override
    public Records find(Records rec, Criteria criteria, Integer offset, Integer limit, String order) {
        return rec.browse(RECORD_ID);
    }

    /**
     * 更新数据，使用name-value的结构保存数据
     */
    @Override
    public void update(Records records, Map<String, Object> values) {
        records.ensureOne();
        MetaModel meta = records.getMeta();
        Cursor cr = records.getEnv().getCursor();
        String sql = "SELECT name FROM res_config";
        cr.execute(sql);
        Set<String> names = cr.fetchAll().stream().map(row -> (String) row[0]).collect(Collectors.toSet());
        for (Entry<String, Object> e : values.entrySet()) {
            String fieldName = e.getKey();
            meta.getField(fieldName);
            Object value = e.getValue();
            if (value != null && !(value instanceof String)) {
                value = value.toString();
            }
            if (names.contains(fieldName)) {
                cr.execute("UPDATE res_config SET value=%s,update_uid=%s,update_date=" + cr.getSqlDialect().getNowUtc()
                    + " WHERE name=%s", Arrays.asList(value, records.getEnv().getUserId(), fieldName));
            } else {
                cr.execute("INSERT INTO res_config(id, name, value, update_uid, update_date) values (%s, %s, %s, %s, "
                        + cr.getSqlDialect().getNowUtc() + ")",
                    Arrays.asList(IdWorker.nextId(), fieldName, value, records.getEnv().getUserId()));
            }
        }
    }

    /**
     * 读取数据，如果配置不存在，将插入数据
     */
    @Override
    public List<Map<String, Object>> read(Records rec, Collection<String> fields) {
        return doRead(rec, fields, true);
    }

    public List<Map<String, Object>> doRead(Records rec, Collection<String> fields, boolean usePresent) {
        MetaModel meta = rec.getMeta();
        List<MetaField> metaFields = fields.stream().filter(f -> !Constants.ID.equals(f)).map(f -> meta.getField(f))
            .collect(Collectors.toList());
        List<String> names = metaFields.stream().filter(f -> f.isStore() && f.getColumnType() != ColumnType.None)
            .map(f -> f.getName()).collect(Collectors.toList());
        Cursor cr = rec.getEnv().getCursor();
        String sql = "SELECT name, value FROM res_config WHERE name in %s";
        cr.execute(sql, Arrays.asList(names));
        Map<String, Object> values = new HashMap<>();
        for (Object[] row : cr.fetchAll()) {
            values.put((String) row[0], row[1]);
        }
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.ID, RECORD_ID);
        Cache cache = rec.getEnv().getCache();
        for (MetaField field : metaFields) {
            String fieldName = field.getName();
            Object value = null;
            if (field.isStore()) {
                if (values.containsKey(fieldName)) {
                    value = values.get(fieldName);
                } else if (field.getColumnType() != ColumnType.None) {
                    value = field.getDefault(rec);
                    try (Cursor c = rec.getEnv().getRegistry().getTenant().getDatabase().openCursor()) {
                        c.execute(
                            "INSERT INTO res_config(id, name, value, update_uid, update_date) values (%s, %s, %s, %s, "
                                + c.getSqlDialect().getNowUtc() + ")",
                            Arrays.asList(IdWorker.nextId(), fieldName, value, Constants.SYSTEM_USER));
                        c.commit();
                    }
                } else {
                    field.read(rec);
                    value = rec.get(fieldName);
                }
                Object cacheValue = field.convertToCache(value, rec, true);
                cache.update(rec, field, Arrays.asList(cacheValue));
            } else {
                value = rec.get(fieldName);
            }
            value = field.convertToRecord(value, rec);
            value = field.convertToRead(value, rec, usePresent);
            map.put(fieldName, value);
        }
        for (Entry<String, Object> e : map.entrySet()) {
            cache.update(rec, meta.getField(e.getKey()), Arrays.asList(e.getValue()));
        }
        return Arrays.asList(map);
    }

    /**
     * 读取字段
     */
    @Override
    public void fetchField(Records rec, MetaField field) {
        doRead(rec, Arrays.asList(field.getName()), false);
    }
}
