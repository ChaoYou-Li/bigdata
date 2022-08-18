package pf.bluemoon.com.hadoop.serializable;

import lombok.Data;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 15:36 2022/8/8
 * @Modified by
 * @Version 1.0.0
 * @Description 自定义对象的序列化
 *      1、实现Writable接口
 *      2、重写序列化方法(write)
 *      3、重写反序列化方法(readFields)
 *      4、想要显示结果，需要重写toString方法
 *      5、如果把对象作为key传输，MapReduce框架中的Shuffle过程要求对key必须能排序，所以要实现 WritableComparable 接口，并重写compareTo方法
 */
@Data
public class FlowBean implements Writable, WritableComparable<FlowBean> {

    private Long upFlow;
    private Long downFlow;
    private Long sumFlow;

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(upFlow);
        out.writeLong(downFlow);
        out.writeLong(sumFlow);
    }

    /**
     * 注意反序列化的顺序和序列化的顺序完全一致
     *
     * @param in
     * @throws IOException
     */
    @Override
    public void readFields(DataInput in) throws IOException {
        upFlow = in.readLong();
        downFlow = in.readLong();
        sumFlow = in.readLong();
    }

    @Override
    public String toString() {
        return "FlowBean{" +
                "upFlow=" + upFlow +
                ", downFlow=" + downFlow +
                ", sumFlow=" + sumFlow +
                '}';
    }

    /**
     * 排序规则：
     *      sumFlow降序  ——>  upFlow升序  ——>  down降序
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(FlowBean o) {
        if (this.sumFlow > o.sumFlow){
            return -1;
        } else if (this.sumFlow < o.sumFlow){
            return 1;
        } else {
            if (this.upFlow > o.upFlow){
                return 1;
            } else if (this.upFlow < o.upFlow){
                return -1;
            } else if (this.downFlow > o.downFlow){
                return -1;
            } else if (this.downFlow < o.downFlow){
                return 1;
            } else {
                return 0;
            }
        }
    }
}
