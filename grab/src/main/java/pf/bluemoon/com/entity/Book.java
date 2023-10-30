package pf.bluemoon.com.entity;

import lombok.Data;
import pf.bluemoon.com.anno.DBEntity;
import pf.bluemoon.com.anno.Index;
import pf.bluemoon.com.anno.PrimryKey;
import pf.bluemoon.com.anno.UniqueKey;
import pf.bluemoon.com.enums.SubdivisionType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 10:52
 * @Modified by
 * @Version 1.0.0
 * @Description 图书实体类
 */
@DBEntity(name = "book")
@Data
public class Book implements Serializable {
    /**
     * 唯一主键
     */
    @PrimryKey
    private Long id;

    /**
     * 图书名称
     */
    @Index
    @UniqueKey(name = "uni_book_idx")
    private String name;

    /**
     * 类型
     */
    @UniqueKey(name = "uni_book_idx")
    private SubdivisionType type;

    /**
     * 摘要说明
     */
    private String description;

    /**
     * 章节目录
     */
    private List<Chapter> chapterList;

    /**
     * 总字数
     */
    private Long totalWords;

    /**
     * 图书作者
     */
    @Index
    @UniqueKey(name = "uni_book_idx")
    private String author;

    /**
     * 出版社
     */
    @UniqueKey(name = "uni_book_idx")
    private String house;

    /**
     * 印刷版本
     */
    @UniqueKey(name = "uni_book_idx")
    private Integer version;

    /**
     * 创作时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date updateTime;

}
