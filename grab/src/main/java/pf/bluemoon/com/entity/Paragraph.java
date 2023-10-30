package pf.bluemoon.com.entity;

import lombok.Data;
import pf.bluemoon.com.anno.DBEntity;
import pf.bluemoon.com.anno.PrimryKey;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 14:40
 * @Modified by
 * @Version 1.0.0
 * @Description 段落内容实体类
 */
@DBEntity(name = "paragraph")
@Data
public class Paragraph implements Serializable {
    /**
     * 唯一主键
     */
    @PrimryKey
    private Long id;

    /**
     * 章节实体主键
     */
    private Long chapterId;

    /**
     * 段落编号
     */
    private Integer code;
}
