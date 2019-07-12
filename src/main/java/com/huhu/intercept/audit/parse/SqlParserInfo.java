package com.huhu.intercept.audit.parse;

import com.huhu.intercept.audit.enumerate.DBActionTypeEnum;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;

import java.util.List;

public class SqlParserInfo {
    private DBActionTypeEnum actionType;
    private String tableName;
    private String schemaName;
    private Table table;
    private Expression whereExpression;

    public SqlParserInfo(String sql, DBActionTypeEnum actionType) throws JSQLParserException {
        this.actionType = actionType;
        if (sql == null || sql.length() <= 0) {
            return;
        }
        //临时建立sql的statement（分结构存储sql）， 方便取sql中的相关信息
        Statement statement = CCJSqlParserUtil.parse(sql);

        if (actionType == DBActionTypeEnum.UPDATE) {
            Update updateStatement = (Update) statement;
            List<Table> updateTables = updateStatement.getTables();
            if (updateTables == null || updateTables.isEmpty()) {
                return;
            }
            this.table = updateTables.get(0);
            this.whereExpression = updateStatement.getWhere();
        } else if (actionType == DBActionTypeEnum.INSERT) {
            Insert deleteStatement = (Insert) statement;
            Table deleteTable = deleteStatement.getTable();
            if (deleteTable == null) {
                return;
            }
            this.table = deleteTable;
            this.whereExpression = null;
        } else if (actionType == DBActionTypeEnum.DELETE) {
            Delete deleteStatement = (Delete) statement;
            Table deleteTables = deleteStatement.getTable();
            if (deleteTables == null) {
                return;
            }
            this.table = deleteTables;
            this.whereExpression = deleteStatement.getWhere();
        }

        this.tableName = table.getName();
        this.schemaName = table.getSchemaName();
    }


    public DBActionTypeEnum getActionType() {
        return actionType;
    }

    public void setActionType(DBActionTypeEnum actionType) {
        this.actionType = actionType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public Expression getWhereExpression() {
        return whereExpression;
    }

    public void setWhereExpression(Expression whereExpression) {
        this.whereExpression = whereExpression;
    }
}

