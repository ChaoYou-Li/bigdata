package pf.bluemoon.com.hadoop.mapjoin;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import pf.bluemoon.com.hadoop.reducejoin.TableBean;
import pf.bluemoon.com.hadoop.utils.NumberUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: chaoyou
 * Email：1277618785@qq.com
 * CSDN：https://blog.csdn.net/qq_41910568
 * Date: 18:39 2022/8/14
 * Content：
 */
public class JoinMapper extends Mapper<LongWritable, Text, TableBean, NullWritable> {

    private TableBean outKey = new TableBean();

    private Map<String, String> cache = new HashMap<>();

    /**
     * 获取缓存文件数据，并把数据读出来存放到一个map中
     */
    @Override
    protected void setup(Context context) throws IOException {
        // 从上下文中提取缓存文件路径
        URI[] cacheFiles = context.getCacheFiles();
        // 从上下文中的配置信息初始化一个文件系统对象
        FileSystem fs = FileSystem.get(context.getConfiguration());
        // 初始化一个输入流
        FSDataInputStream stream = fs.open(new Path(cacheFiles[0]));
        // 初始化一个读缓冲区
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        // 遍历缓冲区内的数据
        while (null != (line = reader.readLine())){
            if (line.length() < 1){
                continue;
            }
            String[] worlds = line.split("\t");
            // 从缓冲区中把数据写入到缓存当中
            cache.put(worlds[0], worlds[worlds.length - 1]);
        }
    }

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        if (null == line || line.length() < 1){
            return;
        }
        String[] worlds = line.split("\t");
        /**
         * 订单表处理逻辑
         */
        long amount = NumberUtil.isNumber(worlds[worlds.length - 1]) ? Long.parseLong(worlds[worlds.length - 1]) : 0;
        outKey.setId(worlds[0]);
        outKey.setAmount(amount);
        outKey.setPid(worlds[worlds.length - 2]);
        // 从缓存中提取产品名称
        outKey.setProName(cache.get(outKey.getPid()));
        outKey.setTabName("order");

        // 输出
        context.write(outKey, NullWritable.get());
    }
}
