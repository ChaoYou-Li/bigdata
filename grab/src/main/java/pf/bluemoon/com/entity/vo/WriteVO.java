package pf.bluemoon.com.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-15 14:04
 * @Modified by
 * @Version 1.0.0
 * @Description 数据存储在文件的元数据
 */
public class WriteVO implements Serializable, Cloneable {

    /**
     * 写入文件的位置偏移量
     */
    private long position;

    /**
     * 写入文件数据的字节长度
     */
    private int length;

    /**
     * 校验码
     */
    private int hashCode;

    public WriteVO() {
    }

    public WriteVO(long position, int length, int hashCode) {
        this.position = position;
        this.length = length;
        this.hashCode = hashCode;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
