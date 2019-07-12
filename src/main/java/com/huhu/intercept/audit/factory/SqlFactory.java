package com.huhu.intercept.audit.factory;

import com.alibaba.fastjson.JSONObject;

import com.huhu.intercept.audit.annotation.DmlSqlAudit;
import com.huhu.intercept.audit.domain.ChangeData;
import com.huhu.intercept.audit.domain.ChangeObject;
import com.huhu.intercept.audit.parse.SqlParserInfo;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SqlFactory {

    public static final String COW_UUID = "cowUuid";

    private String dmlLogSql;
    private List<Object> dmlLogParam;
    private String historyTable;
    private List<ChangeData> changeDataList;
    private boolean hasHistory;
    //用于存储insert操作的相关参数
    private static final Map<String, Object> map = new HashMap<>();

    static {
        map.put(COW_UUID, null); //牛只UUID
    }

    /**
     * 生成sql对应的消息
     *
     * @param sqlParserInfo  sql相关信息 如scheme，table name,where expression
     * @param sql            最终sql
     * @param changeDataList 更改前后的数据
     * @param clz            当前类（mybatis plugin中指的是当前Mapper）
     */
    public SqlFactory(SqlParserInfo sqlParserInfo, String sql, List<ChangeData> changeDataList, Class clz) {

        //记录功能
        String features = null;
        String schema = sqlParserInfo.getSchemaName();
        String table = sqlParserInfo.getTableName();
        //默认不插入历史记录
        this.hasHistory = false;
        String defaultDmlLogTable = "";
        String defaultHistoryTable = "";
        if (clz.isAnnotationPresent(DmlSqlAudit.class)) {
            //强转直接取方法上的值，类似于一个Bean getter
            DmlSqlAudit dmlSqlAudit = (DmlSqlAudit) clz.getAnnotation(DmlSqlAudit.class);
            this.historyTable = dmlSqlAudit.historyTable();
            if (!"".equals(historyTable.trim())) {
                if (schema != null) {
                    defaultHistoryTable = schema + historyTable;
                } else {
                    defaultHistoryTable = historyTable;
                }
            }
            String dmlLogTable = dmlSqlAudit.dmlLogTable();
            if (dmlLogTable != null && !"".equals(dmlLogTable.trim())) {
                if (schema != null) {
                    defaultDmlLogTable = schema + dmlLogTable;
                } else {
                    defaultDmlLogTable = dmlLogTable;
                }
            }
            this.hasHistory = dmlSqlAudit.hasHistory();
            features = dmlSqlAudit.feature();
        }
        //拼接插入的sql，这里可以自定义存取的dml日志
        String insertDmlLogSql = "INSERT INTO " + defaultDmlLogTable + "(DML_UUID,TABLE_NAME,SPECIFIC_SQL,BULL_NUMBER," +
                "FEATURES,OPERATION_TYPE,CREATED_BY) VALUES (?,?,?,?,?,?,?)";
        List<Object> dmlLogParam2 = new ArrayList<>();
        dmlLogParam2.add(table);
        dmlLogParam2.add(sql);//需要与参数结合
        //如果是更新或者删除，从更改前后的数据中获取
        List<ChangeObject> beforeChangeList = new ArrayList<>();
        if (!changeDataList.isEmpty() && changeDataList.get(0).beforeColumnList != null) {
            beforeChangeList = changeDataList.get(0).beforeColumnList;
        }
        HashSet<Object> set = new HashSet<>();
        for (ChangeObject changeObject : beforeChangeList) {
            if (changeObject.getName().equals("COW_UUID")) {
                set.add(changeObject.getValue());
            }
        }
        JSONObject jsonObject = new JSONObject();
        if (set.isEmpty() && map.get(COW_UUID) != null) {
            set.add(map.get(COW_UUID));
        }
        jsonObject.put("value", set);
        dmlLogParam2.add(jsonObject.toJSONString());
        //获取功能
        dmlLogParam2.add(features);
        //操作方式
        String operationType = sqlParserInfo.getActionType().getValue();
        dmlLogParam2.add(operationType);

        this.dmlLogSql = insertDmlLogSql;
        this.dmlLogParam = dmlLogParam2;
        this.historyTable = defaultHistoryTable;
        this.changeDataList = changeDataList;
    }

    public int executeDmlLogSql(Connection connection) throws SQLException {
        return executeSql(connection, dmlLogSql, dmlLogParam);
    }

    public int executeHistorySql(Connection connection) throws SQLException {
        int result = 0;
        if (!hasHistory || "".equals(historyTable)) {
            return result;
        }
        if (changeDataList == null || changeDataList.isEmpty()) {
            return result;
        }
        for (ChangeData changeData : changeDataList) {
            if (changeData == null) {
                continue;
            }
            List<ChangeObject> changeObjectList = changeData.getBeforeColumnList();
            if (changeObjectList == null || changeObjectList.isEmpty()) {
                continue;
            }
            StringBuilder columnBuilder = new StringBuilder(" (");
            StringBuilder valueBuilder = new StringBuilder(" (");
            List<Object> params = new ArrayList<>();
            columnBuilder.append("DML_UUID");
            columnBuilder.append(",");
            valueBuilder.append("?");
            valueBuilder.append(",");
            for (ChangeObject changeObject : changeObjectList) {
                if (changeObject == null || changeObject.getName() == null) {
                    continue;
                }
                //将原表的主键另外命名为SOURCE_ID存储下来
                if ("ID".equals(changeObject.getName())) {
                    columnBuilder.append("SOURCE_ID");
                    params.add(changeObject.getValue());
                } else {
                    columnBuilder.append(changeObject.getName());
                    params.add(changeObject.getValue());
                }
                columnBuilder.append(",");
                valueBuilder.append("?");
                valueBuilder.append(",");
            }
            columnBuilder.deleteCharAt(columnBuilder.length() - 1);
            valueBuilder.deleteCharAt(valueBuilder.length() - 1);
            columnBuilder.append(") ");
            valueBuilder.append(") ");

            String insertHistorySql = "INSERT INTO " + historyTable + " " + columnBuilder.toString() + "VALUES" + valueBuilder.toString();
            result = result + executeSql(connection, insertHistorySql, params);
        }
        return result;
    }

    public int executeSql(Connection connection, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            for (Object obj : params) {
                ps.setObject(i++, obj);
            }
            ps.execute();
            return ps.getUpdateCount();
        }
    }

    public static String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }


    public static String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (!parameterMappings.isEmpty() && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        getCustomValue(metaObject, propertyName);
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        getCustomValue(metaObject, propertyName);
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    }
                }
            }
        }
        return sql;
    }

    private static void getCustomValue(MetaObject metaObject, String propertyName) {
        map.forEach((key, value) -> {
            if (key.equals(propertyName)) {
                map.replace(COW_UUID, metaObject.getValue(propertyName));
            }
        });
    }

    public static int getParameterCount(String sql) {
        int originLength = sql.length();
        sql = sql.replaceAll("\\?", "");
        int replaceLength = sql.length();
        return originLength - replaceLength;
    }

    public static int getFlagRegex(String sql, String flagRegex) {
        int sqlCount = getParameterCount(sql);
        int whereFlag = sql.toLowerCase().lastIndexOf(flagRegex);
        if (whereFlag <= 0) {
            return sqlCount;
        }
        String whereBeforeSql = sql.substring(0, whereFlag);
        int whereBeforeCount = getParameterCount(whereBeforeSql);
        if (whereBeforeCount >= sqlCount) {
            return sqlCount;
        }
        return whereBeforeCount;
    }
}
