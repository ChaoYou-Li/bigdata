package pf.bluemoon.com.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import pf.bluemoon.com.entity.Chapter;
import pf.bluemoon.com.enums.BookType;
import pf.bluemoon.com.enums.SubdivisionType;
import pf.bluemoon.com.service.ChapterService;
import pf.bluemoon.com.utils.GrabUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 17:40
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Service
public class ChapterServiceImpl extends BaseServiceImpl<Chapter> implements ChapterService {

    private static final Logger logger = LoggerFactory.getLogger(ChapterServiceImpl.class);

    @Override
    public List<Chapter> getChapterListByBookId(Long bookId) {
        if (null == bookId){
            return new ArrayList<>();
        }
        return selectALl().stream().filter(e -> e.getBookId().equals(bookId)).collect(Collectors.toList());
    }
}
