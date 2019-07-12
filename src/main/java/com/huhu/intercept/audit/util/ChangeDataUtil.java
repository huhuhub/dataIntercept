package com.huhu.intercept.audit.util;


import com.huhu.intercept.audit.domain.ChangeData;
import com.huhu.intercept.audit.domain.ChangeObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class ChangeDataUtil {

    private ChangeDataUtil() {
        throw new IllegalStateException("util error");
    }

    public static ChangeData buildChangeDataForInsert(final Map<String, Object> afterDataMap) {
        ChangeData changeData = new ChangeData();
        List<ChangeObject> afterColumnList = new ArrayList<>();
        List<ChangeObject> beforeColumnList = new ArrayList<>();
        changeData.setAfterColumnList(afterColumnList);
        changeData.setBeforeColumnList(beforeColumnList);

        ChangeObject changeObject;
        for (Entry<String, Object> queryDataItem : afterDataMap.entrySet()) {
            String queryDataItemKey = queryDataItem.getKey();
            Object queryDataItemValue = queryDataItem.getValue();

            changeObject = new ChangeObject();
            changeObject.setName(queryDataItemKey);
            changeObject.setValue(queryDataItemValue);
            afterColumnList.add(changeObject);
        }
        return changeData;
    }


    public static ChangeData buildChangeDataForDelete(final Map<String, Object> beforeDataMap) {
        ChangeData changeData = new ChangeData();
        List<ChangeObject> afterColumnList = new ArrayList<>();
        List<ChangeObject> beforeColumnList = new ArrayList<>();
        changeData.setBeforeColumnList(beforeColumnList);
        changeData.setAfterColumnList(afterColumnList);


        ChangeObject changeObject;
        for (Entry<String, Object> queryDataItem : beforeDataMap.entrySet()) {
            Object queryDataItemValue = queryDataItem.getValue();
            String queryDataItemKey = queryDataItem.getKey();
            // set after change object
            changeObject = new ChangeObject();
            changeObject.setName(queryDataItemKey);
            changeObject.setValue(queryDataItemValue);
            beforeColumnList.add(changeObject);
        }
        return changeData;
    }

    public static ChangeData buildChangeDataForUpdate(final Map<String, Object> changeDataMap, HashMap<String, Object> queryDataMap) {
        ChangeData changeData = new ChangeData();
        List<ChangeObject> afterColumnList = new ArrayList<>();
        List<ChangeObject> beforeColumnList = new ArrayList<>();
        changeData.setAfterColumnList(afterColumnList);
        changeData.setBeforeColumnList(beforeColumnList);
        if (queryDataMap == null) {
            return changeData;
        }
        Map<String, Object> currentEntityMap = queryDataMap;
        ChangeObject changeObject;
        for (Entry<String, Object> queryDataItem : queryDataMap.entrySet()) {
            String queryDataItemKey = queryDataItem.getKey();
            Object queryDataItemValue = queryDataItem.getValue();

            // set before change object
            changeObject = new ChangeObject();
            changeObject.setName(queryDataItemKey);
            changeObject.setValue(queryDataItemValue);
            beforeColumnList.add(changeObject);

            // set after change object
            changeObject = new ChangeObject();
            changeObject.setName(queryDataItemKey);
            if (changeDataMap != null && changeDataMap.containsKey(queryDataItemKey.toLowerCase())) {
                Object resultValueString = changeDataMap.get(queryDataItemKey.toLowerCase());
                changeObject.setValue(resultValueString);
                currentEntityMap.put(queryDataItemKey, resultValueString);
            } else {
                changeObject.setValue(queryDataItemValue);
            }
            afterColumnList.add(changeObject);
        }
        changeData.setCurrentEntityMap(currentEntityMap);
        return changeData;
    }


}
