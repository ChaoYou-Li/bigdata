package pf.bluemoon.com.processor;

import pf.bluemoon.com.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-26 16:58
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ContentRunnable implements Runnable{
    private String baseUrl;
    private String baseFilePath;
    private List<Map.Entry<String, String>> arrayList;
    private int page;

    public ContentRunnable(String baseUrl, String baseFilePath, List<Map.Entry<String, String>> arrayList, int page) {
        this.baseUrl = baseUrl;
        this.baseFilePath = baseFilePath;
        this.arrayList = arrayList;
        this.page = page;
    }

    @Override
    public void run() {
        for (Map.Entry<String, String> entry : arrayList) {
            String url = entry.getKey();
            String chapter = entry.getValue();
            List<String> contentList = ContentRepoPage.build(baseUrl + url, 1);
            FileUtils.batchAppendLine(contentList, baseFilePath + (page + 1) + File.separator + chapter + ".txt");
        }
    }
}
