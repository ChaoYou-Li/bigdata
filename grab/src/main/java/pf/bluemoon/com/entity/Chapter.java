package pf.bluemoon.com.entity;

import lombok.Data;
import pf.bluemoon.com.anno.DBEntity;
import pf.bluemoon.com.anno.PrimryKey;

import java.io.Serializable;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 11:41
 * @Modified by
 * @Version 1.0.0
 * @Description 章节目录实体类
 */
@DBEntity(name = "chapter")
@Data
public class Chapter implements Serializable {
    /**
     * 唯一主键
     */
    @PrimryKey
    private Long id;

    /**
     * 图书实体主键
     */
    private Long bookId;

    /**
     * 章节编号
     */
    private Integer code;

    /**
     * 章节名字
     */
    private String name;
}
