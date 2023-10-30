package pf.bluemoon.com.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pf.bluemoon.com.config.ScheduledTask;
import pf.bluemoon.com.entity.Book;
import pf.bluemoon.com.entity.resp.ResponseBean;
import pf.bluemoon.com.processor.BookRepoPage;
import pf.bluemoon.com.service.BookService;

import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-11 17:21
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@RequestMapping("/grab")
@RestController
public class BookController {

    @Autowired
    private BookService bookService;
    @Autowired
    private ScheduledTask task;

    @RequestMapping("/book/save")
    public ResponseBean save(String url){
        ResponseBean responseBean = ResponseBean.fail();
        try {
            Book book = BookRepoPage.build(url, 1);
            Book save = bookService.save(book);
            responseBean.setCode(ResponseBean.SUCCESS_CODE);
            responseBean.setSuccess(true);
            responseBean.setMsg("数据保存成功");
            responseBean.setData(save);
            return responseBean;
        } catch (Exception e){
            e.printStackTrace();
            responseBean.setMsg(e.getMessage());
            responseBean.setData(url);
            return responseBean;
        }
    }

    @RequestMapping("/book/del/{id}")
    public ResponseBean delete(@PathVariable Object id){
        ResponseBean responseBean = ResponseBean.fail();
        try {
            Book save = bookService.delete(id);
            responseBean.setCode(ResponseBean.SUCCESS_CODE);
            responseBean.setSuccess(true);
            responseBean.setMsg("数据保存成功");
            responseBean.setData(save);
            return responseBean;
        } catch (Exception e){
            e.printStackTrace();
            responseBean.setMsg(e.getMessage());
            responseBean.setData(id);
            return responseBean;
        }
    }

    @RequestMapping("/book/find/{id}")
    public ResponseBean findBookByPk(@PathVariable Object id){
        ResponseBean responseBean = ResponseBean.fail();
        try {
            Book save = bookService.selectByPk(id);
            responseBean.setCode(ResponseBean.SUCCESS_CODE);
            responseBean.setSuccess(true);
            responseBean.setMsg("数据查询成功");
            responseBean.setData(save);
            return responseBean;
        } catch (Exception e){
            e.printStackTrace();
            responseBean.setMsg(e.getMessage());
            responseBean.setData(id);
            return responseBean;
        }
    }

    @RequestMapping("/book/find")
    public ResponseBean findBookByFt(String author){
        ResponseBean responseBean = ResponseBean.fail();
        try {
            List<Book> bookList = bookService.selectByFt("author", author);
            responseBean.setCode(ResponseBean.SUCCESS_CODE);
            responseBean.setSuccess(true);
            responseBean.setMsg("数据查询成功");
            responseBean.setData(bookList);
            return responseBean;
        } catch (Exception e){
            e.printStackTrace();
            responseBean.setMsg(e.getMessage());
            responseBean.setData(author);
            return responseBean;
        }
    }



    @PostMapping("/test")
    public void test(){
        BookRepoPage.build("http://www.xinbqg.org/31/31467/", 1);
        return ;
    }
}
