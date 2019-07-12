package com.huhu.intercept.audit.parse;


import com.huhu.intercept.audit.domain.ChangeData;
import com.huhu.intercept.audit.mybatis.MybatisInvocation;
import com.huhu.intercept.audit.util.ChangeDataUtil;
import com.huhu.intercept.audit.util.MybatisParameterUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParseInsertData extends ParseContext {

    @Override
    public List<ChangeData> parseBefore(SqlParserInfo sqlParserInfo, MybatisInvocation mybatisInvocation) throws SQLException {
        MappedStatement mappedStatement = mybatisInvocation.getMappedStatement();
        Object updateParameterObject = mybatisInvocation.getParameter();
        BoundSql boundSql = mappedStatement.getBoundSql(mybatisInvocation.getParameter());

        // 获取更新字段列表
        Map<String, Object> updateDataMap = MybatisParameterUtils.getParameter(mappedStatement, boundSql,
                updateParameterObject);

        List<ChangeData> changeDatas = new ArrayList<>();
        ChangeData changeData = ChangeDataUtil.buildChangeDataForInsert(updateDataMap);
        changeData.setTableName(sqlParserInfo.getTableName());
        changeData.setCurrentEntityMap(updateDataMap);
        changeDatas.add(changeData);

        return changeDatas;
    }

}
