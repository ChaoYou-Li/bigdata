package pf.bluemoon.com.entity.vo;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-22 17:00
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class PrimryKeyVO implements Cloneable {
    /**
     * 索引值
     */
    private Object keyValue;
    /**
     *
     */
    private WriteVO writeVO;

    public PrimryKeyVO() {
    }

    public PrimryKeyVO(Object keyValue, WriteVO writeVO) {
        this.keyValue = keyValue;
        this.writeVO = writeVO;
    }

    public Object getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(Object keyValue) {
        this.keyValue = keyValue;
    }

    public WriteVO getWriteVO() {
        return writeVO;
    }

    public void setWriteVO(WriteVO writeVO) {
        this.writeVO = writeVO;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
