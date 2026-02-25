package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.tools.IdWorker;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库锁
 *
 * @author eric
 */
@Model.Meta(name = "ir.lock", label = "数据库同步锁")
@Model.Service(remove = "@all")
public class IrLock extends Model {
    static Field name = Field.Char().label("名称");
    static Field lock_time = Field.DateTime().label("锁定时间");
    static Field release_time = Field.DateTime().label("释放时间");
    static Field locked = Field.Boolean().label("是否锁定");

    /**
     * 判断是否锁定
     */
    public boolean isLocked(Records rec, String name) {
        Records lock = rec.find(Criteria.equal("name", name));
        if (lock.any()) {
            boolean isLocked = lock.getBoolean("locked");
            if (isLocked) {
                Timestamp releaseTime = lock.getDateTime("release_time");
                return releaseTime.after(new Date());
            }
        }
        return false;
    }

    /**
     * 获取锁信息，isLocked, lockTime, releaseTime, lockBy
     */
    public Map<String, Object> getLockInfo(Records rec, String name) {
        Map<String, Object> result = new HashMap<>();
        Records lock = rec.find(Criteria.equal("name", name));
        if (lock.any()) {
            boolean isLocked = lock.getBoolean("locked");
            if (isLocked) {
                Timestamp releaseTime = lock.getDateTime("release_time");
                isLocked = releaseTime.after(new Date());
                if (isLocked) {
                    result.put("isLocked", true);
                    result.put("lockTime", lock.getDateTime("lock_time"));
                    result.put("releaseTime", lock.getDateTime("release_time"));
                    result.put("lockBy", lock.getRec("update_uid").get("present"));
                    return result;
                }
            }
        }
        result.put("isLocked", false);
        return result;
    }

    /**
     * 锁定指定名称的锁
     */
    public void lock(Records rec, String name, int lockMinutes) {
        Records lock = rec.find(Criteria.equal("name", name));
        if (!lock.any()) {
            try (Cursor cr = rec.getEnv().getDatabase().openCursor()) {
                String sql = "insert into ir_lock(id,name,lock_time,release_time,locked,create_uid,create_date,update_uid,update_date)"
                    + "values (%s,%s,%s,%s,%s,%s,%s,%s,%s)";
                Date now = new Date();
                cr.execute(sql, Arrays.asList(IdWorker.nextId(), name, now, Utils.addMinutes(now, lockMinutes),
                    true, rec.getEnv().getUserId(), now, rec.getEnv().getUserId(), now));
                cr.commit();
            } catch (Exception e) {
                throw new PlatformException(e);
            }
        } else {
            try (Cursor cr = rec.getEnv().getDatabase().openCursor()) {
                String sql = "update ir_lock set locked=%s,lock_time=%s,release_time=%s,update_uid=%s,update_date=%s where name=%s";
                Date now = new Date();
                cr.execute(sql, Arrays.asList(true, now, Utils.addMinutes(now, lockMinutes), rec.getEnv().getUserId(), now, name));
                cr.commit();
            } catch (Exception e) {
                throw new PlatformException(e);
            }
        }
        rec.getEnv().getCursor().execute("update ir_lock set locked=%s where name=%s", Arrays.asList(true, name));
    }

    /**
     * 释放指定名称的锁
     */
    public void unlock(Records rec, String name) {
        String sql = "update ir_lock set locked=%s,release_time=%s,update_uid=%s,update_date=%s where name=%s";
        Date now = new Date();
        rec.getEnv().getCursor().execute(sql, Arrays.asList(false, now, rec.getEnv().getUserId(), now, name));
    }
}
