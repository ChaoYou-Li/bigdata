package pf.bluemoon.com.service;

import pf.bluemoon.com.entity.Sentence;
import pf.bluemoon.com.enums.SubdivisionType;

import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 17:34
 * @Modified by
 * @Version 1.0.0
 * @Description 句子实体服务
 */
public interface SentenceService extends BaseService<Sentence> {

    /**
     * 根据段落实体主键获取所在章节编号
     */
    Integer getChapterCodeByParagraphId(Long paragraphId);

    List<Sentence> getSentenceListByChapterId(Long chapterId);

    List<Sentence> getSentenceListByParagraphId(Long paragraphId);
}
