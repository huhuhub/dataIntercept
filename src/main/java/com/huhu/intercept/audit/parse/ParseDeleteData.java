package com.huhu.intercept.audit.parse;


import com.huhu.intercept.audit.domain.ChangeData;
import com.huhu.intercept.audit.mybatis.MybatisInvocation;
import com.huhu.intercept.audit.util.ChangeDataUtil;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ParseDeleteData extends ParseContext {

    @Override
    public List<ChangeData> parseBefore(SqlParserInfo sqlParserInfo, MybatisInvocation mybatisInvocation) throws SQLException {
        MappedStatement mappedStatement = mybatisInvocation.getMappedStatement();
        BoundSql boundSql = mappedStatement.getBoundSql(mybatisInvocation.getParameter());
        // 获取要更新数据
        ArrayList<HashMap<String, Object>> beforeRsults = query(mybatisInvocation, boundSql, sqlParserInfo);

        List<ChangeData> results = buildChangeDatas(beforeRsults, sqlParserInfo);

        return results;
    }

    private List<ChangeData> buildChangeDatas(final ArrayList<HashMap<String, Object>> beforeResults,
                                              SqlParserInfo sqlParserInfo) {
        List<ChangeData> changeDatas = new ArrayList<>();
        if (beforeResults != null && !beforeResults.isEmpty()) {
            for (HashMap<String, Object> queryDataMap : beforeResults) {
                ChangeData changeData = ChangeDataUtil.buildChangeDataForDelete(queryDataMap);
                changeData.setTableName(sqlParserInfo.getTableName());
                changeData.setCurrentEntityMap(queryDataMap);
                changeDatas.add(changeData);
            }
        }
        return changeDatas;
    }

}
