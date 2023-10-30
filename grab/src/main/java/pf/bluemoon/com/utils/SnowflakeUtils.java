package pf.bluemoon.com.utils;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-15 15:15
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class SnowflakeUtils {
    private static SnowflakeIdGenerator generator;
    static {
        if (null == generator){
            generator = new SnowflakeIdGenerator(3, 3);
        }
    }

    /**
     * 获取雪花算法生成的主键id
     *
     * @return
     */
    public static long getId(){
        return generator.generateId();
    }
}
