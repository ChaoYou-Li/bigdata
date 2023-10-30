package pf.bluemoon.com.hbase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import pf.bluemoon.com.entity.Book;
import pf.bluemoon.com.entity.Chapter;
import pf.bluemoon.com.entity.Sentence;
import pf.bluemoon.com.entity.vo.WriteVO;
import pf.bluemoon.com.enums.SubdivisionType;
import pf.bluemoon.com.processor.BookRepoPage;
import pf.bluemoon.com.processor.ChapterRepoPage;
import pf.bluemoon.com.processor.ContentRepoPage;
import pf.bluemoon.com.processor.ContentRunnable;
import pf.bluemoon.com.service.BookService;
import pf.bluemoon.com.service.ChapterService;
import pf.bluemoon.com.service.ParagraphService;
import pf.bluemoon.com.service.SentenceService;
import pf.bluemoon.com.utils.FileUtils;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@SpringBootTest
class GrapApplicationTests {

    @Autowired
    private BookService bookService;
    @Autowired
    private ChapterService chapterService;
    @Autowired
    private ParagraphService paragraphService;
    @Autowired
    private SentenceService sentenceService;
    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() throws FileNotFoundException {
        String currentDirectory = System.getProperty("user.dir"); // 获取当前项目根目录路径
        File file = new File("D:\\workspace\\idea\\springboot\\bigdata\\data\\book");
        boolean mkdir = file.mkdir();
        System.out.println(mkdir);
    }

    @Test
    void saveBook(){
        Book book = new Book();
        book.setAuthor("朝油");
        book.setName("我的好日子");
        book.setType(SubdivisionType.ROMANCE);
        book.setTotalWords(4000L);
        book.setHouse("工业出版社");
        book.setDescription("这是我的第一本书");
        book.setVersion(1);
        book.setCreateTime(new Date());
        book.setUpdateTime(new Date());
        Book save = bookService.save(book);
        System.out.print(save);
    }

    @Test
    void moni(){
        String str = "Hello World";

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(str.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String uniqueNumber = number.toString(10);

            System.out.println("Unique number for '" + str + "': " + uniqueNumber);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Test
    void test(){
        String filePath = "D:\\workspace\\idea\\springboot\\bigdata\\grab\\data\\book\\data.txt";
        String data = FileUtils.readByte(filePath, 37, 663);
    }

    @Test
    void chapter(){
        String baseUrl = "http://www.xinbqg.org";
        String baseFilePath = "E:\\novel\\text\\";
        Map<String, String> build = local();
        int page = 100;
        int index = 1;
        for (Map.Entry<String, String> entry : build.entrySet()) {
            if (index < 6936){
                if (index % 100 == 0){
                    page = 100 + index;
                }

                index++;
                continue;
            }
            String url = entry.getValue();
            String chapter = entry.getKey();
            List<String> contentList = ContentRepoPage.build(baseUrl + url, 3);
            FileUtils.batchAppendLine(contentList, baseFilePath + page + File.separator + chapter + ".txt");
            if (index % 100 == 0){
                page = 100 + index;
            }

            index++;
        }
    }

    @Test
    void content(){
        List<String> contentList = ContentRepoPage.build("http://www.xinbqg.org/31/31467/14560497.html", 1);
        System.out.println(contentList);
    }

    Map<String, String> local(){
        File file = new File("C:\\Users\\admin\\Desktop\\chapter.html");
        Map<String, String> chapterMap = new LinkedHashMap<>();
        // 读取指定行数的内容
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.lines().forEach(line -> {
                if (null != line && line.length() > 1 && line.startsWith("<dd><a")){
                    line = line.replace("<dd><a href='", "")
                            .replace(" >", "")
                            .replace("</a></dd>", "");
                    String[] split = line.split("'");
                    if (null != split && split.length == 2){
                        String url = split[0];
                        String chapter = split[1];
                        if (chapter.startsWith("第")){
                            chapterMap.put(chapter, url);
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chapterMap;
    }

}
