package pf.bluemoon.com.hadoop.reducejoin;

import lombok.Data;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Author: chaoyou
 * Email：1277618785@qq.com
 * CSDN：https://blog.csdn.net/qq_41910568
 * Date: 18:27 2022/8/14
 * Content：
 */
@Data
public class TableBean implements Writable, WritableComparable<TableBean> {

    /**
     * 订单id
     */
    private String id = "";
    /**
     * 商品id
     */
    private String pid = "";
    /**
     * 商品数量
     */
    private Long amount = 0L;
    /**
     * 产品名称
     */
    private String proName = "";

    /**
     * 数据所在表名
     */
    private String tabName = "";

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(pid);
        out.writeLong(amount);
        out.writeUTF(proName);
        out.writeUTF(tabName);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readUTF();
        this.pid = in.readUTF();
        this.amount = in.readLong();
        this.proName = in.readUTF();
        this.tabName = in.readUTF();
    }

    @Override
    public String toString() {
        return id + "\t" + proName + "\t" + amount;
    }

    @Override
    public int compareTo(TableBean o) {
        if (this.amount > o.amount){
            return 1;
        } else {
            return -1;
        }
    }
}
