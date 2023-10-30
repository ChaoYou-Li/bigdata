package pf.bluemoon.com.service.Impl;

import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import pf.bluemoon.com.config.InitTable;
import pf.bluemoon.com.entity.Chapter;
import pf.bluemoon.com.entity.Paragraph;
import pf.bluemoon.com.entity.Sentence;
import pf.bluemoon.com.enums.BookType;
import pf.bluemoon.com.enums.SubdivisionType;
import pf.bluemoon.com.service.ChapterService;
import pf.bluemoon.com.service.ParagraphService;
import pf.bluemoon.com.service.SentenceService;
import pf.bluemoon.com.utils.GrabUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 17:48
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Service
public class SentenceServiceImpl extends BaseServiceImpl<Sentence> implements SentenceService {

    private static final Logger logger = LoggerFactory.getLogger(SentenceServiceImpl.class);

    @Autowired
    private ParagraphService paragraphService;

    @Override
    public Integer getChapterCodeByParagraphId(Long paragraphId) {
        return null;
    }

    @Override
    public List<Sentence> getSentenceListByChapterId(Long chapterId) {
        List<Sentence> result = new ArrayList<>();
        if (null == chapterId){
            return result;
        }
        List<Paragraph> paragraphList = paragraphService.getParagraphListByChapterId(chapterId);
        for (Paragraph paragraph : paragraphList) {
            result.addAll(getSentenceListByParagraphId(paragraph.getId()));
        }
        return result;
    }

    @Override
    public List<Sentence> getSentenceListByParagraphId(Long paragraphId) {
        if (null == paragraphId){
            return new ArrayList<>();
        }
        return selectALl().stream().filter(e -> e.getParagraphId().equals(paragraphId)).collect(Collectors.toList());
    }
}
