package pf.bluemoon.com.hadoop.utils;

/**
 * @Author chaoyou
 * @Date Create in 9:43 2022/8/9
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class NumberUtil {

    public static boolean isNumber(String str){
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }
}
