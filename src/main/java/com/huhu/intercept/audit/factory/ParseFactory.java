package com.huhu.intercept.audit.factory;

import com.huhu.intercept.audit.enumerate.DBActionTypeEnum;
import com.huhu.intercept.audit.parse.DataPaser;
import com.huhu.intercept.audit.parse.ParseDeleteData;
import com.huhu.intercept.audit.parse.ParseInsertData;
import com.huhu.intercept.audit.parse.ParseUpdateData;

import java.util.HashMap;
import java.util.Map;


public class ParseFactory {

    private ParseFactory() {
    }

    private static ParseFactory factory = new ParseFactory();
    private static Map<String, DataPaser> parseMap = new HashMap<>();

    static {
        parseMap.put(DBActionTypeEnum.UPDATE.getValue(), new ParseUpdateData());
        parseMap.put(DBActionTypeEnum.INSERT.getValue(), new ParseInsertData());
        parseMap.put(DBActionTypeEnum.DELETE.getValue(), new ParseDeleteData());
    }

    public static ParseFactory getInstance() {
        return factory;
    }

    public DataPaser creator(String commandName) {
        return parseMap.get(commandName);
    }
}
