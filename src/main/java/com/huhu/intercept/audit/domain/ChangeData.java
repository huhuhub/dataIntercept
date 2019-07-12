package com.huhu.intercept.audit.domain;

import java.util.List;
import java.util.Map;

public class ChangeData {
    // 更改后的所有属性和值
    public List<ChangeObject> afterColumnList;
    // 更改的所有属性的以及它对应的原始值,即更改之前的值
    public List<ChangeObject> beforeColumnList;
    // 数据库表名
    public String tableName;
    // 对应的数据库表的主键ID
    public Long entityId;
    // 更新之后的对象的属性及值的键值对
    public Map<String, Object> currentEntityMap;

    public Map<String, Object> getCurrentEntityMap() {
        return currentEntityMap;
    }

    public void setCurrentEntityMap(Map<String, Object> currentEntityMap) {
        this.currentEntityMap = currentEntityMap;
    }

    public List<ChangeObject> getAfterColumnList() {
        return afterColumnList;
    }

    public void setAfterColumnList(List<ChangeObject> afterColumnList) {
        this.afterColumnList = afterColumnList;
    }

    public List<ChangeObject> getBeforeColumnList() {
        return beforeColumnList;
    }

    public void setBeforeColumnList(List<ChangeObject> beforeColumnList) {
        this.beforeColumnList = beforeColumnList;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

}
