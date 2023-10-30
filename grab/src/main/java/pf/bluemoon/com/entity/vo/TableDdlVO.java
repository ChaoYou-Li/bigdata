package pf.bluemoon.com.entity.vo;

import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-21 16:47
 * @Modified by
 * @Version 1.0.0
 * @Description 表结构关系存储
 */
public class TableDdlVO {
    /**
     * 表名
     */
    private String tableName;
    /**
     * 主键
     */
    private String primryKey;
    /**
     * 其他键
     */
    private Map<String, List<String>> uniKeyMapFieldList;

    public TableDdlVO() {
    }

    public TableDdlVO(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPrimryKey() {
        return primryKey;
    }

    public void setPrimryKey(String primryKey) {
        this.primryKey = primryKey;
    }

    public List<String> getIndexList() {
        return null;
    }

    public void setIndexList(List<String> indexList) {

    }

    public Map<String, String> getForeignKeyMapField() {
        return null;
    }

    public void setForeignKeyMapField(Map<String, String> foreignKeyMapField) {

    }

    public Map<String, List<String>> getUniKeyMapFieldList() {
        return uniKeyMapFieldList;
    }

    public void setUniKeyMapFieldList(Map<String, List<String>> uniKeyMapFieldList) {
        this.uniKeyMapFieldList = uniKeyMapFieldList;
    }
}
