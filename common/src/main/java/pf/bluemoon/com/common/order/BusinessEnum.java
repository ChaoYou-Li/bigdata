package pf.bluemoon.com.common.order;

/**
 * @Author chaoyou
 * @Date Create in 15:28 2022/11/18
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public enum  BusinessEnum {
    BEIJING(100000000000000000L, "服装"),
    SHANGHAI(101000000000000000L, "手机"),
    SHENZHEN(102000000000000000L, "电脑"),
    GUANGZHOU(103000000000000000L, "背包"),
    HANGZHOU(104000000000000000L, "书籍"),
    SUZHOU(105000000000000000L, "家电"),
    NANJING(106000000000000000L, "药品"),
    WUHAN(107000000000000000L, "生鲜"),
    XIAN(108000000000000000L, "零件"),
    TIANJIN(109000000000000000L, "鞋子"),
    XIAMEN(110000000000000000L, "手表"),
    NANCHANG(121000000000000000L, "首饰"),
    ;

    private long code;
    private String business;

    public long getCode() {
        return code;
    }

    public String getBusiness() {
        return business;
    }

    BusinessEnum(long code, String business) {
        this.code = code;
        this.business = business;
    }

    public static BusinessEnum getEnumByAppName(String business){
        for (BusinessEnum matchEnum : BusinessEnum.values()){
            if (matchEnum.business.equalsIgnoreCase(business)){
                return matchEnum;
            }
        }
        return null;
    }
}
