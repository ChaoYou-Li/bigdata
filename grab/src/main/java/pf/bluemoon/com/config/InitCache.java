package pf.bluemoon.com.config;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.core.io.Resource;
import pf.bluemoon.com.anno.*;
import pf.bluemoon.com.cache.FileDDLCache;
import pf.bluemoon.com.cache.FileIndexCache;
import pf.bluemoon.com.cache.IndexCache;
import pf.bluemoon.com.entity.vo.*;
import pf.bluemoon.com.utils.ReflectUtils;
import pf.bluemoon.com.utils.SpringBeanUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-16 14:58
 * @Modified by
 * @Version 1.0.0
 * @Description 项目启动就初始化索引缓存数据
 */
@DependsOn({"initTable"})
@Configuration
public class InitCache {
    @Autowired
    private IndexCache indexCache;

    private static final Logger logger = LoggerFactory.getLogger(InitCache.class);

//    @PostConstruct
    public void init() throws FileNotFoundException {
        logger.info("------------------------- 开始执行索引元数据初始化逻辑 ------------------------------");
        Map<String,Object> propertiesMap = new HashMap<String,Object>();
        List<String> tableNameList = getTableNameList();
        for (String tableName : tableNameList) {
            Map<String, Object> indexMap = createTableCacheDDL(tableName);
            if (indexMap == null){
                continue;
            }
            propertiesMap.put(tableName, indexMap);
        }

        // 通过反射机制动态构建索引缓存层
        IndexCache.indexCache = (IndexCache) ReflectUtils.getObject(indexCache, propertiesMap);

        // 把索引文件中的数据初始化到缓存层
        startIdxCache(tableNameList);

        // 把文件末尾偏移量数据初始化到缓存层
        startOffsetCache();

        logger.info("------------------------- 完成执行索引元数据初始化逻辑 ------------------------------");
    }

    /**
     * 创建数据表的缓存结构
     *
     * @param tableName
     * @return
     */
    public static Map<String, Object> createTableCacheDDL(String tableName) {
        TableDdlVO tableDdlVO = FileDDLCache.getTable(tableName);
        if (null == tableDdlVO){
            return null;
        }
        Map<String, Object> indexMap = new HashMap<>();

        // 创建主键索引缓存
        indexMap.put(InitTable.PRIMRY_KEY, new HashMap<String, WriteVO>(1));

        // 创建唯一索引缓存
        if (null != tableDdlVO.getUniKeyMapFieldList() && tableDdlVO.getUniKeyMapFieldList().size() > 0){
            Map<String, Map<String, List<String>>> unKeyMapValueMapWrite = new HashMap<>(tableDdlVO.getUniKeyMapFieldList().size());
            for (String key : tableDdlVO.getUniKeyMapFieldList().keySet()) {
                unKeyMapValueMapWrite.put(key, new HashMap<>(1));
            }
            indexMap.put(InitTable.UNIQUE_KEY, unKeyMapValueMapWrite);
        }

        // 创建外键索引缓存
        if (null != tableDdlVO.getForeignKeyMapField() && tableDdlVO.getForeignKeyMapField().size() > 0){
            HashMap<String, Map<String, List<String>>> fkKeyMapValueMapWrite = new HashMap<>(tableDdlVO.getForeignKeyMapField().size());
            for (String key : tableDdlVO.getForeignKeyMapField().keySet()) {
                fkKeyMapValueMapWrite.put(key, new HashMap<>(1));
            }
            indexMap.put(InitTable.FOREIGN_KEY, fkKeyMapValueMapWrite);
        }

        // 创建普通搜索索引缓存
        if (null != tableDdlVO.getIndexList() && tableDdlVO.getIndexList().size() > 0){
            HashMap<String, Map<String, List<String>>> ftKeyMapValueMapWrite = new HashMap<>(tableDdlVO.getIndexList().size());
            for (String key : tableDdlVO.getIndexList()) {
                ftKeyMapValueMapWrite.put(key, new HashMap<>(1));
            }
            indexMap.put(InitTable.FILE_TEXT, ftKeyMapValueMapWrite);
        }
        return indexMap;
    }

    /**
     * 项目启动时，重新把索引文件中的数据同步到缓存层
     */
    private void startIdxCache(List<String> tableNameList) throws FileNotFoundException {
        String basePath = InitTable.getBasePath();
        for (String table : tableNameList) {
            File tableFile = new File(basePath + File.separator + table);
            if (tableFile.exists() && tableFile.isDirectory()){
                TableDdlVO tableDdlVO = FileDDLCache.getTable(table);
                if (null == tableDdlVO){
                    continue;
                }

                // 初始化主键索引数据到缓存层
                setCache(tableFile.getPath(), InitTable.PRIMRY_KEY, table);

                // 初始化外键索引数据到缓存层
                setCache(tableFile.getPath(), InitTable.FOREIGN_KEY, table);

                // 初始化普通检索索引数据到缓存层
                setCache(tableFile.getPath(), InitTable.FILE_TEXT, table);

                // 初始化唯一检索索引数据到缓存层
                setCache(tableFile.getPath(), InitTable.UNIQUE_KEY, table);
            }
            logger.info("数据表：{}", table);
        }
    }

    private void setCache(String tablePath, String idxType, String table) throws FileNotFoundException {
        File file = new File(tablePath + File.separator + InitTable.getFileName(idxType));
        if (file.exists() && file.isFile()){
            BufferedReader reader = new BufferedReader(new FileReader(file.getPath()));
            if (InitTable.PRIMRY_KEY.equals(idxType)){
                reader.lines().forEach(line -> {
                    PrimryKeyVO primryKeyVO = JSONObject.parseObject(line, PrimryKeyVO.class);
                    IndexCache.indexCache.setPkIdxMsg(table, primryKeyVO);
                });
            } else if (InitTable.UNIQUE_KEY.equals(idxType)){
                reader.lines().forEach(line -> {
                    SingleKeyVO singleKeyVO = JSONObject.parseObject(line, SingleKeyVO.class);
                    IndexCache.indexCache.setUkIdxMsg(table, singleKeyVO);
                });
            } else if (InitTable.FOREIGN_KEY.equals(idxType)){
                reader.lines().forEach(line -> {
                    GroupKeyVO groupKeyVO = JSONObject.parseObject(line, GroupKeyVO.class);
                    IndexCache.indexCache.setFkIdxMsg(table, groupKeyVO);
                });
            } else if (InitTable.FILE_TEXT.equals(idxType)){
                reader.lines().forEach(line -> {
                    GroupKeyVO groupKeyVO = JSONObject.parseObject(line, GroupKeyVO.class);
                    IndexCache.indexCache.setFtIdxMsg(table, groupKeyVO);
                });
            }
        }
    }


    private void startOffsetCache() throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(InitTable.getBasePath() + File.separator + InitTable.getFileName(InitTable.FILE_OFFSET)));
        reader.lines().forEach(line -> {
            FileIndexVO indexVO = JSONObject.parseObject(line, FileIndexVO.class);
            FileIndexCache.write(indexVO);
            logger.info("文件路径：{}，元数据：{}", indexVO.getFilePath(), JSONObject.toJSONString(indexVO));
        });
    }

    /**
     * 获取当前项目所有实体类的表名
     *
     * @return
     */
    public static List<Class<?>> getEntityList(){
        List<Class<?>> result = new ArrayList<>();
        Set<String> packages = SpringBeanUtils.getComponentScanningPackages();
        //spring工具类，可以获取指定路径下的全部类
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            for (String pkg : packages) {
                String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                        ClassUtils.convertClassNameToResourcePath(pkg) + "/**/*.class";
                Resource[] resources = resourcePatternResolver.getResources(pattern);
                //MetadataReader 的工厂类
                MetadataReaderFactory readerfactory = new CachingMetadataReaderFactory(resourcePatternResolver);
                for (Resource resource : resources) {
                    //用于读取类信息
                    MetadataReader reader = readerfactory.getMetadataReader(resource);
                    //扫描到的class
                    String classname = reader.getClassMetadata().getClassName();
                    Class<?> clazz = Class.forName(classname);
                    //判断是否有指定主解
                    DBEntity anno = clazz.getAnnotation(DBEntity.class);
                    if (anno != null) {
                        result.add(clazz);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取当前项目所有实体类的表名
     *
     * @return
     */
    public static List<String> getTableNameList(){
        List<String> result = new ArrayList<>();
        Set<String> packages = SpringBeanUtils.getComponentScanningPackages();
        //spring工具类，可以获取指定路径下的全部类
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            for (String pkg : packages) {
                String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                        ClassUtils.convertClassNameToResourcePath(pkg) + "/**/*.class";
                Resource[] resources = resourcePatternResolver.getResources(pattern);
                //MetadataReader 的工厂类
                MetadataReaderFactory readerfactory = new CachingMetadataReaderFactory(resourcePatternResolver);
                for (Resource resource : resources) {
                    //用于读取类信息
                    MetadataReader reader = readerfactory.getMetadataReader(resource);
                    //扫描到的class
                    String classname = reader.getClassMetadata().getClassName();
                    Class<?> clazz = Class.forName(classname);
                    //判断是否有指定主解
                    DBEntity anno = clazz.getAnnotation(DBEntity.class);
                    if (anno != null) {
                        //将注解中的类型值作为key，对应的类作为 value
                        String name = anno.name();
                        result.add(name == null ? classname : name);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }
}
