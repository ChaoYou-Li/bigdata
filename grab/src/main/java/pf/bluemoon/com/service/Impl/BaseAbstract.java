package pf.bluemoon.com.service.Impl;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import pf.bluemoon.com.anno.DBEntity;
import pf.bluemoon.com.anno.PrimryKey;
import pf.bluemoon.com.cache.FileDDLCache;
import pf.bluemoon.com.cache.FileIndexCache;
import pf.bluemoon.com.cache.IndexCache;
import pf.bluemoon.com.config.InitTable;
import pf.bluemoon.com.entity.vo.FileIndexVO;
import pf.bluemoon.com.entity.vo.TableDdlVO;
import pf.bluemoon.com.entity.vo.WriteVO;
import pf.bluemoon.com.utils.SnowflakeUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-16 11:51
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public abstract class BaseAbstract<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseAbstract.class);

//    /**
//     * 读取文件中指定位置的内容
//     *
//     * @param filePath 文件路径
//     * @param position 要读取位置的偏移量
//     * @param length 读取内容的字节长度
//     * @return
//     */
//    T readByte(String filePath, int position, int length){
//        // 读取文件中指定位置的内容
//        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
//            // 设置文件指针位置
//            raf.seek(position);
//            // 初始化一个指定字节长度的缓冲区
//            byte[] buffer = new byte[length];
//            // 读取文件数据到缓冲区
//            raf.read(buffer);
//            String content = new String(buffer);
//            logger.info("读数据成功：[ filePath={}, position={}, length={}, content={} ]", filePath, position, length, content);
//            return JSONObject.parseObject(content, getGenericsClass());
//        } catch (IOException e) {
//            logger.error("读数据失败：[ filePath={}, position={}, length={}, reason={} ]", filePath, position, length, e);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }
//
//    /**
//     * 追加数据到文件末尾位置
//     *
//     * @param content 要写入的数据
//     * @return
//     */
//    WriteVO appendByte(T content) throws ClassNotFoundException {
//        WriteVO writeVO;
//        // 写入文件指定位置的内容
//        try (RandomAccessFile raf = new RandomAccessFile(getFilePath(), "rw")) {
//            // 获取当前文件的最后位置偏移量
//            FileIndexVO read = FileIndexCache.read(getFilePath());
//            if (null == read){
//                throw new RuntimeException("[ filePath=" + getFilePath() + ", reason=找不到文件位置偏移量 ]");
//            }
//            // 设置文件指针位置
//            raf.seek(read.getIndex());
//            // 写入新的内容
//            String str = JSONObject.toJSONString(content);
//            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
//            raf.write(bytes);
//            // 记录当前文件的最后位置偏移量
//            read.setStep(bytes.length);
//            int position = read.getIndex();
//            Boolean write = FileIndexCache.write(read);
//            if (!write){
//                throw new RuntimeException("[ filePath=" + getFilePath() + ", reason=无法保存文件位置偏移量缓存 ]");
//            }
//            logger.info("写数据成功：[ filePath={}, position={}, length={}, content={} ]",
//                    getFilePath(),
//                    position,
//                    bytes.length,
//                    content);
//            writeVO = new WriteVO(position, bytes.length, str.hashCode());
//        }catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return writeVO;
//    }
//
//    /**
//     * 更新文件中指定位置偏移量的数据
//     *
//     * @param filePath
//     * @param newContent
//     * @param writeVO
//     * @return
//     */
//    @Deprecated
//    Boolean updateByte(String filePath, String newContent, WriteVO writeVO){
//        // 写入文件指定位置的内容
//        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
//            // 校验新旧数据是否一致（校验码）
//            T oldData = readByte(filePath, writeVO.getPosition(), writeVO.getLength());
//            if (oldData.hashCode() != writeVO.getHashCode()){
//                // 校验不通过，直接放弃执行更新操作
//                return false;
//            }
//
//            /**
//             * 校验通过，执行更新逻辑：
//             *      1、把当前要更新的位置偏移量+旧数据长度作为新偏移量到文件末尾的数据读取出来
//             *      2、把要更新的数据拼接到 2 结果数据的头部
//             *      3、把 3 结果数据写入到要更新位置偏移量的后面
//             *      4、重新保存文件的尾部索引
//             */
//            FileIndexVO read = FileIndexCache.read(filePath);
//            if (null == read){
//                logger.error("写数据失败：[ filePath={}, reason={} ]", filePath, "找不到文件位置偏移量！！！");
//                return false;
//            }
//            T suffix = readByte(filePath, writeVO.getPosition() + writeVO.getLength(), read.getIndex());
//            String newData = newContent + suffix;
//
//            // 设置文件指针位置
//            raf.seek(writeVO.getPosition());
//            // 写入新的内容
//            byte[] bytes = newData.getBytes(StandardCharsets.UTF_8);
//            raf.write(bytes);
//            // 记录当前文件的最后位置偏移量
//            read.setStep(bytes.length);
//            Boolean write = FileIndexCache.write(read);
//            if (!write){
//                logger.error("写数据失败：[ filePath={}, reason={} ]", filePath, "无法保存文件位置偏移量缓存！！！");
//            }
//            logger.info("写数据成功：[ filePath={}, position={}, length={}, content={} ]",
//                    filePath,
//                    writeVO.getPosition(),
//                    newContent.getBytes(StandardCharsets.UTF_8).length,
//                    newContent);
//            return true;
//        } catch (IOException e) {
//            logger.error("写数据失败：[ filePath={}, reason={} ]", filePath, e);
//        }
//        return false;
//    }
//
//    /**
//     * 读取文件中指定位置的内容
//     *
//     * @param position 要读取位置的行号
//     * @return
//     */
//    T readLine(int position) throws ClassNotFoundException {
//        // 读取指定行数的内容
//        try (BufferedReader br = new BufferedReader(new FileReader(getFilePath()))) {
//            String line;
//            int currentLine = 1;
//            while ((line = br.readLine()) != null) {
//                if (currentLine == position) {
//                    break;
//                }
//                currentLine++;
//            }
//            logger.info("读数据成功：[ filePath={}, position={}, content={} ]", getFilePath(), position, line);
//            return (T) JSONObject.parseObject(line);
//        } catch (IOException e) {
//            logger.error("读数据失败：[ filePath={}, position={}, reason={} ]", getFilePath(), position, e);
//        }
//        return null;
//    }
//
//    WriteVO appendLine(T content) throws ClassNotFoundException {
//        WriteVO writeVO = null;
//        // 写入指定行数的内容
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter(getFilePath(), true))) {
//            // 获取当前文件的最后位置偏移量
//            FileIndexVO read = FileIndexCache.read(getFilePath());
//            if (null == read){
//                throw new RuntimeException("[ filePath=" + getFilePath() + ", reason=找不到文件位置偏移量 ]");
//            }
//
//            // 写入新的内容
//            bw.append(JSONObject.toJSONString(content));
//
//            // 记录当前文件的最后位置偏移量
//            read.setStep(1);
//            Boolean write = FileIndexCache.write(read);
//            if (!write){
//                throw new RuntimeException("[ filePath=" + getFilePath() + ", reason=无法保存文件位置偏移量缓存 ]");
//            }
//            logger.info("写数据成功：[ filePath={}, position={}, content={} ]", getFilePath(), read.getIndex(), content);
//            writeVO = new WriteVO(read.getIndex(), 1, content.hashCode());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return writeVO;
//    }
//
//    /**
//     * 更新文件中指定行号的数据：
//     *      1、先校验旧数据与当前缓存中的元数据是否一致
//     *      2、执行更新指定行号的数据
//     *      3、更新缓存中存储该文件的元数据
//     *
//     * @param filePath 文件路径
//     * @param newContent 要更新的数据
//     * @param writeVO 旧数据的元数据
//     * @return
//     */
//    Boolean updateLine(String filePath, String newContent, WriteVO writeVO) throws ClassNotFoundException {
//        // 写入文件指定位置的内容
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
//            // 校验新旧数据是否一致（校验码）
//            T oldData = readLine(writeVO.getPosition());
//            if (oldData.hashCode() != writeVO.getHashCode()){
//                // 校验不通过，直接放弃执行更新操作
//                return false;
//            }
//
//            int lineNumber = writeVO.getPosition();
//            for (int i = 1; i <= lineNumber; i++) {
//                if (i == lineNumber) {
//                    bw.write(newContent);
//                    bw.newLine();
//                } else {
//                    bw.newLine();
//                }
//            }
//
//            logger.info("写数据成功：[ filePath={}, position={}, length={}, content={} ]",
//                    filePath,
//                    writeVO.getPosition(),
//                    1,
//                    newContent);
//            return true;
//        } catch (IOException e) {
//            logger.error("写数据失败：[ filePath={}, reason={} ]", filePath, e);
//        }
//        return false;
//    }

    String getTableName() throws ClassNotFoundException {
        Class<T> clazz = getGenericsClass();
        DBEntity annotation = clazz.getAnnotation(DBEntity.class);
        String name = annotation.name();
        return null == name ? clazz.getName() : name;
    }

    Class<T> getGenericsClass() throws ClassNotFoundException {
        ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        return (Class<T>) typeArgument;
    }

    Object getPrimryKey(T t) throws IllegalAccessException {
        Class<?> clazz = t.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            PrimryKey primryKey = field.getAnnotation(PrimryKey.class);
            if (null != primryKey){
                field.setAccessible(true);
                return field.get(t);
            }
        }
        return null;
    }

    void initPrimryKey(T t) throws IllegalAccessException {
        Class<?> clazz = t.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            PrimryKey primryKey = field.getAnnotation(PrimryKey.class);
            if (null != primryKey){
                field.setAccessible(true);
                field.set(t, SnowflakeUtils.getId());
                break;
            }
        }
    }

    Object getValueByField(String field, T t) throws IllegalAccessException {
        if (null == field || null == t){
            return null;
        }
        Class<?> clazz = t.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(field)){
                f.setAccessible(true);
                return f.get(t);
            }
        }
        return null;
    }

    String getIdxFilePath(Object idxType) throws ClassNotFoundException {
        String tableName = getTableName();
        return InitTable.getBasePath() + File.separator +
                tableName + File.separator +
                InitTable.getFileName(idxType + "");
    }

    String getFilePath() {
        try {
            String tableName = getTableName();
            return InitTable.getBasePath() + File.separator +
                    tableName + File.separator +
                    InitTable.getFileName(InitTable.TABLE_DATA);
        } catch (ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }
}
