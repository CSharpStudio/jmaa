package org.jmaa.sdk.fields;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.DeleteMode;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.Criteria;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.core.BaseModel;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.DbColumn;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.data.Query.SqlClause;
import org.jmaa.sdk.util.Cache;
import org.jmaa.sdk.util.Tuple;
import org.jmaa.sdk.data.Query;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 多对多
 *
 * @author Eric Liang
 */
public class Many2manyField extends RelationalMultiField<Many2manyField> {
    @JsonIgnore
    String relation;
    @JsonIgnore
    String column1;
    @JsonIgnore
    String column2;
    @JsonIgnore
    Boolean autoJoin;
    @JsonIgnore
    Integer limit;
    @JsonIgnore
    DeleteMode ondelete = DeleteMode.Cascade;

    public Many2manyField() {
        type = Constants.MANY2MANY;
    }

    public Many2manyField(String comodel, String relation, String column1, String column2) {
        this();
        args.put("comodelName", comodel);
        args.put("relation", relation);
        args.put("column1", column1);
        args.put("column2", column2);
    }

    public String getRelation() {
        return relation;
    }

    public String getColumn1() {
        return column1;
    }

    public String getColumn2() {
        return column2;
    }

    /**
     * 删除模式
     *
     * @param ondelete
     * @return
     */
    public Many2manyField ondelete(DeleteMode ondelete) {
        args.put("ondelete", ondelete);
        return this;
    }

    @Override
    protected void updateDb(Records model, Map<String, DbColumn> columns) {
        Cursor cr = model.getEnv().getCursor();
        SqlDialect sd = cr.getSqlDialect();
        Records comodel = model.getEnv().get(getComodel());
        if (!sd.tableExists(cr, relation)) {
            sd.createMany2ManyTable(cr, relation, column1, column2, String.format("RELATION BETWEEN %s AND %s",
                model.getMeta().getTable(), comodel.getMeta().getTable()));
            model.getEnv().getRegistry().addPostInit(e -> {
                updateDbForeignKey(model);
            });
        }
    }

    public void updateDbForeignKey(Records model) {
        Registry reg = model.getEnv().getRegistry();
        MetaModel meta = model.getMeta();
        MetaModel comodel = reg.get(getComodel());
        String module = getModule();
        if (StringUtils.isEmpty(module)) {
            module = "-";
        }
        if (!StringUtils.isEmpty(meta.getTable())) {
            reg.addForeignKey(relation, getColumn1(), meta.getTable(), "id", "cascade", meta.getName(), module, false);
        }
        if (!StringUtils.isEmpty(comodel.getTable())) {
            String onDelete = ondelete == null ? "cascade" : ondelete.getName();
            reg.addForeignKey(relation, getColumn2(), comodel.getTable(), "id", onDelete, meta.getName(), module, true);
        }
    }

    public List<String> find(Records records, Integer limit, Integer offset) {
        Cursor cr = doRead(records, limit, offset);
        List<String> list = new ArrayList<>();
        for (Object[] row : cr.fetchAll()) {
            list.add((String) row[1]);
        }
        return list;
    }

    public int count(Records records) {
        Records comodel = records.getEnv().get(getComodel());
        Criteria criteria = getCriteria(records);
        Boolean activeTest = ObjectUtils.toBoolean(records.getEnv().getContext().get("active_test"), true);
        if (activeTest && comodel.getMeta().getFields().containsKey("active")) {
            criteria.and(Criteria.equal("active", true));
        }
        comodel.call("flushSearch", criteria, Collections.emptyList(), "");
        Query wquery = BaseModel.whereCalc(comodel, criteria, true);
        String orderBy = BaseModel.generateOrderBy(comodel, null, wquery);
        SqlClause q = wquery.getSql();
        Cursor cr = records.getEnv().getCursor();
        String rel = cr.quote(relation);
        String id1 = cr.quote(column1);
        String id2 = cr.quote(column2);
        String where = q.getWhere();
        if (StringUtils.isEmpty(where)) {
            where = "1=1";
        }
        String sql = "SELECT count(DISTINCT " + rel + "." + id2 + ")"
            + " FROM " + rel + ", " + q.getFrom() + " WHERE " + where + " AND " + rel + "." + id1
            + " IN %s AND " + rel + "." + id2 + "=" + cr.quote(comodel.getMeta().getTable()) + ".id";
        List<Object> params = q.getParams();
        params.add(Arrays.asList(records.getIds()));
        cr.execute(sql, params);
        return ObjectUtils.toInt(cr.fetchOne()[0]);
    }

    Cursor doRead(Records records, Integer limit, Integer offset) {
        Records comodel = records.getEnv().get(getComodel());
        Criteria criteria = getCriteria(records);
        Boolean activeTest = ObjectUtils.toBoolean(records.getEnv().getContext().get("active_test"), true);
        if (activeTest && comodel.getMeta().getFields().containsKey("active")) {
            criteria.and(Criteria.equal("active", true));
        }
        comodel.call("flushSearch", criteria, Collections.emptyList(), "");
        Query wquery = BaseModel.whereCalc(comodel, criteria, true);
        SqlClause q = wquery.getSql();
        Cursor cr = records.getEnv().getCursor();
        String rel = cr.quote(relation);
        String id1 = cr.quote(column1);
        String id2 = cr.quote(column2);
        String where = q.getWhere();
        if (StringUtils.isEmpty(where)) {
            where = "1=1";
        }
        String sql = "SELECT " + rel + "." + id1 + ", " + rel + "." + id2
            + " FROM " + rel + ", " + q.getFrom() + " WHERE " + where + " AND " + rel + "." + id1
            + " IN %s AND " + rel + "." + id2 + "=" + cr.quote(comodel.getMeta().getTable()) + ".id ORDER BY " + rel + ".id";
        sql = cr.getSqlDialect().getPaging(sql, limit, offset);
        List<Object> params = q.getParams();
        params.add(Arrays.asList(records.getIds()));
        cr.execute(sql, params);
        return cr;
    }

    @Override
    public void read(Records records) {
        Cursor cr = doRead(records, limit, 0);
        Map<String, List<String>> group = new HashMap<>();
        for (Object[] row : cr.fetchAll()) {
            List<String> list = group.get(row[0]);
            if (list == null) {
                list = new ArrayList<>();
                group.put((String) row[0], list);
            }
            list.add((String) row[1]);
        }
        Cache cache = records.getEnv().getCache();
        for (Records r : records) {
            cache.set(r, this, group.get(r.getId()));
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "AlibabaMethodTooLong"})
    protected Records doSave(List<Tuple<Records, Object>> recordsCommandsList, boolean create) {
        if (recordsCommandsList.size() == 0) {
            return null;
        }
        Records model = recordsCommandsList.get(0).getItem1().browse();
        Records comodel = model.getEnv().get(getComodel()).withContext(getContext());
        Set<String> ids = new HashSet<>();
        recordsCommandsList.stream().forEach(t -> ids.addAll(Arrays.asList(t.getItem1().getIds())));
        Records records = model.browse(ids);

        if (isStore()) {
            Collection<String> missingIds = records.getEnv().getCache().getMissingIds(records, this);
            if (missingIds.size() > 0) {
                read(records.browse(missingIds));
            }
        }

        Map<String, Set<String>> oldRelation = new HashMap<>();
        Map<String, Set<String>> newRelation = new HashMap<>();
        Map<String, Set<String>> delRelation = new HashMap<>();
        for (Records record : records) {
            List<String> rids = Arrays.asList(((Records) record.get(getName())).getIds());
            oldRelation.put(record.getId(), new HashSet<>(rids));
            newRelation.put(record.getId(), new LinkedHashSet<>());
        }

        BiConsumer<String[], String> relationAdd = (xs, y) -> {
            for (String x : xs) {
                Set<String> ys = newRelation.get(x);
                if (ys == null) {
                    ys = new LinkedHashSet<>();
                    newRelation.put(x, ys);
                }
                ys.add(y);
            }
        };

        BiConsumer<String[], String> relationRemove = (xs, y) -> {
            for (String x : xs) {
                Set<String> ys = delRelation.get(x);
                if (ys == null) {
                    ys = new HashSet<>();
                    delRelation.put(x, ys);
                }
                ys.add(y);
            }
        };

        BiConsumer<String[], List<String>> relationSet = (xs, ys) -> {
            for (String x : xs) {
                newRelation.put(x, new LinkedHashSet<>(ys));
            }
        };

        Consumer<List<String>> relationDelete = (ys) -> {
            for (Set<String> ys1 : oldRelation.values()) {
                ys1.removeAll(ys);
            }
            for (Set<String> ys1 : newRelation.values()) {
                ys1.removeAll(ys);
            }
        };

        Cursor cr = records.getEnv().getCursor();
        for (Tuple<Records, Object> tuple : recordsCommandsList) {
            List<Tuple<String[], Map<String, Object>>> toCreate = new ArrayList<>();
            List<String> toDelete = new ArrayList<>();
            List<Object> commands = (List<Object>) tuple.getItem2();
            Records recs = tuple.getItem1();
            for (Object cmdList : commands) {
                if (!(cmdList instanceof List)) {
                    continue;
                }
                List<Object> command = (List<Object>) cmdList;
                Command cmd = Command.get(command.get(0));
                if (cmd == Command.ADD) {
                    toCreate.add(new Tuple<>(recs.getIds(), (Map<String, Object>) command.get(2)));
                } else if (cmd == Command.UPDATE) {
                    comodel.browse((String) command.get(1)).update((Map<String, Object>) command.get(2));
                } else if (cmd == Command.DELETE) {
                    toDelete.add((String) command.get(1));
                } else if (cmd == Command.REMOVE) {
                    relationRemove.accept(recs.getIds(), (String) command.get(1));
                } else if (cmd == Command.PUT) {
                    relationAdd.accept(recs.getIds(), (String) command.get(1));
                } else if (cmd == Command.CLEAR || cmd == Command.REPLACE) {
                    toCreate = toCreate.stream().map(c -> {
                        Set<String> set = new HashSet<>(Arrays.asList(c.getItem1()));
                        set.removeAll(Arrays.asList(recs.getIds()));
                        return new Tuple<>(set.toArray(ArrayUtils.EMPTY_STRING_ARRAY), c.getItem2());
                    }).collect(Collectors.toList());
                    relationSet.accept(recs.getIds(),
                        cmd == Command.REPLACE ? (List<String>) command.get(2) : Collections.emptyList());
                    // 先删后新增逻辑判断
                    if (cmd == Command.REPLACE) {
                        for (Records rec : records) {
                            Set<String> oldSet = oldRelation.get(rec.getId());
                            Set<String> removeSet = delRelation.computeIfAbsent(rec.getId(), k -> new HashSet<>());
                            for (String ys : oldSet) {
                                removeSet.add(ys);
                            }
                            oldSet.clear();
                            doDelete(cr, new HashMap<String, Set<String>>() {{
                                put(rec.getId(), removeSet);
                            }}, new HashMap<String, Set<String>>() {{
                                put(rec.getId(), Collections.emptySet());
                            }});
                            removeSet.clear();
                        }
                    }
                }
            }

            if (toCreate.size() > 0) {
                Records lines = comodel
                    .createBatch(toCreate.stream().map(t -> t.getItem2()).collect(Collectors.toList()));
                for (List<Object> line : ArrayUtils.zip(lines, toCreate)) {
                    relationAdd.accept(((Tuple<String[], Map<String, Object>>) line.get(1)).getItem1(),
                        ((Records) line.get(0)).getId());
                }
            }

            if (toDelete.size() > 0) {
                comodel.browse(toDelete).delete();
                relationDelete.accept(toDelete);
            }
        }

        doDelete(cr, delRelation, newRelation);
        doInsert(cr, newRelation, oldRelation);

        // update the cache of self
        Cache cache = records.getEnv().getCache();
        for (Records record : records) {
            cache.remove(record, this);
        }
        // TODO update the cache of inverse fields


        return records.filter(record -> !newRelation.get(record.getId()).equals(oldRelation.get(record.getId())));
    }

    void doInsert(Cursor cr, Map<String, Set<String>> newRelation, Map<String, Set<String>> oldRelation) {
        // process pairs to add (beware of duplicates)
        List<List<String>> pairs = new ArrayList<>();
        for (Entry<String, Set<String>> e : newRelation.entrySet()) {
            Set<String> set = new LinkedHashSet<>(e.getValue());
            set.removeAll(oldRelation.get(e.getKey()));
            for (String id : set) {
                pairs.add(Arrays.asList(IdWorker.nextId(), e.getKey(), id));
            }
        }


        if (pairs.size() > 0) {
            String sql = String.format("INSERT INTO %s(id,%s,%s) VALUES (%%s,%%s,%%s)", cr.quote(relation), cr.quote(column1),
                cr.quote(column2));
            for (List<String> params : pairs) {
                cr.execute(sql, params);
            }
        }
    }

    void doDelete(Cursor cr, Map<String, Set<String>> delRelation, Map<String, Set<String>> newRelation) {
        // process pairs to remove
        List<List<String>> pairs = new ArrayList<>();
        for (Entry<String, Set<String>> e : delRelation.entrySet()) {
            Set<String> set = new HashSet<>(e.getValue());
            set.removeAll(newRelation.get(e.getKey()));
            for (String id : set) {
                pairs.add(Arrays.asList(e.getKey(), id));
            }
        }
        if (pairs.size() > 0) {
            Map<String, Set<String>> yToXs = new HashMap<>();
            for (List<String> p : pairs) {
                Set<String> xs = yToXs.get(p.get(1));
                if (xs == null) {
                    xs = new HashSet<>();
                    yToXs.put(p.get(1), xs);
                }
                xs.add(p.get(0));
            }

            if (isStore()) {
                Map<Set<String>, Set<String>> xsToYs = new HashMap<>();
                for (Entry<String, Set<String>> e : yToXs.entrySet()) {
                    Set<String> ys = xsToYs.get(e.getValue());
                    if (ys == null) {
                        ys = new HashSet<>();
                        xsToYs.put(e.getValue(), ys);
                    }
                    ys.add(e.getKey());
                }

                String condition = String.format("%s IN %%s AND %s IN %%s", cr.quote(column1), cr.quote(column2));
                String where = xsToYs.entrySet().stream().map(p -> condition).collect(Collectors.joining(" OR "));
                String sql = String.format("DELETE FROM %s WHERE %s", cr.quote(relation), where);
                List<Object> params = new ArrayList<>();
                for (Entry<Set<String>, Set<String>> e : xsToYs.entrySet()) {
                    params.add(Arrays.asList(e.getKey().toArray()));
                    params.add(Arrays.asList(e.getValue().toArray()));
                }
                cr.execute(sql, params);
            }
        }
    }
}
