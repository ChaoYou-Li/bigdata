package pf.bluemoon.com.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pf.bluemoon.com.entity.Book;
import pf.bluemoon.com.enums.BookType;
import pf.bluemoon.com.enums.SubdivisionType;
import pf.bluemoon.com.service.BookService;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-11 16:50
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class BookRepoPage implements PageProcessor {

    private static Book book;

    private Site site = Site.me().setRetryTimes(3).setSleepTime(100);

    @Override
    public void process(Page page) {
        String bookName = page.getHtml().xpath("/html/body/div/div[4]/div[2]/div/h1/text()").get();
        String author = page.getHtml().xpath("/html/body/div/div[4]/div[2]/div[1]/p[1]/text()").get().replace("作 者：", "");
        String updateCreate = page.getHtml().xpath("/html/body/div/div[4]/div[2]/div[1]/p[3]/text()").get().replace("最后更新：", "");
        String house = page.getHtml().xpath("/html/body/div/div[4]/div[1]/a[1]/text()").get();
        String type = page.getHtml().xpath("/html/body/div/div[4]/div[1]/a[2]/text()").get();
        String description = page.getHtml().xpath("/html/body/div/div[4]/div[2]/div[2]/p[2]/text()").get();

        book = new Book();
        book.setName(bookName);
        book.setHouse(house);
        book.setType(SubdivisionType.getEnumLikeNameAndType(type, BookType.FICTION));
        book.setDescription(description);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            book.setUpdateTime(format.parse(updateCreate));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        book.setVersion(1);
        book.setAuthor(author);
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static Book build(String url, Integer threadNum){
        Spider.create(new BookRepoPage())
                .addUrl(url)
                .thread(threadNum)
                .run();
        return book;
    }
}
