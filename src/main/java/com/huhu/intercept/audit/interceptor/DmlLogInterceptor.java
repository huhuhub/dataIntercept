package com.huhu.intercept.audit.interceptor;

import com.huhu.intercept.audit.annotation.DmlSqlAudit;
import com.huhu.intercept.audit.domain.ChangeData;
import com.huhu.intercept.audit.enumerate.DBActionTypeEnum;
import com.huhu.intercept.audit.factory.ParseFactory;
import com.huhu.intercept.audit.factory.SqlFactory;
import com.huhu.intercept.audit.mybatis.MybatisInvocation;
import com.huhu.intercept.audit.parse.DataPaser;
import com.huhu.intercept.audit.parse.SqlParserInfo;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
@Component
public class DmlLogInterceptor implements Interceptor {

    private static final Logger LOG = LoggerFactory.getLogger(DmlLogInterceptor.class);

    //全局开关
    @Value("${dml.log.globalSwitch:false}")
    private Boolean globalSwitch;
    //是否需要注解控制
    @Value("${dml.log.annotationSwitch:true}")
    private Boolean annotationSwitch;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        LOG.info("进入DML日志审计");
        if (globalSwitch == null || Boolean.FALSE.equals(globalSwitch)) {
            LOG.info("DML审计全局开关未开启");
            return invocation.proceed();
        }
        Object target = invocation.getTarget();
        //只要它是mybatis的线程
        if (target instanceof Executor) {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];
            String commandName = ms.getSqlCommandType().name();

            String id = ms.getId();
            String className = id.substring(0, id.lastIndexOf('.'));
            Class clz = Class.forName(className);
            if (Boolean.TRUE.equals(annotationSwitch) && !clz.isAnnotationPresent(DmlSqlAudit.class)) {
                LOG.info("DML审计注解未到位");
                return invocation.proceed();
            }
            LOG.info("开启DML审计");
            //根据Mapper中的命令名获取相应的存储对象（工厂模式）
            DataPaser dataPaser = ParseFactory.getInstance().creator(commandName);
            //临时存储参数
            MybatisInvocation mybatisInvocation = new MybatisInvocation(args, ms, parameter, (Executor) target);
            //实际执行sql
            BoundSql boundSql = ms.getBoundSql(mybatisInvocation.getParameter());
            String sql = boundSql.getSql();
            LOG.info("初始SQL为：{}", sql);
            //解析sql相关信息
            SqlParserInfo sqlParserInfo = new SqlParserInfo(sql, DBActionTypeEnum.valueOf(commandName));

            //方法执行之前解析数据
            List<ChangeData> changeTable = dataPaser.parseBefore(sqlParserInfo, mybatisInvocation);

            //获取数据库连接
            Connection connection = mybatisInvocation.getExecutor().getTransaction().getConnection();

            Configuration configuration = ms.getConfiguration();
            sql = SqlFactory.showSql(configuration, boundSql);
            LOG.info("dml sql:{}", sql);
            SqlFactory sqlFactory = new SqlFactory(sqlParserInfo, sql, changeTable, clz);
            //插入到数据库中
            sqlFactory.executeDmlLogSql(connection);
            if (!DBActionTypeEnum.INSERT.getValue().equals(commandName)){
                sqlFactory.executeHistorySql(connection);
            }
        }
        LOG.info("关闭DML审计");
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        //implements Interceptor
    }

    public Boolean getGlobalSwitch() {
        return globalSwitch;
    }

    public void setGlobalSwitch(Boolean globalSwitch) {
        this.globalSwitch = globalSwitch;
    }

    public Boolean getAnnotationSwitch() {
        return annotationSwitch;
    }

    public void setAnnotationSwitch(Boolean annotationSwitch) {
        this.annotationSwitch = annotationSwitch;
    }
}
