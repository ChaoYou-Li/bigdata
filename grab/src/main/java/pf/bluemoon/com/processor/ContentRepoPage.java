package pf.bluemoon.com.processor;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import pf.bluemoon.com.entity.Book;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-26 13:17
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ContentRepoPage implements PageProcessor {

    private static List<String> contentList = new LinkedList<>();

    private Site site = Site.me().setRetryTimes(3).setSleepTime(100);

    @Override
    public void process(Page page) {
        Element content = page.getHtml().getDocument().getElementById("content");
        List<Node> nodes = content.childNodes();
        for (Node node : nodes) {
            if (node.nodeName().equalsIgnoreCase("div") || node.nodeName().equalsIgnoreCase("br")){
                continue;
            }
            if (node.toString().startsWith("&nbsp;")){
                String text = node.toString().replace("&nbsp;", "");
                contentList.add(text);
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static List<String> build(String url, Integer threadNum){
        contentList.clear();
        Spider.create(new ContentRepoPage())
                .addUrl(url)
                .thread(threadNum)
                .run();
        return contentList;
    }
}
