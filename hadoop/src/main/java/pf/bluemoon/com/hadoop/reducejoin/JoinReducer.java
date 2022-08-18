package pf.bluemoon.com.hadoop.reducejoin;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: chaoyou
 * Email：1277618785@qq.com
 * CSDN：https://blog.csdn.net/qq_41910568
 * Date: 18:50 2022/8/14
 * Content：
 */
public class JoinReducer extends Reducer<Text, TableBean, TableBean, NullWritable> {

    // 自定义逻辑
    @Override
    protected void reduce(Text key, Iterable<TableBean> values, Context context) throws IOException, InterruptedException {
        List<TableBean> orderList = new ArrayList<>();
        TableBean pdBean = new TableBean();
        for (TableBean value : values) {
            // 判断数据来自哪个表
            if ("order".equals(value.getTabName())){
                // 创建一个临时TableBean对象接收value
                TableBean orderBean = new TableBean();
                BeanUtils.copyProperties(value, orderBean);
                // 在hadoop语法里面不能直接否则orderList.add(value)，否则orderList里面只有一个元素
                orderList.add(orderBean);
            } else {
                BeanUtils.copyProperties(value, pdBean);
            }
        }

        // 遍历集合orderBeans,替换掉每个orderBean的pid为pname,然后写出
        for (TableBean order : orderList) {
            order.setProName(pdBean.getProName());
            context.write(order, NullWritable.get());
        }
    }
}
