package pf.bluemoon.com.service;

import pf.bluemoon.com.entity.Paragraph;
import pf.bluemoon.com.enums.BookType;
import pf.bluemoon.com.enums.SubdivisionType;

import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 17:59
 * @Modified by
 * @Version 1.0.0
 * @Description 段落实体服务
 */
public interface ParagraphService extends BaseService<Paragraph> {

    List<Paragraph> getParagraphListByChapterId(Long chapterId);

}
