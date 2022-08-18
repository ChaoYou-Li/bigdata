package pf.bluemoon.com.hadoop.reducejoin;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import pf.bluemoon.com.hadoop.utils.NumberUtil;

import java.io.IOException;

/**
 * Author: chaoyou
 * Email：1277618785@qq.com
 * CSDN：https://blog.csdn.net/qq_41910568
 * Date: 18:39 2022/8/14
 * Content：
 */
public class JoinMapper extends Mapper<LongWritable, Text, Text, TableBean> {

    private String fileName = "";

    private Text outKey = new Text();
    private TableBean outValue = new TableBean();

    /**
     * 获取文件名称(每个MapTask只会执行一遍)
     */
    @Override
    protected void setup(Context context) {
        InputSplit split = context.getInputSplit();
        FileSplit fileSplit = (FileSplit) split;
        fileName = fileSplit.getPath().getName();
    }

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        if (null == line || line.length() < 1){
            return;
        }
        String[] worlds = line.split("\t");
        if (fileName.contains("order")){
            /**
             * 订单表处理逻辑
             */
            outKey.set(worlds[worlds.length - 2]);
            long amount = NumberUtil.isNumber(worlds[worlds.length - 1]) ? Long.parseLong(worlds[worlds.length - 1]) : 0;
            outValue.setId(worlds[0]);
            outValue.setAmount(amount);
            outValue.setPid(worlds[worlds.length - 2]);
            outValue.setTabName("order");
        } else {
            /**
             * 商品表处理逻辑
             */
            outKey.set(worlds[0]);
            outValue.setProName(worlds[worlds.length - 1]);
            outValue.setTabName("pd");
        }

        // 输出
        context.write(outKey, outValue);
    }
}
