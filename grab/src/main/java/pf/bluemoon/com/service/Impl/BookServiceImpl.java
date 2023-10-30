package pf.bluemoon.com.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import pf.bluemoon.com.entity.Book;
import pf.bluemoon.com.enums.BookType;
import pf.bluemoon.com.service.BookService;
import pf.bluemoon.com.utils.GrabUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-11 17:24
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Service
public class BookServiceImpl extends BaseServiceImpl<Book> implements BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);

}
