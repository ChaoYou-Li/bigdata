package pf.bluemoon.com.lcy.offer_yellow.number;

/**
 * @Author chaoyou
 * @Date Create in 2023-11-05 16:17
 * @Modified by
 * @Version 1.0.0
 * @Description 1.1 整数的基本知识
 */
public class IntegerNumber {
    /**
     * 整数是一种基本的数据类型。编程语言可能会提供占据不同内存
     * 空间的整数类型，每种类型能表示的整数的范围也不相同。例如，
     * Java中有4种不同的整数类型，分别为8位的byte（-27～27-1）、16位
     * 的short（-215～215-1）、32位的int（-231～231-1）和64位的
     * long（-263～263-1）。
     *
     * Java中的整数类型都是有符号整数，即如果整数的二进制表示的
     * 最高位为0则表示其为正数，如果整数的二进制表示的最高位为1则表
     * 示其为负数。有些语言（如C/C++）支持无符号整数。无符号整数无论
     * 二进制表示的最高位是0还是1，都表示其为一个正数。无符号的32位
     * 整数的范围是0～232-1。
     *
     * 通常，编程语言中的整数运算都遵循四则运算规则（加、减、乘、除），可以使用任
     * 意嵌套的小括号。需要注意的是，由于整数的范围限制，如果计算结
     * 果超出了范围就会产生溢出。产生溢出时运行不会出错，但结果可能
     * 会出乎意料。如果除数为0，那么整数的除法在运行时将报错。
     */

    /**
     * 面试题1：整数除法
     *
     * 题目：
     *      输入2个int型整数，它们进行除法计算并返回商，要求
     *      不得使用乘号'*'、除号'/'及求余符号'%'。当发生溢出时，返回最
     *      大的整数值。假设除数不为0。例如，输入15和2，输出15/2的结
     *      果，即7。
     *
     * 分析：
     *      1、输入两个 int 类型参数
     *      2、进行除法计算返回商值（要求：不能使用 “*”，“/”，“%” 运算符），用减法/位移完成计算
     *      3、计算结果发生溢出（int 类型存不下）时，返回整数的最大值。
     */


    /**
     * 使用位移计算法
     *
     * @param dividend
     * @param divisor
     * @return
     */
    public static int display(int dividend, int divisor){

        // 3、计算结果发生溢出（int 类型存不下）时，返回整数的最大值。
        if (dividend == Integer.MIN_VALUE && divisor == -1){
            return Integer.MAX_VALUE;
        }

        // 为了消除正负数对计算的影响，统一把两个参数转换成负数，并要记录计算结果的正负性
        boolean negative = (dividend ^ divisor) < 0;

        if (dividend > 0){
            dividend = -dividend;
        }

        if (divisor > 0){
            divisor = -divisor;
        }

        // 用进制位移做计算
        int result = 0;

        while(dividend <= divisor){
            int i = 0;
            while((divisor << i) >= 0xc0000000 && dividend < (divisor << (i+1))){
                i++;
            }
            result += 1 << i;
            dividend -= divisor << i;
        }

        return negative ? -result : result;
    }


    /**
     * 使用减法计算法
     *
     * @param dividend
     * @param divisor
     * @return
     */
    public static int subtraction(int dividend, int divisor){

        // 3、计算结果发生溢出（int 类型存不下）时，返回整数的最大值。
        if (dividend == Integer.MIN_VALUE && divisor == -1){
            return Integer.MAX_VALUE;
        }

        // 为了消除正负数对计算的影响，统一把两个参数转换成负数，并要记录计算结果的正负性
        boolean negative = (dividend ^ divisor) < 0;

        if (dividend > 0){
            dividend = -dividend;
        }

        if (divisor > 0){
            divisor = -divisor;
        }

        // 用进制位移做计算
        int result = 0;

        while (dividend <= divisor){
            int value = divisor + 0;
            int i = 1;
            while (value >= 0xc0000000 && dividend <= value + value){
                i += i;
                value += value;
            }

            result += i;
            dividend -= value;
        }

        return negative ? -result : result;
    }
}
