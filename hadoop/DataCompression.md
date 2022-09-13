# Hadoop 数据压缩
## 概述
### 压缩的好处和坏处
- `优点`：以减少磁盘IO、减少磁盘存储空间、减少网络传输时间。
- `缺点`：增加CPU开销，需要增添压缩和解压两个处理流程。
### 压缩原则
- `运算`密集型的Job，少用压缩。频繁调用数据，也意味着频繁对数据进行：解压 -> 压缩 -> 解压
- `IO`密集型的Job，多用压缩。只是用于网络传输，所以数据占用内存越少传输越快
## MR支持的压缩编码
### 压缩算法对比
|   压缩格式   |   Hadoop自带   |   算法   |   文件扩展名  |   是否可切片 | 换成压缩格式后，原来的程序是否需要修改| 
| ---- | ---- | ---- | ---- | ---- | ---- |
|   DEFLATE   |  是，直接使用    |   DEFLATE   |   .deflate   |   否   |   和文本处理一样，不需要修改   |
|   Gzip   |    是，直接使用  |   DEFLATE   |   .gz   |   否   |   和文本处理一样，不需要修改   |
|   bzip2   |   是，直接使用   |   bzip2   |   .bz2   |   是   |   和文本处理一样，不需要修改   |
|   LZO   |   否，需要安装   |   LZO   |   .lzo   |   是  |   需要建索引，还需要指定输入格式   |
|   Snappy   |   是，直接使用   |   Snappy   |   .snappy   |   否   |    和文本处理一样，不需要修改  |

### 压缩性能比较
|   压缩算法   |   原文件   |   压缩后   |   压缩速度   |   解压速度   |
| ---- | ---- | ---- | ---- | ---- |
|   gzip   |   8.3GB   |   1.8GB   |   17.5MB/s   |   58MB/s   |
|   bzip2   |   8.3GB   |   1.1GB   |   2.4MB/s   |   9.5MB/s   |
|   LZO   |   8.3GB   |   2.9GB   |   49.3MB/s   |   84.6MB/s   |

http://google.github.io/snappy/

Snappy is a compression/decompression library. It `does not aim for maximum compression`, or compatibility with any other compression library; instead, it `aims for very high speeds` and reasonable compression. For instance, compared to the fastest mode of zlib, Snappy is an order of magnitude faster for most inputs, but the resulting compressed files are anywhere from 20% to 100% bigger.On a single core of a Core i7 processor in 64-bit mode, Snappy `compresses at about 250 MB/sec` or more and `decompresses at about 500 MB/sec` or more.
## 压缩方式选择
`重点考虑`：压缩/解压缩速度、压缩率（压缩后存储大小）、压缩后是否可以支持切片。
### Gzip
`优点`：压缩率比较高

`缺点`：不支持Split(切片)；压缩/解压速度一般；
### Bzip2
`优点`：压缩率非常高；支持Split； 

`缺点`：压缩/解压速度慢。
### Lzo
`优点`：压缩/解压速度比较快；支持Split；

`缺点`：压缩率一般；想支持切片需要额外创建索引；不支持直接使用，需要额外安装
### Snappy
`优点`：压缩和解压缩速度快； 

`缺点`：不支持Split；压缩率一般；hadoop3.x之后自带(仅在Linux环境生效)
### 压缩位置选择
压缩可以在MapReduce作用的任意阶段启用。
```

         ---------------------------------------------- MapReduce ----------------------------------------------
                                  ^                                                        ^
                                  |                                                        | 
     --------------------------- Map --------------------------------------------------- Reduce -----------------------------
                 ^                                           ^                                                      ^
                 |                                           |                                                      |
---------- 输入端采用压缩 -------------------------- Mapper输出采用压缩 ----------------------------------- Reducer输出采用压缩 ------------
```
#### 输入端采用压缩
无需显示指定使用的编解码方式。hadoop自动检查文件扩展名，如果扩展名能够匹配，就会用恰当的编解码方式对文件进行压缩和解压；
##### 企业开发考虑因素
- 数据量小于块大小，重点考虑压缩和解压速度比较快的`LZO`和`Snappy`
- 数据量非常大，重点考虑支持切片的`Bzip2`和`LZO`
#### Mapper输出采用压缩
企业开发中为了减少MapTask和ReduceTask之间的网络I/O。重点考虑压缩和2解压速度快的`LZO`和`Snappy`
#### Reducer输出采用压缩
这么模块就得看需求了，如果数据永久保存，就优先考虑压缩率比较高的`Bzip2`和`Gzip`，如果作为下一个MapTask的输入，就需要考虑数据量大小以及切片能力了
## 压缩参数配置
为了支持多种压缩/解压缩算法，Hadoop引入了编码/解码器

|   压缩格式   |   对应的编码/解码器   |
| ---- | ---- |
|   DEFLATE   |   org.apache.hadoop.io.compress.DefaultCodec   |
|   Gzip   |   org.apache.hadoop.io.compress.GzipCodec   |
|   Bzip2   |   org.apache.hadoop.io.compress.BZip2Codec   |
|   LZO   |   com.hadoop.compression.lzo.LzopCodec   |
|   Snappy   |   org.apache.hadoop.io.compress.SnappyCodec  |

要在Hadoop中启用压缩，可以配置如下参数

|   参数   |   配置空间   |   默认值   |   阶段   |   建议   |
| ---- | ---- | ---- | ---- | ---- |
|   io.compression.codecs  | core-site.xml  |   无，这个需要在命令行输入hadoop checknative查看   |   输入压缩   |   Hadoop使用文件扩展名判断是否支持某种编解码器   |
|   mapreduce.map.output.compress|  mapred-site.xml  |   false   |   Mapper输出   |   这个参数设为true启用压缩   |
|   mapreduce.map.output.compress.codec|  mapred-site.xml     |   org.apache.hadoop.io.compress.DefaultCodec   |   mapper输出   |  企业多使用LZO或Snappy编解码器在此阶段压缩数据  |
|   mapreduce.output.fileoutputformat.compress|  mapred-site.xml     |   false   |   reducer输出   |   这个参数设为true启用压缩   |
|   mapreduce.output.fileoutputformat.compress.codec|  mapred-site.xml     |   org.apache.hadoop.io.compress.DefaultCodec   |   reducer输出   |   使用标准工具或者编解码器，如Gzip和Bzip2   |

## 压缩实战
### Mapper输出采用压缩
即使你的`MapReduce`的输入/输出文件都是未压缩的文件，你仍然可以对Map任务的中间结果输出做压缩，因为它要写在硬盘并且通过网络传输到Reduce节点，对其压缩可以提高很多性能，这些工作只要设置两个属性即可，我们来看下代码怎么设置。
```java
/**
 * Content：正常的Mapper处理类
 */
public class ETLMapper extends Mapper<LongWritable, Text, Text, NullWritable> {

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        // 取出行数据
        String line = value.toString();

        boolean pass = true;

        // etl清洗逻辑：筛选出每行字段长度都大于15
        String[] worlds = line.split(" ");
        if (worlds.length < 15){
            pass = false;
        }
        if (pass){
            context.write(value, NullWritable.get());
        }
    }
}

/**
 * Content：压缩实战
 */
public class ETLDriver {
    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        // 初始化配置参数
        Configuration config = new Configuration();
        // 开启map端输出压缩
        config.setBoolean("mapreduce.map.output.compress", true);
        // 设置map端输出压缩方式
        config.setClass("mapreduce.map.output.compress.codec", BZip2Codec.class, CompressionCodec.class);

        Job job = Job.getInstance(config);

        job.setJarByClass(ETLDriver.class);

        job.setMapperClass(ETLMapper.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(NullWritable.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        job.setNumReduceTasks(0);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        boolean result = job.waitForCompletion(true);
        System.exit(result ? 1 : 0);
    }
}
```
### Reducer输出采用压缩
```java
/**
 * Content：正常的Mapper处理类
 */
public class ETLMapper extends Mapper<LongWritable, Text, Text, NullWritable> {

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        // 取出行数据
        String line = value.toString();

        boolean pass = true;

        // etl清洗逻辑：筛选出每行字段长度都大于15
        String[] worlds = line.split(" ");
        if (worlds.length < 15){
            pass = false;
        }
        if (pass){
            context.write(value, NullWritable.get());
        }
    }
}

/**
 * Content：压缩实战
 */
public class ETLDriver {
    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        // 初始化配置参数
        Configuration config = new Configuration();

        Job job = Job.getInstance(config);

        job.setJarByClass(ETLDriver.class);

        job.setMapperClass(ETLMapper.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(NullWritable.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        job.setNumReduceTasks(0);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 设置reduce端输出压缩开启
        FileOutputFormat.setCompressOutput(job, true);
        // 设置压缩的方式
        FileOutputFormat.setOutputCompressorClass(job, BZip2Codec.class);

        boolean result = job.waitForCompletion(true);
        System.exit(result ? 1 : 0);
    }
}
```
