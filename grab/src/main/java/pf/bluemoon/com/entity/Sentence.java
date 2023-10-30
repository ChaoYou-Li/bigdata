package pf.bluemoon.com.entity;

import lombok.Data;
import pf.bluemoon.com.anno.DBEntity;
import pf.bluemoon.com.anno.PrimryKey;

import java.util.Date;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 17:08
 * @Modified by
 * @Version 1.0.0
 * @Description 句子实体类
 */
@DBEntity(name = "sentence")
@Data
public class Sentence {
    /**
     * 唯一主键
     */
    @PrimryKey
    private Long id;

    /**
     * 段落实体主键
     */
    private Long paragraphId;

    /**
     * 句子编号
     */
    private Integer code;

    /**
     * 句子内容
     */
    private String content;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否删除
     */
    private Boolean isDel;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
