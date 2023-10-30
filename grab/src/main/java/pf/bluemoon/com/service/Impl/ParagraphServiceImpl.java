package pf.bluemoon.com.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import pf.bluemoon.com.entity.Chapter;
import pf.bluemoon.com.entity.Paragraph;
import pf.bluemoon.com.enums.BookType;
import pf.bluemoon.com.enums.SubdivisionType;
import pf.bluemoon.com.service.ChapterService;
import pf.bluemoon.com.service.ParagraphService;
import pf.bluemoon.com.utils.GrabUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 18:00
 * @Modified by
 * @Version 1.0.0
 * @Description 段落实体服务
 */
@Service
public class ParagraphServiceImpl extends BaseServiceImpl<Paragraph> implements ParagraphService {

    private static final Logger logger = LoggerFactory.getLogger(ParagraphServiceImpl.class);

    @Autowired
    private ChapterService chapterService;

    @Override
    public List<Paragraph> getParagraphListByChapterId(Long chapterId) {
        if (null == chapterId){
            return new ArrayList<>();
        }
        return selectALl().stream().filter(e -> e.getChapterId().equals(chapterId)).collect(Collectors.toList());
    }
}
