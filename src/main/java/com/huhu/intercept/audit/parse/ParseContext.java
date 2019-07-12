package com.huhu.intercept.audit.parse;

import com.huhu.intercept.audit.factory.SqlFactory;
import com.huhu.intercept.audit.mybatis.MybatisInvocation;
import com.huhu.intercept.audit.util.MSUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ParseContext implements DataPaser {

    private static final Logger LOG = LoggerFactory.getLogger(ParseContext.class);

    /**
     * 查询
     *
     * @param mybatisInvocation
     * @param boundSql
     * @param sqlParserInfo
     * @return
     * @throws SQLException
     */
    public ArrayList<HashMap<String, Object>> query(MybatisInvocation mybatisInvocation,
                                                    BoundSql boundSql, SqlParserInfo sqlParserInfo) throws SQLException {
        //获取当前表名
        Table table = sqlParserInfo.getTable();
        MappedStatement mappedStatement = mybatisInvocation.getMappedStatement();
        MappedStatement selectMappedStatement = MSUtils.newHashMapMappedStatement(mappedStatement);
        List<Column> updateColumns = new ArrayList<>();
        Column column = new Column();
        column.setColumnName("*");
        updateColumns.add(column);
        //获取where条件
        Expression whereExpression = sqlParserInfo.getWhereExpression();
        //组装select sql语句
        Select select = JsqlParserHelper.getSelect(table, updateColumns, whereExpression);
        String selectSqlString = select.toString();

        List<ParameterMapping> selectParamMap = new ArrayList<>();
        List<String> whereIdList = JsqlParserHelper.getWhereColumn(whereExpression);
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null) {
            parameterMappings = new ArrayList<>();
        }
        //拼接sql条件
        for (int i = 0; i < parameterMappings.size(); i++) {
            ParameterMapping paramMap = parameterMappings.get(i);
            if (paramMap.getProperty() != null && isContain(whereIdList, paramMap.getProperty())) {
                selectParamMap.add(paramMap);
                whereIdList.remove(paramMap.getProperty().toLowerCase());
            } else if (i >= SqlFactory.getFlagRegex(boundSql.getSql(), "where") && i < SqlFactory.getFlagRegex(boundSql.getSql(), "having")) {
                selectParamMap.add(paramMap);
            }
        }
        //生成改造后的sql
        BoundSql queryBoundSql = new BoundSql(mybatisInvocation.getMappedStatement().getConfiguration(), selectSqlString, selectParamMap, mybatisInvocation.getParameter());
        LOG.info("改造后但未加参数的sql:{}",queryBoundSql.getSql());
        for (ParameterMapping parameterMapping : selectParamMap) {
            if (boundSql.hasAdditionalParameter(parameterMapping.getProperty())) {
                queryBoundSql.setAdditionalParameter(parameterMapping.getProperty(), boundSql.getAdditionalParameter(parameterMapping.getProperty()));
            }
        }
        //执行查询
        Object queryResultList = mybatisInvocation.getExecutor().query(selectMappedStatement, mybatisInvocation.getParameter(), RowBounds.DEFAULT, null, null, queryBoundSql);

        ArrayList<HashMap<String, Object>> queryResults = (ArrayList<HashMap<String, Object>>) queryResultList;
        return queryResults;
    }

    private boolean isContain(List<String> whereList, String property) {
        if (whereList == null || property == null) {
            return false;
        }
        switch (property) {
            case "audit.now":
                property = "lastupdatedby";
                break;
            case "audit.user":
                property = "lastupdatedate";
                break;
            default:
                property = property.toLowerCase();
                break;
        }
        return whereList.contains(property);
    }

}
