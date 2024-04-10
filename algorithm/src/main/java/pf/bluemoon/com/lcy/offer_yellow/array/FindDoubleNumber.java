package pf.bluemoon.com.lcy.offer_yellow.array;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author chaoyou
 * @Date Create in 2023-11-23 16:17
 * @Modified by
 * @Version 1.0.0
 * @Description 1346. 检查整数及其两倍数是否存在
 */
public class FindDoubleNumber {
    /**
     * 给你一个整数数组 arr，请你检查是否存在两个整数 N 和 M，满足 N 是 M 的两倍（即，N = 2 * M）。
     *
     * 更正式地，检查是否存在两个下标 i 和 j 满足：
     *                                      i != j
     *                                      0 <= i, j < arr.length
     *                                      arr[i] == 2 * arr[j]
     *
     *
     *
     * 示例 1：
     *      输入：arr = [10,2,5,3]
     *      输出：true
     *      解释：N = 10 是 M = 5 的两倍，即 10 = 2 * 5 。
     *
     * 示例 3：
     *      输入：arr = [3,1,7,11]
     *      输出：false
     *      解释：在该情况下不存在 N 和 M 满足 N = 2 * M 。
     *
     *
     *
     * 提示：
     *      2 <= arr.length <= 500
     *      -10^3 <= arr[i] <= 10^3
     */


    /**
     * 暴力解题
     *
     * @param arr
     * @return
     */
    public boolean checkIfExist1(int[] arr) {
        for (int i = 0; i<arr.length-1; i++) {
            for (int j=i+1; j<arr.length; j++){
                if (2*arr[i] == arr[j] || arr[i] == 2*arr[j]){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 哈希表解题
     *
     * @param arr
     * @return
     */
    public boolean checkIfExist2(int[] arr) {
        Set<Integer> set = new HashSet<>();
        for(int i : arr){
            if (set.contains(i) || set.contains(4*i)){
                return true;
            }
            set.add(i*2);
        }
        return false;
    }


    /**
     * 排序 + 二分查找
     *
     * @param arr
     * @return
     */
    public boolean checkIfExist3(int[] arr) {
        // 排序
        Arrays.sort(arr);

        // 二分查找
        int left, right, mid;
        for (int i = 0; i < arr.length-1; i++) {
            left = 0;
            right = arr.length - 1;
            while (left <= right){
                mid = left + (right - left) / 2;
                if (arr[mid] == 2* arr[i] && i != mid){
                    return true;
                } else if (arr[mid] > 2*arr[i]){
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            }
        }
        return false;
    }

    private boolean banarySearch(int[] arr, int findValue){
        int left = 0;
        int right = arr.length - 1;
        while (left <= right){
            int mid = left + (right - left) / 2;
            if (2*arr[mid] == findValue){
                return true;
            } else if (arr[mid] == 2*findValue) {
                return true;
            } else if (arr[mid] > findValue){
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return false;
    }
}
