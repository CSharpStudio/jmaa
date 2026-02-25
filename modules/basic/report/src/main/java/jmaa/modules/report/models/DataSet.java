package jmaa.modules.report.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.data.SqlFormat;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.data.xml.SqlTemplateBuilder;
import org.jmaa.sdk.exceptions.DataException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.ArrayUtils;
import org.jmaa.sdk.tools.IdWorker;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Model.Meta(name = "rpt.dataset", label = "数据集", authModel = "rpt.report")
@Model.UniqueConstraint(name = "code_unique", fields = {"code", "report_id"})
public class DataSet extends Model {
    static Field code = Field.Char().label("编码");
    static Field name = Field.Char().label("名称");
    static Field content = Field.Text().label("内容");
    static Field report_id = Field.Many2one("rpt.report").label("报表").ondelete(DeleteMode.Cascade);

    @Constrains("content")
    public void contentConstrains(Records records) {
        for (Records record : records) {
            String sql = record.getString("content");
            if (Utils.isNotBlank(sql)) {
                validateSql(sql);
            }
        }
    }

    @Override
    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        Object code = values.get("code");
        if (Utils.isEmpty(code)) {
            values.put("code", IdWorker.nextId());
        }
        return (Map<String, Object>) callSuper(rec, values);
    }

    public Object readDataSetColumns(Records dataSet) {
        String content = dataSet.getString("content");
        Cursor cr = dataSet.getEnv().getCursor();
        SqlDialect sqlDialect = cr.getSqlDialect();
        SqlTemplate template = SqlTemplateBuilder.getBuilder().createSqlTemplate(content);
        Map<String, Object> params = new HashMap<>();
        DataSet.extractParams(content).forEach(p -> params.put(p, ""));
        SqlFormat sql = template.process(params);
        String querySql = sqlDialect.getPaging(sql.getSql(), 1, 0);
        validateSql(querySql);
        Connection connection = null;
        try {
            connection = dataSet.getEnv().getDatabase().getConnection();
            SqlFormat format = cr.getSqlFormat(querySql, sql.getParmas());
            PreparedStatement statement = connection.prepareStatement(format.getSql());
            int parameterIndex = 1;
            for (Object p : format.getParmas()) {
                statement.setObject(parameterIndex++, sqlDialect.prepareObject(p));
            }
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            return getColumnMeta(resultSet, sqlDialect);
        } catch (Exception e) {
            return ArrayUtils.EMPTY_OBJECT_ARRAY;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static Collection<String> extractParams(String content) {
        Set<String> result = new HashSet<>();
        Pattern pattern = Pattern.compile("#\\{([^\\[\\}]+)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(1).trim());
        }
        return result;
    }

    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE",
        "TRUNCATE", "RENAME", "COMMIT", "ROLLBACK", "GRANT", "REVOKE"
    );

    /**
     * 验证sql是否合法
     */
    public static void validateSql(String sql) {
        if (Utils.isBlank(sql)) {
            throw new ValidationException("SQL语句不能为空");
        }

        // 去除SQL前后空格，转换为大写便于校验（忽略大小写）
        String trimmedSql = sql.trim().toUpperCase(Locale.ROOT);

        // 2.1 检查是否以SELECT开头（允许前面有注释，但实际场景建议禁止注释）
        if (!trimmedSql.startsWith("SELECT")) {
            throw new ValidationException("仅允许执行SELECT语句，禁止执行: " + sql);
        }

        // 2.2 检查是否包含危险关键字（分割SQL为单词，避免部分匹配，如"SELECTUPDATE"）
        // 简单分割：按非字母分割，提取所有单词
        String[] words = trimmedSql.split("[^A-Z_]+");
        for (String word : words) {
            if (FORBIDDEN_KEYWORDS.contains(word)) {
                throw new ValidationException("SQL包含禁止的操作关键字: " + word);
            }
        }
    }

    /**
     * 获取ResultSet的字段信息
     */
    public static Object[] getColumnMeta(ResultSet resultSet, SqlDialect sqlDialect) {
        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            Object[] columns = new Object[meta.getColumnCount()];
            for (int i = 1; i <= columns.length; i++) {
                columns[i - 1] = Arrays.asList(sqlDialect.getColumnLabel(meta.getColumnLabel(i)), meta.getColumnTypeName(i), meta.getColumnClassName(i));
            }
            return columns;
        } catch (SQLException e) {
            throw new DataException("读取行失败", e);
        }
    }
}
