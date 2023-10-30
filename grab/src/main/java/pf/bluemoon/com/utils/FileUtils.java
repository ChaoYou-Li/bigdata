package pf.bluemoon.com.utils;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pf.bluemoon.com.cache.FileIndexCache;
import pf.bluemoon.com.entity.vo.FileIndexVO;
import pf.bluemoon.com.entity.vo.WriteVO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-17 12:41
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 读取文件中指定位置的内容
     *
     * @param filePath 文件路径
     * @param position 要读取位置的偏移量
     * @param length 读取内容的字节长度
     * @return
     */
    public  static String readByte(String filePath, long position, int length){
        // 读取文件中指定位置的内容
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // 设置文件指针位置
            raf.seek(position);
            // 初始化一个指定字节长度的缓冲区
            byte[] buffer = new byte[length];
            // 读取文件数据到缓冲区
            raf.read(buffer);
            String content = new String(buffer);
            logger.info("读数据成功：[ filePath={}, position={}, length={}, content={} ]", filePath, position, length, content);
            return content;
        } catch (IOException e) {
            logger.error("读数据失败：[ filePath={}, position={}, length={}, reason={} ]", filePath, position, length, e);
        }
        return null;
    }

    /**
     * 追加数据到文件末尾位置
     *
     * @param content 要写入的数据
     * @return
     */
    public static WriteVO appendByte(Object content, String filePath) {
        if (null == content || null == filePath){
            return null;
        }
        WriteVO writeVO;
        // 写入文件指定位置的内容
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // 获取当前文件的最后位置偏移量
            long position = raf.length();
            // 设置文件指针位置
            raf.seek(position);
            // 写入新的内容
            String str = obj2Str(content);
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            raf.write(bytes);
            logger.info("写数据成功：[ filePath={}, position={}, length={}, content={} ]",
                    filePath,
                    position,
                    bytes.length,
                    content);
            writeVO = new WriteVO(position, bytes.length, str.hashCode());
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        return writeVO;
    }

    public static <T> Map<T, WriteVO> batchAppendByte(List<T> contents, String filePath) {
        if (null == contents || contents.isEmpty() || null == filePath){
            return null;
        }
        Map<T, WriteVO> writeVOMap = new HashMap<>();
        // 写入文件指定位置的内容
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // 获取当前文件的最后位置偏移量
            long position = raf.length();
            // 设置文件指针位置
            raf.seek(position);
            // 写入新的内容
            StringBuffer buffer = new StringBuffer();
            long len = 0;
            for (T content : contents) {
                String str = obj2Str(content);
                buffer.append(str);
                WriteVO writeVO = new WriteVO(position + len, str.getBytes(StandardCharsets.UTF_8).length, str.hashCode());
                writeVOMap.put(content, writeVO);
                len = len + writeVO.getLength();
            }
            byte[] bytes = buffer.toString().getBytes(StandardCharsets.UTF_8);
            raf.write(bytes);
            logger.info("写数据成功：[ filePath={}, position={}, length={}, content={} ]",
                    filePath,
                    position,
                    bytes.length,
                    buffer);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        return writeVOMap;
    }

    /**
     * 读取文件中指定位置的内容
     *
     * @param position 要读取位置的行号
     * @return
     */
    public static String readLine(long position, String filePath) throws ClassNotFoundException {
        // 读取指定行数的内容
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int currentLine = 0;
            while ((line = br.readLine()) != null) {
                if (currentLine == position) {
                    break;
                }
                currentLine++;
            }
            logger.info("读数据成功：[ filePath={}, position={}, content={} ]", filePath, position, line);
            return line;
        } catch (IOException e) {
            logger.error("读数据失败：[ filePath={}, position={}, reason={} ]", filePath, position, e);
        }
        return null;
    }

    public static WriteVO appendLine(Object content, String filePath) {
        WriteVO writeVO = null;
        // 写入指定行数的内容
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            // 获取当前文件的最后位置偏移量
            FileIndexVO read = FileIndexCache.read(filePath);
            if (null == read){
                throw new RuntimeException("[ filePath=" + filePath + ", reason=找不到文件位置偏移量 ]");
            }

            // 写入新的内容
            bw.append(content.toString());
            bw.newLine();
            bw.newLine();

            // 记录当前文件的最后位置偏移量
            read.setStep(1);
            Boolean write = FileIndexCache.write(read);
            if (!write){
                throw new RuntimeException("[ filePath=" + filePath + ", reason=无法保存文件位置偏移量缓存 ]");
            }
            logger.info("写数据成功：[ filePath={}, position={}, content={} ]", filePath, read.getIndex(), content);
            writeVO = new WriteVO(read.getIndex(), 1, content.hashCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return writeVO;
    }

    /**
     * 更新文件中指定行号的数据：
     *      1、先校验旧数据与当前缓存中的元数据是否一致
     *      2、执行更新指定行号的数据
     *      3、更新缓存中存储该文件的元数据
     *
     * @param filePath 文件路径
     * @param newContent 要更新的数据
     * @param writeVO 旧数据的元数据
     * @return
     */
    public static Boolean updateLine(String filePath, String newContent, WriteVO writeVO) throws ClassNotFoundException {
        // 写入文件指定位置的内容
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            // 校验新旧数据是否一致（校验码）
            String oldData = readLine(writeVO.getPosition(), filePath);
            if (oldData.hashCode() != writeVO.getHashCode()){
                // 校验不通过，直接放弃执行更新操作
                return false;
            }

            long lineNumber = writeVO.getPosition();
            for (int i = 1; i <= lineNumber; i++) {
                if (i == lineNumber) {
                    bw.write(newContent);
                    bw.newLine();
                } else {
                    bw.newLine();
                }
            }

            logger.info("写数据成功：[ filePath={}, position={}, length={}, content={} ]",
                    filePath,
                    writeVO.getPosition(),
                    1,
                    newContent);
            return true;
        } catch (IOException e) {
            logger.error("写数据失败：[ filePath={}, reason={} ]", filePath, e);
        }
        return false;
    }

    /**
     * 对象类型转字符串类型（注意必须是实现类 Serializable 的对象才能正常转换）
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static String obj2Str(Object obj) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
        } catch (IOException e){
            throw new RuntimeException(e);
        } finally {
            if (null != oos){
                try {
                    oos.flush();
                    oos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (null != bos){
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bos.toString();
    }

    public static <T> Map<T, WriteVO> batchAppendLine(List<T> data, String filePath) {
        if (null == data || null == filePath){
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()){
            File parentFile = file.getParentFile();
            if (!parentFile.exists()){
                parentFile.mkdir();
            }
        }
        // 写入指定行数的内容
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            // 写入新的内容
            for (T datum : data) {
                bw.append(datum.toString());
                bw.newLine();
                bw.newLine();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
