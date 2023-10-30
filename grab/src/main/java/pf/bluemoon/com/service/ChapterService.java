package pf.bluemoon.com.service;

import pf.bluemoon.com.entity.Chapter;
import pf.bluemoon.com.enums.SubdivisionType;

import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 17:39
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public interface ChapterService extends BaseService<Chapter> {

    List<Chapter> getChapterListByBookId(Long bookId);

}
