package com.huhu.intercept.audit.parse;


import com.huhu.intercept.audit.domain.ChangeData;
import com.huhu.intercept.audit.mybatis.MybatisInvocation;
import com.huhu.intercept.audit.util.ChangeDataUtil;
import com.huhu.intercept.audit.util.MybatisParameterUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseUpdateData extends ParseContext {

    @Override
    public List<ChangeData> parseBefore(SqlParserInfo sqlParserInfo, MybatisInvocation mybatisInvocation) throws SQLException {
        List<ChangeData> results = null;
        MappedStatement mappedStatement = mybatisInvocation.getMappedStatement();
        Object updateParameterObject = mybatisInvocation.getParameter();
        BoundSql boundSql = mappedStatement.getBoundSql(mybatisInvocation.getParameter());

        //获取要更新数据
        ArrayList<HashMap<String, Object>> queryResults = query(mybatisInvocation, boundSql, sqlParserInfo);

        //获取更新字段列表
        Map<String, Object> updateDataMap = MybatisParameterUtils.getParameter(mappedStatement, boundSql, updateParameterObject);
        //组装变更列表
        results = buildChangeDatas(updateDataMap, queryResults, sqlParserInfo);
        return results;
    }

    private List<ChangeData> buildChangeDatas(final Map<String, Object> updateDataMap,
                                              final ArrayList<HashMap<String, Object>> queryResults, SqlParserInfo sqlParserInfo) {
        List<ChangeData> changeDatas = new ArrayList<>();
        if (queryResults != null && !queryResults.isEmpty()) {
            for (HashMap<String, Object> queryDataMap : queryResults) {
                ChangeData changeData = ChangeDataUtil.buildChangeDataForUpdate(updateDataMap, queryDataMap);
                changeData.setTableName(sqlParserInfo.getTableName());
                changeDatas.add(changeData);
            }
        }
        return changeDatas;
    }

}
