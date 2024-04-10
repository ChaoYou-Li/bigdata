package com.bluemoon.lcy.datajpa.enums;

/**
 * @Author chaoyou
 * @Date Create in 2024-01-04 13:57
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public enum IdxEnum {
    primary_key(1, "primary_key", "主键索引"),
    unique_key(2, "unique_key", "唯一索引"),
    foreign_key(3, "foreign_key", "外键索引"),
    full_text_key(4, "full_text_key", "普通索引"),

    ;

    private int code;
    private String field;
    private String remark;

    IdxEnum(int code, String field, String remark) {
        this.code = code;
        this.field = field;
        this.remark = remark;
    }

    public int getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public String getRemark() {
        return remark;
    }
}
