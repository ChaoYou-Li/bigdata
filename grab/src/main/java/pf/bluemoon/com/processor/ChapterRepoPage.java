package pf.bluemoon.com.processor;

import com.alibaba.fastjson.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import pf.bluemoon.com.entity.Book;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Json;
import us.codecraft.webmagic.selector.Selectable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-26 13:17
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ChapterRepoPage implements PageProcessor {
    public static Map<String, String> chapterMap = new LinkedHashMap<>();

    private Site site = Site.me().setRetryTimes(3).setSleepTime(100);

    @Override
    public void process(Page page) {
        Element body = page.getHtml().getDocument().getElementById("list");
        List<Node> nodes = body.childNodes();
        int index = 1;
        for (Node node : nodes) {
            if (!node.nodeName().equalsIgnoreCase("dl")){
                continue;
            }
            for (Node childNode : node.childNodes()) {
                if (!childNode.nodeName().equalsIgnoreCase("dd")){
                    continue;
                }
                for (Node a : childNode.childNodes()) {
                    if (a.nodeName().equalsIgnoreCase("a")){
                        String url = a.attributes().get("href");
                        String text = a.childNode(0).toString();
                        if (text.startsWith("第")){
                            if (null == chapterMap.get(text)){
                                chapterMap.put(text, url);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static Map<String, String> build(String url, Integer threadNum){
        chapterMap.clear();
        Spider.create(new ChapterRepoPage())
                .addUrl(url)
                .thread(threadNum)
                .run();
        return chapterMap;
    }
}
