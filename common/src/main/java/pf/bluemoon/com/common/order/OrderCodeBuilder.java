package pf.bluemoon.com.common.order;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author chaoyou
 * @Date Create in 11:48 2022/11/17
 * @Modified by
 * @Version 1.0.0
 * @Description 订单号生成器
 */
public class OrderCodeBuilder {
    private static final long subTime = 28800000L;
    private volatile static int sortNum = 0;
    private volatile static long delCurrentTimeMillis = 0;

    /**
     * 订单号 = 业务号(3位) + 城市号(3位) + 毫秒时间戳(8位) + 有序数字(4位)
     *
     * @param businessCode 业务号
     * @param cityCode 城市号
     * @param currentTimeMillis 毫秒时间戳
     * @return
     */
    public static long getOrderNo(long businessCode, long cityCode, long currentTimeMillis){
        return businessCode + cityCode + getCurrentTimeMillisWithToday(currentTimeMillis) * 10000 + getSortNum();
    }

    public static long getCurrentTimeMillisAndSortNum(long currentTimeMillis){
        return getCurrentTimeMillisWithToday(currentTimeMillis) * 10000 + getSortNum();
    }


    public static long getCurrentTimeMillisWithToday(long currentTimeMillis){
        return (currentTimeMillis + subTime) % (60 * 60 * 24 * 1000);
    }

    public static int getSortNum(){
        try {
            sortNum++;
        } catch (Exception e){
            sortNum = 0;
        }
        return sortNum % 10000;
    }
}
