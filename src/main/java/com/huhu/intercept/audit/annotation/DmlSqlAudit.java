package com.huhu.intercept.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DML日志审计
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DmlSqlAudit {
    /**
     * 是否有历史记录
     */
    boolean hasHistory() default false;

    /**
     * 指定历史表的表名
     */
    String historyTable() default "";

    /**
     * 指定日志表的表名
     */
    String dmlLogTable();

    /**
     * 功能描述
     */
    String feature();
}