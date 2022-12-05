package pf.bluemoon.com.common.order;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author chaoyou
 * @Date Create in 11:05 2022/11/22
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class SafeSortNum {

    private static int sortNum = 0;

    private static int version = 0;

    public static int getSortNum() {
        return sortNum;
    }

    public static void setSortNum(int sortNum) {
        SafeSortNum.sortNum = sortNum;
    }

    public static int getVersion() {
        return version;
    }

    public static void setVersion(int version) {
        SafeSortNum.version = version;
    }
}
