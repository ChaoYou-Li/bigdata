package pf.bluemoon.com.common.order;

import lombok.Data;

/**
 * @Author chaoyou
 * @Date Create in 15:27 2022/11/18
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public enum CityEnum {
    BEIJING(100000000000000L, "北京"),
    SHANGHAI(101000000000000L, "上海"),
    SHENZHEN(102000000000000L, "深圳"),
    GUANGZHOU(103000000000000L, "广州"),
    HANGZHOU(104000000000000L, "杭州"),
    SUZHOU(105000000000000L, "苏州"),
    NANJING(106000000000000L, "南京"),
    WUHAN(107000000000000L, "武汉"),
    XIAN(108000000000000L, "西安"),
    TIANJIN(109000000000000L, "天津"),
    XIAMEN(110000000000000L, "厦门"),
    NANCHANG(111000000000000L, "南昌"),
            ;

    private long code;
    private String city;


    public long getCode() {
        return code;
    }

    public String getCity() {
        return city;
    }

    CityEnum(long code, String city) {
        this.code = code;
        this.city = city;
    }

    public static CityEnum getEnumByCity(String city){
        for (CityEnum matchEnum : CityEnum.values()){
            if (matchEnum.city.equalsIgnoreCase(city)){
                return matchEnum;
            }
        }
        return null;
    }
}
