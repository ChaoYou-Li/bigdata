package pf.bluemoon.com.config;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pf.bluemoon.com.anno.PrimryKey;
import pf.bluemoon.com.cache.FileDDLCache;
import pf.bluemoon.com.cache.FileIndexCache;
import pf.bluemoon.com.cache.IndexCache;
import pf.bluemoon.com.entity.vo.*;
import pf.bluemoon.com.utils.ReflectUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-18 13:44
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@DependsOn({"initCache"})
@Configuration
public class ScheduledTask {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);

    @Autowired
    private IndexCache indexCache;

//    @PostConstruct
    public void init() {
        logger.info("------------------------- 开始执行定时任务逻辑 ------------------------------");
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                saveIdxCache();
                saveFileIdxCache();
            }
        }, 0, 180, TimeUnit.SECONDS);
        logger.info("------------------------- 完成执行定时任务逻辑 ------------------------------");
    }

    public void saveIdxCache() {
        List<String> tableNameList = InitCache.getTableNameList();
        for (String tableName : tableNameList) {
            TableDdlVO tableDdlVO = FileDDLCache.getTable(tableName);
            if (null == tableDdlVO){
                continue;
            }

            // 持久化主键索引元数据
            Map<String, WriteVO> pkMapObj = indexCache.getCacheByPk(tableName);
            String basePkg = InitTable.getBasePath() + File.separator +
                    tableName;
            saveIdxCache(pkMapObj.entrySet().stream().map(e -> new PrimryKeyVO(e.getKey(), e.getValue())).collect(Collectors.toList()), basePkg, InitTable.PRIMRY_KEY);

            // 唯一键
            if (null != tableDdlVO.getUniKeyMapFieldList() && tableDdlVO.getUniKeyMapFieldList().size() > 0){
                Map<String, Map<String, WriteVO>> ukMapObj = indexCache.getCacheByUk(tableName);
                saveIdxCache(getObjList(ukMapObj, InitTable.UNIQUE_KEY), basePkg, InitTable.UNIQUE_KEY);
            }

            /**
             * 持久化外键、唯一、普通检索的索引元数据
             */
            // 外键
            if (null != tableDdlVO.getForeignKeyMapField() && tableDdlVO.getForeignKeyMapField().size() > 0){
                Map<String, Map<String, List<WriteVO>>> fkMapObj = indexCache.getCacheByfk(tableName);
                saveIdxCache(getObjList(fkMapObj, InitTable.FOREIGN_KEY), basePkg, InitTable.FOREIGN_KEY);
            }

            // 普通检索索引
            if (null != tableDdlVO.getIndexList() && tableDdlVO.getIndexList().size() > 0){
                Map<String, Map<String, List<WriteVO>>> ftMapObj = indexCache.getCacheByft(tableName);
                saveIdxCache(getObjList(ftMapObj, InitTable.FILE_TEXT), basePkg, InitTable.FILE_TEXT);
            }

        }
    }

    private List<Object> getObjList(Object obj, String idxType) {
        List<Object> result = new ArrayList<>();
        Map<String, Map<String, Object>> cache = (Map<String, Map<String, Object>>) obj;
        Iterator<Map.Entry<String, Map<String, Object>>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, Map<String, Object>> entry = iterator.next();
            Map<String, Object> keyValueMapObj = entry.getValue();
            if (InitTable.UNIQUE_KEY.equals(idxType)){
                result.addAll(keyValueMapObj.entrySet().stream().map(e ->
                        new SingleKeyVO(entry.getKey(), e.getKey(), (WriteVO) e.getValue())).collect(Collectors.toList()));
            } else {
                result.addAll(keyValueMapObj.entrySet().stream().map(e ->
                        new GroupKeyVO(entry.getKey(), e.getKey(), (List<WriteVO>) e.getValue())).collect(Collectors.toList()));
            }
        }
        return result;
    }

    public static void saveIdxCache(Collection<Object> objectList, String basePkg, String idxType) {
        if (null == objectList || objectList.isEmpty()){
            return;
        }
        logger.info("++++++++++++++++++++++++ 持久化索引缓存到磁盘 ++++++++++++++++++++++++++++");
        String idxTemp = InitTable.getFileName(idxType + "_cp");
        String idxName = InitTable.getFileName(idxType);
        // 创建一个副本文件
        InitTable.createFile(basePkg, idxTemp);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(basePkg + File.separator + idxTemp, true))) {
            // 把缓存中的数据持久化到副本文件中
            for (Object entry : objectList) {
                bw.append(JSONObject.toJSONString(entry));
                bw.newLine();
            }
        } catch (IOException e) {
            logger.error("定时持久化索引数据失败：[ filePath={}, reason={} ]", basePkg + File.separator + idxName, e);
        }
        // 删除原来的索引文件
        File old = new File(basePkg + File.separator + idxName);
        File cp = new File(basePkg + File.separator + idxTemp);
        if (old.delete()){
            // 把副本文件更名为原来索引文件名称
            cp.renameTo(old);
        } else {
            cp.delete();
        }
    }

    public void saveFileIdxCache() {
        String basePkg = InitTable.getBasePath();
        String idxTemp = InitTable.getFileName(InitTable.FILE_OFFSET + "_cp");
        String idxName = InitTable.getFileName(InitTable.FILE_OFFSET);

        // 创建一个副本文件
        InitTable.createFile(basePkg, idxTemp);

        // 把缓存中的数据持久化到副本文件中
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(basePkg + File.separator + idxTemp, true))) {
            for (FileIndexVO indexVO : FileIndexCache.getCache().values()) {
                bw.append(JSONObject.toJSONString(indexVO));
                bw.newLine();
            }
        } catch (IOException e) {
            logger.error("定时持久化索引数据失败：[ filePath={}, reason={} ]", basePkg + File.separator + idxName, e);
        }

        // 删除原来的索引文件
        File old = new File(basePkg + File.separator + idxName);
        File cp = new File(basePkg + File.separator + idxTemp);
        if (old.delete()){
            // 把副本文件更名为原来索引文件名称
            cp.renameTo(old);
        } else {
            cp.delete();
        }
    }
}
