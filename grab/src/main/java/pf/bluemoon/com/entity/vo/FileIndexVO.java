package pf.bluemoon.com.entity.vo;

import pf.bluemoon.com.utils.SnowflakeUtils;

import java.io.Serializable;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-15 14:50
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class FileIndexVO implements Serializable {

    /**
     * 文件存储地址
     */
    private String filePath;
    /**
     * 位置偏移量/行数
     */
    private int index;
    /**
     * 增长步长
     */
    private int step;
    /**
     * 校验码（雪花算法id），用于乐观锁
     */
    private long checkCode;

    public FileIndexVO(String filePath, Integer index) {
        this.filePath = filePath;
        this.index = index;
        this.checkCode = SnowflakeUtils.getId();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getIndex() {
        return index;
    }

    public synchronized void increase(){
        this.index = getIndex() + getStep();
    }

    public long getCheckCode() {
        return checkCode;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
