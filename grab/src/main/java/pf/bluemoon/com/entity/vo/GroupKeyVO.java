package pf.bluemoon.com.entity.vo;

import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-22 17:03
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class GroupKeyVO implements Cloneable {
    private String keyName;
    private Object keyValue;
    private List<WriteVO> writeVOList;

    public Object getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(Object keyValue) {
        this.keyValue = keyValue;
    }

    public List<WriteVO> getWriteVOList() {
        return writeVOList;
    }

    public void setWriteVOList(List<WriteVO> writeVOList) {
        this.writeVOList = writeVOList;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public GroupKeyVO() {
    }

    public GroupKeyVO(String keyName, Object keyValue, List<WriteVO> writeVOList) {
        this.keyName = keyName;
        this.keyValue = keyValue;
        this.writeVOList = writeVOList;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
