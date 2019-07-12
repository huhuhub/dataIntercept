package com.huhu.intercept.audit.parse;

import com.huhu.intercept.audit.domain.ChangeData;
import com.huhu.intercept.audit.mybatis.MybatisInvocation;

import java.sql.SQLException;
import java.util.List;

public interface DataPaser {

    /**
     * 在执行update方法之前解析数据
     *
     * @param sqlParserInfo
     * @param mybatisInvocation
     * @return
     * @throws Throwable
     */
    List<ChangeData> parseBefore(SqlParserInfo sqlParserInfo, MybatisInvocation mybatisInvocation) throws
            SQLException;
}
