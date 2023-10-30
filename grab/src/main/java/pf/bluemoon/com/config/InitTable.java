package pf.bluemoon.com.config;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import pf.bluemoon.com.GrapApplication;
import pf.bluemoon.com.anno.*;
import pf.bluemoon.com.cache.FileDDLCache;
import pf.bluemoon.com.cache.IndexCache;
import pf.bluemoon.com.entity.vo.TableDdlVO;

import javax.annotation.PostConstruct;
import javax.imageio.IIOException;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-17 15:16
 * @Modified by
 * @Version 1.0.0
 * @Description 项目启动初始化所有数据表的存储文件
 */
@DependsOn("springBeanUtils")
@Configuration
public class InitTable {

    private static final Logger logger = LoggerFactory.getLogger(InitTable.class);

    public static final String FILE_OFFSET = "offset";
    // 获取当前项目根目录路径
    public static final String CUR_DIR = System.getProperty("user.dir");
    private static Environment environment;
    public static final String TABLE_DATA = "data";
    public static final String PRIMRY_KEY = "primry_key_idx";
    public static final String FILE_TEXT = "text_idx";
    public static final String UNIQUE_KEY = "unique_key_idx";
    public static final String FOREIGN_KEY = "foreign_key_idx";
    @Value("${grab.server.file.extension}")
    private String extension;
    @Value("${grab.server.database.base.package}")
    private String basePackage;

    @Autowired
    public void setEnvironment(Environment environment) {
        InitTable.environment = environment;
    }


//    @PostConstruct
    public void init() {
        logger.info("------------------------- 开始执行数据表初始化逻辑 ------------------------------");
        // 1、拼接初始化目录
        String basePath = CUR_DIR + File.separator + basePackage;

        // 2、初始化所有数据表的结构关系缓存
        initDDL();

        // 3、初始化文件尾部位置缓存文件（FileIndexCache）
        createFile(basePath, FILE_OFFSET + extension);

        // 4、初始化所有数据表的数据存储、索引逻辑文件
        List<String> entityList = InitCache.getTableNameList();
        for (String table : entityList) {
            // 创建数据存储文件
            createFile(basePath + File.separator + table, TABLE_DATA + extension);
            // 创建索引逻辑文件
            createFile(basePath + File.separator + table, PRIMRY_KEY + extension);
            /**
             * 获取数据表结构关系
             */
            TableDdlVO tableDdlVO = FileDDLCache.getTable(table);
            if (null == tableDdlVO){
                continue;
            }
            if (null != tableDdlVO.getIndexList() && tableDdlVO.getIndexList().size() > 0){
                // 创建普通搜索索引
                createFile(basePath + File.separator + table, FILE_TEXT + extension);
            }
            if (null != tableDdlVO.getUniKeyMapFieldList() && tableDdlVO.getUniKeyMapFieldList().size() > 0){
                // 创建唯一索引
                createFile(basePath + File.separator + table, UNIQUE_KEY + extension);
            }
            if (null != tableDdlVO.getForeignKeyMapField() && tableDdlVO.getForeignKeyMapField().size() > 0){
                // 创建外键索引
                createFile(basePath + File.separator + table, FOREIGN_KEY + extension);
            }
        }
        logger.info("------------------------- 完成执行数据表初始化逻辑 ------------------------------");
    }

    private boolean initDDL(){
        List<Class<?>> entityList = InitCache.getEntityList();
        for (Class<?> entity : entityList) {
            String name = entity.getAnnotation(DBEntity.class).name();
            String tableName = name == null ? entity.getSimpleName().toLowerCase() : name;
            TableDdlVO tableDdlVO = new TableDdlVO(tableName);
            Map<String, String> foreignKeyMapField = new HashMap<>();
            Map<String, List<String>> uniKeyMapFieldList = new HashMap<>();
            List<String> indexFieldList = new ArrayList<>();
            for (Field field : entity.getDeclaredFields()) {
                // 查找主键
                PrimryKey primryKey = field.getAnnotation(PrimryKey.class);
                if (null != primryKey){
                    tableDdlVO.setPrimryKey(field.getName());
                }

                // 查找外键
                ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
                if (null != foreignKey){
                    String fieldName = foreignKeyMapField.get(foreignKey.name());
                    if (null != fieldName){
                        throw new RuntimeException("存在重复的外键名称：" + fieldName);
                    }
                    foreignKeyMapField.put(foreignKey.name(), field.getName());
                }

                // 查找唯一索引
                UniqueKey uniqueKey = field.getAnnotation(UniqueKey.class);
                if (null != uniqueKey){
                    if (null == uniqueKey.name()){
                        throw new RuntimeException("唯一键的名称不能为空");
                    }
                    List<String> fieldList = uniKeyMapFieldList.get(uniqueKey.name());
                    if (null == fieldList){
                        fieldList = new ArrayList<>();
                    }
                    fieldList.add(field.getName());
                    uniKeyMapFieldList.put(uniqueKey.name(), fieldList);
                }

                // 查找普通索引
                Index index = field.getAnnotation(Index.class);
                if (null != index){
                    indexFieldList.add(field.getName());
                }
            }
            tableDdlVO.setForeignKeyMapField(foreignKeyMapField);
            tableDdlVO.setIndexList(indexFieldList);
            tableDdlVO.setUniKeyMapFieldList(uniKeyMapFieldList);
            if (null == tableDdlVO.getPrimryKey()){
                throw new RuntimeException("数据表 [ " + tableName + " ] 找不到主键字段");
            }
            logger.info("数据表：{}，DDL：{}", tableName, JSONObject.toJSONString(tableDdlVO));
            FileDDLCache.getTable(tableName);
        }
        return false;
    }

    public static boolean createFile(String dir, String fileName) {
        if (null == dir){
            return false;
        }
        File folder = new File(dir);

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                throw new RuntimeException("文件创建失败：" + dir + File.separator + fileName);
            }
        }

        if (null == fileName){
            return false;
        }
        String filePath = dir + File.separator + fileName; // 构建文件路径
        File file = new File(filePath);
        try {
            if (!file.exists()){
                if (file.createNewFile()) {
                    logger.info("文件创建成功：{}", file.getPath());
                    return true;
                } else {
                    throw new RuntimeException("文件创建失败：" + file.getPath());
                }
            } else {
//                logger.info("文件已存在：{}", file.getPath());
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException("文件创建失败：" + file.getPath());
        }
    }

    public static String getBasePath(){
        return CUR_DIR + File.separator + environment.getProperty("grab.server.database.base.package");
    }

    public static String getFileName(String fileName){
        return fileName + environment.getProperty("grab.server.file.extension");
    }
}
