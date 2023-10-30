package pf.bluemoon.com.cache;

import org.springframework.stereotype.Component;
import pf.bluemoon.com.entity.vo.FileIndexVO;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-15 14:55
 * @Modified by
 * @Version 1.0.0
 * @Description 文件索引缓存，用于保存每个文件尾部的位置偏移量/行数
 */
@Component
public class FileIndexCache {
    /**
     * key：文件路径
     * value：FileIndexVO对象
     */
    private static Map<String, FileIndexVO> cache;

    static {
        if (null == cache){
            cache = new HashMap<>(16);
        }
    }

    /**
     * 从缓存中读取文件索引数据
     *
     * @param filePath
     * @return
     */
    public static FileIndexVO read(String filePath){
        if (null == filePath){
            return null;
        }
        // 第一次读取不到文件索引数据，初始化一个数据
        FileIndexVO index = FileIndexCache.cache.get(filePath);
        if (null == index){
            index = new FileIndexVO(filePath,0);
        }
        return index;
    }

    public static Boolean write(FileIndexVO index){
        if (null == index){
            return false;
        }
        FileIndexVO fileIndexVO = FileIndexCache.cache.get(index.getFilePath());
        if (null == fileIndexVO){
            // 缓存中没有记录该文件的末尾索引数据，初始化一个数据
            fileIndexVO = index;
        } else {
            // 缓存中存在，检查校验码是否一致（乐观锁逻辑：不一致放弃执行写操作）
            if (index.getCheckCode() != fileIndexVO.getCheckCode()){
                return false;
            }
        }
        synchronized (index){
            // 修改步长
            fileIndexVO.setStep(index.getStep());
            // 重新计算偏移量
            fileIndexVO.increase();
            // 步长置零
            fileIndexVO.setStep(0);
        }
        cache.put(index.getFilePath(), fileIndexVO);
        return true;
    }

    public static Map<String, FileIndexVO> getCache(){
        return  FileIndexCache.cache;
    }
}
