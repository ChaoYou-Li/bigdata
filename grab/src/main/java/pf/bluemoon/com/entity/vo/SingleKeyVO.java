package pf.bluemoon.com.entity.vo;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-08 17:43
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class SingleKeyVO extends PrimryKeyVO implements Cloneable {
    /**
     * 索引名称
     */
    private String keyName;

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public SingleKeyVO(){}

    public SingleKeyVO(String keyName, Object keyValue, WriteVO writeVO){
        this.keyName = keyName;
        this.setKeyValue(keyValue);
        this.setWriteVO(writeVO);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
