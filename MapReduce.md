# Hadoop技术之MapReduce框架
## MapReduce概述
### MapReduce定义
MapReduce是一个分布式运算框架，是用户开发“基于Hadoop的数据分析应用”的核心框架。
MapReduce核心功能是将用户编写的业务逻辑代码和自带默认组件整合成一个完整的分布式运算程序，并发运行在一个Hadoop集群上。
### MapReduce优缺点
#### 优点
`易于编程`：开发者只需要专注于编写自己的业务代码，MapReduce框架会帮助完成一个分布式程序，也就是说你写一个分布式程序，跟写一个简单的串行程序是一模一样的。就是因为这个特点使得MapReduce编程变得非常流行。

`良好的扩展性`：当你的计算资源不能得到满足的时候，你可以通过简单的增加机器来扩展它的计算能力。

`高容错性`：MapReduce设计的初衷就是使程序能够部署在廉价的PC机器上，这就要求它具有很高的容错性。比如其中一台机器挂了，它可以把上面的计算任务转移到另外一个节点上运行，不至于这个任务运行失败，而且这个过程不需要人工参与，而完全是由Hadoop内部完成的。

`适合PB级以上海量数据的离线处理`：适合PB级以上海量数据的离线处理
#### 缺点
`不善长实时计算`：MapReduce无法像MySQL一样，能在毫秒或者秒级的响应效果。

`不善长流式计算`：流式计算的输入数据是动态的，而MapReduce的输入数据集是静态的，不能动态变化。这是因为MapReduce自身的MapReduce自身设计特点决定了数据源必须是静态的。

`不善长DAG（有向无环图）计算`：多个应用存在依赖关系，后一个应用的输入为前一个应用的输出。在这种情况下MapReduce并不是不能做，而是使用后，每个MapReduce作业的输出结果都会写入到磁盘，会造成大量的磁盘IO，导致性能非常的低。

### MapReduce核心思想
分布式的运算程序往往需要分成至少2个阶段
#### MrAppMaster
它负责整个程序的**过程调度**及**状态调度**
#### MapTask阶段
这个阶段的并发实例，完全并行运行，互不相干。

- 读数据（TextInputFormat），并按行处理文件数据
- 格式化行数据，把行数据按照保存的规范进行格式化把具体的属性提出来
- 业务逻辑代码，这部分代码是自定义的、业务逻辑
- 输出K/V键值对，把从行数据中提取出来的属性按K/V键值对方式输出
#### ReduceTask阶段
这个阶段的并发实例互不相干，但是它们输入数据依赖于上一阶段所有的MapTask并发实例的输出。

- 

## Hadoop序列化
### 序列化概述
#### 序列化
序列化就是把内存中的对象，转换成字节序列（或其他数据传输协议）以便于存储到磁盘（持久化）和网络传输。
#### 反序列化
反序列化就是将收到字节序列（或其他数据传输协议）或者是磁盘的持久化数据，转换成内存中的对象。
#### 为什么要序列化
一般来说，“活的”对象只生存在内存里，关机断电就没有了。而且“活的”对象只能由本地的进程使用，并不能在网络上进行传输。 然而序列化可以存储“活的”对象，可以将“活的”对象就可以在网络上传输了。
#### Java序列化和Hadoop序列化
Java的序列化是一个重量级序列化框架（Serializable），一个对象被序列化后，会附带很多额外的信息（各种校验信息，Header，继承体系等），不便于在网络中高效传输。所以，Hadoop自己开发了一套序列化机制（Writable）。
#### Hadoop序列化特点
`紧凑`：高效实用存储空间

`快速`：读/写数据的额外开销小

`互操作`：支持多语言的交互

### 序列化实战
- 实现Writable接口
- 重写序列化方法(write)
- 重写反序列化方法(readFields)
- 想要显示结果，需要重写toString方法
- 如果把对象作为key传输，MapReduce框架中的Shuffle过程要求对key必须能排序，所以要实现Comparable接口，并重写compareTo方法

```java
public class FlowBean implements Writable, WritableComparable<FlowBean> {

    private String username;
    private String password;
    private int grade;

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBytes(username);
        out.writeBytes(password);
        out.writeInt(grade);
    }

    /**
     * 注意反序列化的顺序和序列化的顺序完全一致
     *
     * @param in
     * @throws IOException
     */
    @Override
    public void readFields(DataInput in) throws IOException {
        username = in.readLine();
        password = in.readLine();
        grade = in.readInt();
    }

    @Override
    public String toString() {
        return "FlowBean{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", grade=" + grade +
                '}';
    }

    @Override
    public int compareTo(FlowBean o) {
        return this.grade > o.grade ? -1 : 1;
    }
}
```
## MapReduce框架原理
### InputFormat数据输入
#### 切片与MapTask并行度决定机制
MapTask的并行度决定Map阶段的任务处理并发度，进而影响到整个Job的处理速度。
##### 问题引出
`思考`：1G的数据，启动8个MapTask，可以提高集群的并发处理能力。那么1K的数据，也启动8个MapTask，会提高集群性能吗？MapTask并行任务是否越多越好呢？哪些因素影响了MapTask并行度？
`注意`：当数据的处理时间小于MapTask的创建时间时，MapTask的并发性能反而会降低
##### MapTask并行度决定机制
`数据块`：Block是HDFS物理上把数据分成一块一块。数据块是HDFS存储数据的单位。
`数据切片`：数据切片只是在逻辑上对输入进行分片，并不会在磁盘上将其切分成片进行存储。数据切片是MapReduce程序计算输入数据的单位，一个切片会对应启动一个MapTask。
#### Job提交流程源码和切片源码详解
##### Job提交流程源码详解
```
                org.apache.hadoop.mapreduce.Job.waitForCompletion()
                                         |
                                         V
                      org.apache.hadoop.mapreduce.Job.submit()
                                         |
                                         V
                     org.apache.hadoop.mapreduce.Job.connect()
             org.apache.hadoop.mapreduce.JobSubmitter.submitJobInternal()
                                         |
                                         V
org.apache.hadoop.mapreduce.JobSubmissionFiles.getStagingDir(org.apache.hadoop.mapreduce.Cluster, org.apache.hadoop.conf.Configuration)
           org.apache.hadoop.mapreduce.protocol.ClientProtocol.getNewJobID()
          org.apache.hadoop.mapreduce.JobSubmitter.copyAndConfigureFiles() ——> org.apache.hadoop.mapreduce.JobResourceUploader.uploadResources()
               org.apache.hadoop.mapreduce.JobSubmitter.writeSplits()                                                                         |
                org.apache.hadoop.mapreduce.JobSubmitter.writeConf() ——> org.apache.hadoop.conf.Configuration.writeXml(java.io.OutputStream)  |
                org.apache.hadoop.mapred.YARNRunner.submitJob()                                                                               V
                                         |                                                                 org.apache.hadoop.mapreduce.JobResourceUploader.uploadResourcesInternal()
                                         V                                                                 
         org.apache.hadoop.mapred.YARNRunner.createApplicationSubmissionContext()

```

```java
// 提交Job
waitForCompletion()
    // 正式提交
    submit();    
        // 1建立连接
        connect();	
            // 1）创建提交Job的代理
            new Cluster(getConfiguration());
                // （1）判断是本地运行环境还是yarn集群运行环境
                initialize(jobTrackAddr, conf); 
    
        // 2 提交job
        submitter.submitJobInternal(Job.this, cluster)
    
            // 1）创建给集群提交数据的Stag路径
            Path jobStagingArea = JobSubmissionFiles.getStagingDir(cluster, conf);
        
            // 2）获取jobid ，并创建Job路径
            JobID jobId = submitClient.getNewJobID();
        
            // 3）拷贝jar包到集群
            copyAndConfigureFiles(job, submitJobDir);
                uploadResources(Job job, Path submitJobDir);
                    uploadResourcesInternal(Job job, Path submitJobDir);
                        uploadFiles(job, jobSubmitDir);
        
            // 4）计算切片，生成切片规划文件
            writeSplits(job, submitJobDir);
                writeNewSplits(job, jobSubmitDir);
                    input.getSplits(job);
        
            // 5）向Stag路径写XML配置文件
            writeConf(conf, submitJobFile);
                conf.writeXml(out);

            // 6）提交Job,返回提交状态
            status = submitClient.submitJob(jobId, submitJobDir.toString(), job.getCredentials());
                //（1）构造启动 MR AM 所需的所有信息
                ApplicationSubmissionContext appContext = createApplicationSubmissionContext(conf, jobSubmitDir, ts);
                //（2）创建 MrAppMaster 并执行调度
                ApplicationId applicationId = resMgrDelegate.submitApplication(appContext);
                ApplicationReport appMaster = resMgrDelegate.getApplicationReport(applicationId);
```
##### FileInputFormat切片源码解析（input.getSplits(job)）
切片逻辑源码
```java
public List<InputSplit> getSplits(JobContext job) throws IOException {
    StopWatch sw = new StopWatch().start();
    // 默认值：1
    long minSize = Math.max(getFormatMinSplitSize(), getMinSplitSize(job));
    // 默认值：Long.MAX_VALUE
    long maxSize = getMaxSplitSize(job);
    
    // 切片收集器
    List<InputSplit> splits = new ArrayList<InputSplit>();
    List<FileStatus> files = listStatus(job);
    
    boolean ignoreDirs = !getInputDirRecursive(job)
      && job.getConfiguration().getBoolean(INPUT_DIR_NONRECURSIVE_IGNORE_SUBDIRS, false);
    
    // 按文件为单位进行切片，而不是数据集
    for (FileStatus file: files) {
      if (ignoreDirs && file.isDirectory()) {
        continue;
      }
      Path path = file.getPath();
      // 获取文件的长度
      long length = file.getLen();
      if (length != 0) {
        BlockLocation[] blkLocations;
        if (file instanceof LocatedFileStatus) {
          // 从本地环境获取文件块的偏移量
          blkLocations = ((LocatedFileStatus) file).getBlockLocations();
        } else {
          // 从Yarn集群环境获取文件块的偏移量
          FileSystem fs = path.getFileSystem(job.getConfiguration());
          blkLocations = fs.getFileBlockLocations(file, 0, length);
        }
        // 判断文件是否可以切片（压缩文件一般不可再切片）
        if (isSplitable(job, path)) {
          // 数据块：32M(本地模式)、128M(Yarn集群模式)
          long blockSize = file.getBlockSize();
          // 切片大小：默认是数据块大小，可以通过minSize调大，也可以通过maxSize调小
          long splitSize = computeSplitSize(blockSize, minSize, maxSize);
    
          long bytesRemaining = length;
          /**
           * 每次切片前都会比较：
           *         文件剩余容量 / 切片设置值 > 1.1
           *            是：执行切片，并继续切片循环
           *            否：作为最后一块分片，跳出切片循环
           */
          while (((double) bytesRemaining)/splitSize > SPLIT_SLOP) {
            int blkIndex = getBlockIndex(blkLocations, length-bytesRemaining);
            // 执行切片
            splits.add(makeSplit(path, length-bytesRemaining, splitSize,
                        blkLocations[blkIndex].getHosts(),
                        blkLocations[blkIndex].getCachedHosts()));
            bytesRemaining -= splitSize;
          }
    
          if (bytesRemaining != 0) {
            int blkIndex = getBlockIndex(blkLocations, length-bytesRemaining);
            splits.add(makeSplit(path, length-bytesRemaining, bytesRemaining,
                       blkLocations[blkIndex].getHosts(),
                       blkLocations[blkIndex].getCachedHosts()));
          }
        } else { // not splitable
          if (LOG.isDebugEnabled()) {
            // Log only if the file is big enough to be splitted
            if (length > Math.min(file.getBlockSize(), minSize)) {
              LOG.debug("File is not splittable so no parallelization "
                  + "is possible: " + file.getPath());
            }
          }
          splits.add(makeSplit(path, 0, length, blkLocations[0].getHosts(),
                      blkLocations[0].getCachedHosts()));
        }
      } else { 
        //Create empty hosts array for zero length files
        splits.add(makeSplit(path, 0, length, new String[0]));
      }
    }
    // Save the number of input files for metrics/loadgen
    job.getConfiguration().setLong(NUM_INPUT_FILES, files.size());
    sw.stop();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Total # of splits generated by getSplits: " + splits.size()
          + ", TimeTaken: " + sw.now(TimeUnit.MILLISECONDS));
    }
    return splits;
}
```

切片实体源码，通过读源码可知切片中保存的信息包括：切片所在文件名、切片第一个字节的偏移量、切片长度、文件块所在机器host
```java
 /** Constructs a split with host information
   *
   * @param file 切片所在文件名
   * @param start 切片第一个字节的偏移量
   * @param length 切片长度
   * @param hosts 包含该块的主机列表，可能为空
   */
  public FileSplit(Path file, long start, long length, String[] hosts) {
    this.file = file;
    this.start = start;
    this.length = length;
    this.hosts = hosts;
  }
```
##### 切片机制
思考：在运行MapReduce程序时，输入的文件格式包括：基于行的日志文件、二进制格式文件、数据库表等。那么，针对不同的数据类型，MapReduce是如何读取这些数据的呢？
###### TextInputFormat
TextInputFormat是默认的FileInputFormat实现类。按行读取每条记录。键是存储该行在整个文件中的起始字节偏移量， LongWritable类型。值是这行的内容，不包括任何行终止符（换行符和回车符），Text类型。
###### CombineTextInputFormat
框架默认的TextInputFormat切片机制是对任务按文件规划切片，不管文件多小，都会是一个单独的切片，都会交给一个MapTask，这样如果有大量小文件，就会产生大量的MapTask，处理效率极其低下。
`当分片处理时间小于MapTask创建时间时，大量创建MapTask也会消耗大量时间，造成MapReduce处理效率低下`
####### 应用场景
CombineTextInputFormat用于小文件过多的场景，它可以将多个小文件从逻辑上规划到一个切片中，所以就可以把多个小文件交给一个MapTask处理。
####### 虚拟存储切片最大值设置
注意：虚拟存储切片最大值设置最好根据实际的小文件大小情况来设置具体的值。
```java
CombineTextInputFormat.setMaxInputSplitSize(job, 4 * 1024 * 1024);// 4m
```
####### 切片过程
生成切片过程包括：虚拟存储过程和切片过程二部分。
- 虚拟存储过程

将`输入目录`下所有文件大小，依次和设置的`setMaxInputSplitSize`值比较，如果`不大于`设置的最大值，逻辑上划分`一个块`。如果输入文件`大于`设置的最大值`且大于两倍`，那么以`最大值切割一块`；当`剩余数据`大小`超过设置的最大值且不大于最大值2倍`，此时将文件`均分成2个虚拟存储块`（防止出现太小切片）。

例如：setMaxInputSplitSize值为`4M`，输入文件大小为8.02M，则先逻辑上分成一个4M。剩余的大小为4.02M，如果按照4M逻辑划分，就会出现0.02M的小的虚拟存储文件，所以将剩余的4.02M文件切分成（2.01M和2.01M）两个文件。

- 切片过程

（1）判断虚拟存储的文件大小是否大于`setMaxInputSplitSize`值：

（1.1）大于等于则单独形成`一个切片`。

（1.2）如果不大于则跟下一个虚拟存储文件进行`合并`，共同形成`一个切片`。

（2）测试举例：有4个小文件大小分别为1.7M、5.1M、3.4M以及6.8M这`四个小文件`，则虚拟存储之后形成`6个文件块`，大小分别为：1.7M，（2.55M、2.55M），3.4M以及（3.4M、3.4M）

（3）最终会形成`3个切片`，大小分别为：（1.7+2.55）M，（2.55+3.4）M，（3.4+3.4）M
- 源码解析
```java
/**
 * 单个文件的切片原理
 */
static class OneFileInfo {
    private long fileSize;               // 文件大小
    private OneBlockInfo[] blocks;       // 这个文件可以规划的切片数组

    OneFileInfo(FileStatus stat, Configuration conf,
                boolean isSplitable,
                HashMap<String, List<OneBlockInfo>> rackToBlocks,
                HashMap<OneBlockInfo, String[]> blockToNodes,
                HashMap<String, Set<OneBlockInfo>> nodeToBlocks,
                HashMap<String, Set<String>> rackToNodes,
                long maxSize)
                throws IOException {
      this.fileSize = 0;

      // 记录文件系统中数据块的的偏移量
      BlockLocation[] locations;
      if (stat instanceof LocatedFileStatus) {
        // 从本地环境中的文件中获取偏移量
        locations = ((LocatedFileStatus) stat).getBlockLocations();
      } else {
        // 从Yarn集群环境中的文件中获取偏移量
        FileSystem fs = stat.getPath().getFileSystem(conf);
        locations = fs.getFileBlockLocations(stat, 0, stat.getLen());
      }
      // 创建所有块及其位置的列表
      if (locations == null) {
        blocks = new OneBlockInfo[0];
      } else {
        
        if(locations.length == 0 && !stat.isDirectory()) {
          locations = new BlockLocation[] { new BlockLocation() };
        }

        if (!isSplitable) {
          // 如果文件不可拆分，只需创建一个完整文件长度的块
          if (LOG.isDebugEnabled()) {
            LOG.debug("File is not splittable so no parallelization "
                + "is possible: " + stat.getPath());
          }
          blocks = new OneBlockInfo[1];
          // 获取文件的长度
          fileSize = stat.getLen();
          // 初始化一个切片对象（信息包括：所属文件路径、起始偏移量、切片长度、所属文件所在文件系统主机、网络拓扑中的完整路径名）
          blocks[0] = new OneBlockInfo(stat.getPath(), 0, fileSize,
              locations[0].getHosts(), locations[0].getTopologyPaths());
        } else {
          // 文件切片逻辑如下：
          ArrayList<OneBlockInfo> blocksList = new ArrayList<OneBlockInfo>(
              locations.length);
          for (int i = 0; i < locations.length; i++) {
            // 计算所有文件总长度
            fileSize += locations[i].getLength();

            // 获取文件长度
            long left = locations[i].getLength();
            // 切片在文件中的起始偏移量
            long myOffset = locations[i].getOffset();
            // 切片长度
            long myLength = 0;
            do {
              if (maxSize == 0) {
                // 切片设置值 = 0，相当于不对文件进行切片
                myLength = left;
              } else {
                if (left > maxSize && left < 2 * maxSize) {
                   /**
                    * 如果 left 在 maxSize 和 2*maxSize 之间，
                    * 那么我们创建大小为 left/2 的分片，而不是创建大小为 maxSize、left-maxSize 的分片。 
                    * 这是一种避免创建非常小的分割的启发式方法。
                    */
                  myLength = left / 2;
                } else {
                  /**
                   * 如果 left 比 maxSize 大，则创建 maxSize 大小的分片，否则创建 left 大小的分片。
                   */
                  myLength = Math.min(maxSize, left);
                }
              }

              // 初始化一个分片对象
              OneBlockInfo oneblock = new OneBlockInfo(stat.getPath(),
                  myOffset, myLength, locations[i].getHosts(),
                  locations[i].getTopologyPaths());
              
              // 更新文件剩余长度
              left -= myLength;
              // 更新切片在文件中的起始偏移量
              myOffset += myLength;

              blocksList.add(oneblock);
            } while (left > 0);
          }
          blocks = blocksList.toArray(new OneBlockInfo[blocksList.size()]);
        }
        
        populateBlockInfo(blocks, rackToBlocks, blockToNodes, 
                          nodeToBlocks, rackToNodes);
      }
    }
  }
```

##### 切片实战
```java
public static void drive(String[] args){
    try {
        // 1、获取配置信息以及获取job对象
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 2、关联Driver程序的jar
        job.setJarByClass(CombineDriver.class);

        // 3、关联Mapper和Reducer的jar
        job.setMapperClass(HelloMapper.class);
        job.setReducerClass(HelloReducer.class);

        // 4、设置Mapper输出的K/V类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        // 5、设置最终输出的类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        /**
         * 设置小文件合并切片机制，并设置虚拟存储切片最大值
         */
        job.setInputFormatClass(CombineTextInputFormat.class);
        CombineTextInputFormat.setMaxInputSplitSize(job, 20 * 1024 * 1024);

        // 6、设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    } catch (Exception e){
        e.printStackTrace();
    }
}
```
### MapReduce工作流程
```
    MapTask                     Shuffle                        ReduceTask
-----------------------------------|---------------------------------------
          Mapper ------------------|-------------------> Reducer
           /                       |                       \
     InputFormat                   |                    OutputFormat
        /                          |                          \
Input /                            |                            \ Output
-----------------------------------|---------------------------------------

```
Shuffle 描述着数据从MapTask输出到ReduceTask输入的这段过程。shuffle是连接`Map和Reduce之间的桥梁`，Map的输出要用到Reduce中必须经过shuffle这个环节，shuffle的性能高低直接影响了整个程序的性能和吞吐量。因为在分布式情况下，ReduceTask需要跨节点去拉取其它节点上的MapTask结果。这一过程将会产生`网络资源`和`内存`，`磁盘IO`的消耗。
通常shuffle分为两部分：Map 阶段的数据准备和 Reduce 阶段的数据拷贝处理。一般将在Map阶段的Shuffle称之为`Shuffle Write`，在Reduce阶段的Shuffle称之为`Shuffle Read`。

```
                           TextFileInputFormat
                        /                       \
      >-----------------                          FileInputFormat------------MrAppMaster
      |                 \                       /        |
      |                   CombineFileInputFormat         |
      |                                                  |
      |                                      ---------------------------
      |                                     | FileSplit + jar + job.xml |        MapTask
      |                                      ---------------------------  
      |
hdfs/localFile



```
#### 源文件数据
待处理的文件数据
#### 客户端提交Job
提交自定义的job作业，客户端submit()前，获取待处理的数据信息，然后根据参数配置，形成一个任务分配的规划。
```java
// 7、提交
boolean completion = job.waitForCompletion(true);
System.exit(completion ? 0 : 1);
```
#### 切割分片
 MapReduce负责读取源数据的是FileInputFormat，而切片的处理类均是其子类，其中切片机制有两种：`TextInputFormat`(按行读取数据)、`CombineTextInputFormat`(小文件处理机制)。
 Job会为每个切片创建一个对应的切片临时文件，里面放着`Job.xml`、`切片`、`MapReduce处理jar`(本地模式没有)。
#### 计算MapTask个数
在切片分割完成后就会去初始化MrAppMaster调度器，MrAppMaster会根据切片临时文件的个数计算出要启动的MapTask的个数
#### MapTask读取分片数据
FileInputFormat的父类是InputFormat，而InputFormat有一个方法org.apache.hadoop.mapreduce.InputFormat.createRecordReader()，这个方法会创建`RecordReader`类，这个类就是实际读取分片操作的执行者。
#### MapTask逻辑运算
MapTask其实就是调用Mapper处理类的进程，所以运算逻辑其实就是指Mapper类中的map()处理方法。
#### MapTask写数据到Collector
MapTask执行完成之后都会通过Context.write(K, V)把处理结果输出到Collector(环形缓冲区)，这个环形缓冲区(默认100M)，会分成两部分：索引、数据
#### 缓存数据分区/排序
在数据溢出过程及合并的过程中，都要调用Partitioner进行分区和针对key进行快速排序。可以自定义分区逻辑，但是自定义类需要实现接口。
#### 缓存数据溢出到磁盘
当前环形缓冲区的容量达到80%的时候，内存缓冲区从起始索引位置开始不断把内存数据溢出本地磁盘文件，可能会溢出多个文件。而从MapTask新写入到Collector的数据会执行逆向保存
#### 分区数据归并排序
多个溢出文件会被合并成大的溢出文件，MapTask是按行读取文件数据。每行数据都会产生多个分区，行间的会有同类型的分区，同类型的分区会进行归并排序。
#### 分区合并(同一个MapTask)
同一个MapTask的行间分区经过归并排序后已经形成了具体顺序的分区排列，同类型分区会进行合并成一个大的分区`每个分区都有唯一标识，所有的MapTask都有一样的分区`，把合并完成的分区写入到磁盘当中。
#### ReduceTask启动
MrAppMaster会根据不同类型分区数量决定要启动的ReduceTask数量，并且把分区标识赋给ReduceTask作为后续拉取数据的依据
#### 下载MapTask结果数据
ReduceTask创建并启动完成后，ReduceTask会根据分区标识去拉取磁盘上保存的每个MapTask结果数据。
#### 合并分区(归并排序，不同MapTask间)
ReduceTask根据自己的分区号，去各个MapTask机器上取相应的结果分区数据，ReduceTask会抓取到同一个分区的来自不同MapTask的结果文件，ReduceTask会将这些文件再进行合并（归并排序）
#### 自定义分组
在执行ReduceTask的reduce处理方法之前可以自定义一个分组策略`基本不会用`
#### ReduceTask结果输出
在ReduceTask执行完成reduce处理逻辑之后，向输出路径写入结果K/V
#### 注意两点
- Shuffle中的缓冲区大小会影响到MapReduce程序的执行效率，原则上说，缓冲区越大，磁盘io的次数越少，执行速度就越快。
- 缓冲区的大小可以通过参数调整，参数：mapreduce.task.io.sort.mb默认100M。 
### Shuffle机制
MapTask的map方法之后，ReduceTask的reduce方法之前的数据处理过程称之为 Shuffle。
#### Shuffle机制
- MapTask写数据到Collector
- 缓存数据分区/排序
- 缓存数据溢出到磁盘
- 分区数据归并排序
- 缓存数据溢出到磁盘
- 分区数据归并排序
- 分区合并(同一个MapTask)
- ReduceTask启动
- 下载MapTask结果数据
- 合并分区(归并排序，不同MapTask间)
- 自定义分组
#### Partition分区
##### 问题引出
要求将统计结果`按照条件输出到不同文件中`（分区）。比如：将统计结果按照手机归属地不同省份输出到不同的分区
##### 默认partition分区逻辑
```java
public class HashPartitioner<K2, V2> implements Partitioner<K2, V2> {

  public void configure(JobConf job) {}

  // 默认逻辑：partitions = key.hashCode() & numReduceTasks
  public int getPartition(K2 key, V2 value,
                          int numReduceTasks) {
    return (key.hashCode() & Integer.MAX_VALUE) % numReduceTasks;
  }

}
```
##### Partition分区案例实战
###### 自定义Partitioner步骤
- 自定义分区类，并实现Partitioner接口，重写getPartition()方法
- 在Job驱动中，设置自定义Partitioner类的关联关系
- 自定义Partition后，要根据自定义Partitioner的逻辑设置相应数量的ReduceTask数量
###### 实战代码
```java
/**
 * 自定义分区实现类
 */
public class CustomPartitioner extends Partitioner<Text, FlowBean> {

    /**
     * 分区逻辑：共设置5个分区，根据手机号前3位设置分区号
     *      0：136
     *      1：137
     *      2：138
     *      3：139
     *      4：其他
     *
     * @param key
     * @param value
     * @param numPartitions
     * @return
     */
    @Override
    public int getPartition(Text key, FlowBean value, int numPartitions) {
        int partition;
        String phone = key.toString();
        String prePhone = phone.substring(0, 3);
        if ("136".equals(prePhone)){
            partition = 1;
        } else if ("137".equals(prePhone)){
            partition = 2;
        } else if ("138".equals(prePhone)){
            partition = 3;
        } else if ("139".equals(prePhone)){
            partition = 4;
        } else {
            partition = 0;
        }
        return partition;
    }
}

/**
 * 作业调度类
 */
public class PartitionDriver {

    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(PartitionDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(FlowMapper.class);
        job.setReducerClass(FlowReducer.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);

        // 设置最后输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        // 关联自定义分区实现类
        job.setPartitionerClass(CustomPartitioner.class);
        // 设置ReduceTask数量
        job.setNumReduceTasks(5);

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);

    }
}
```
##### 分区总结
- 如果 ReduceTask数量 > getPartition()的结果，则会多产生几个空的输出文件part-r-000xx；
- 如果 1 < ReduceTask 的数量 < getPartition()结果，则有一部分分区数据无处安放，会报IOException；
- 如果 ReduceTask数量 = 1，则不管MapTask端输出多少个分区文件，最终结果都只会交给一个ReduceTask，最终也就只会产生一个结果文件part-r-00000；
- 分区号必须从零开始，逐一累加

#### WritableComparable排序
排序是MapReduce框架中最重要的操作之一。
##### 排序概述
- MapTask和ReduceTask均会对数据按照`key进行排序`。该操作属于Hadoop的`默认行为`。任何应用程序中的数据均会被排序，而不管逻辑上是否需要。
- 默认排序是按照`字典顺序`排序，且实现该排序的方法是`快速排序`。
- MapTask：它会将处理的结果暂时存放到Collector环形缓冲区中，`当环形缓冲区使用率达到一定阈值后，再对缓冲区中的数据进行一次快速排序`，并将有序数据溢写到磁盘上，而当数据处理完毕后，它会`对磁盘上所有文件进行归并排序`。
- ReduceTask：它从每个MapTask上远程拷贝相应的数据文件，如果文件大小超过一定阈值，则溢写到磁盘上，否则存储在内存中。如果磁盘上文件数目达到一定阈值，则进行一次归并排序以生成一个更大文件；如果内存中文件大小或者数目超过一定阈值，则进行一次合并后将数据溢写到磁盘上。当所有数据拷贝完毕后，`ReduceTask统一对内存和磁盘上的所有数据进行一次归并排序`。
##### 排序分类
- `部分排序`：`MapTask`根据输入的键值对数据集排序，保证`输出的每个文件内部有序`。
- `全排序`：`最终输出结果只有一个文件，且文件内部有序`。实现方式是只设置一个`ReduceTask`，但该方法在`实现大型文件时效率极低`，因为一台机器处理所有文件，完全丧失了MapReduce所提供的并行架构。
- `辅助排序`：（GroupingComparator分组），在Reduce端对key进行分组。应用于：在接受的key为bean对象时，想让一个或几个字段相同（全部字段比较不相同）的key进入到同一个reduce方法时，可以采用`分组排序`。
- `二次排序`：在`自定义排序`过程中，如果compareTo中的判断条件为两个，即为二次排序。
##### 自定义排序步骤
- 创建bean对象类，作为key传输
- 该bean实现WritableComparable接口，并重写compareTo方法
- 在compareTo方法中实现排序逻辑
- Mapper/Reducer中的key设置为bean对象
##### 自定义排序实战（全排序）
```java
/**
 * 创建bean对象：该bean实现WritableComparable接口，并重写compareTo方法
 */
public class FlowBean implements WritableComparable<FlowBean> {

    private Long upFlow;
    private Long downFlow;
    private Long sumFlow;

    /**
     * 排序逻辑：
     *      sumFlow降序  ——>  upFlow升序  ——>  down降序
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

/**
 * Mapper/Reducer中的key设置为bean对象
 */
public class ComparableMapper extends Mapper<LongWritable, Text, FlowBean, Text> {

    private FlowBean outKey = new FlowBean();

    private Text outValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        if (null == line || line.length() < 1){
            return;
        }
        String[] worlds = line.split("\t");

        // 提取出上行流量
        String upFlow = worlds[worlds.length - 3];
        outKey.setUpFlow(NumberUtil.isNumber(upFlow) ? Long.parseLong(upFlow) : 0);
        // 提取下行流量
        String downFlow = worlds[worlds.length - 2];
        outKey.setDownFlow(NumberUtil.isNumber(downFlow) ? Long.parseLong(downFlow) : 0);
        // 统计总流量
        outKey.setSumFlow(outKey.getUpFlow() + outKey.getDownFlow());
        // 提取手机号
        outValue.set(worlds[1]);
        // map输出
        context.write(outKey, outValue);
    }
}

/**
 * 驱动类
 */
public class ComparableDriver {

    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(ComparableDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(ComparableMapper.class);
        job.setReducerClass(ComparableReducer.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(FlowBean.class);
        job.setMapOutputValueClass(Text.class);

        // 设置最后输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }
}
```
##### 自定义排序实战（区内排序） 
```java
/**
 * 创建bean对象：该bean实现WritableComparable接口，并重写compareTo方法（如上所示）
 */

/**
 * Mapper/Reducer中的key设置为bean对象（如上所示）
 */

/**
 * 自定义分区类
 */
public class CustomPartitioner extends Partitioner<FlowBean, Text> {

    /**
     * 分区逻辑：共设置5个分区，根据手机号前3位设置分区号
     *      0：136
     *      1：137
     *      2：138
     *      3：139
     *      4：其他
     *
     * @param key
     * @param value
     * @param numPartitions
     * @return
     */
    @Override
    public int getPartition(FlowBean key, Text value, int numPartitions) {
        int partition;
        String phone = value.toString();
        String prePhone = phone.substring(0, 3);
        if ("136".equals(prePhone)){
            partition = 1;
        } else if ("137".equals(prePhone)){
            partition = 2;
        } else if ("138".equals(prePhone)){
            partition = 3;
        } else if ("139".equals(prePhone)){
            partition = 4;
        } else {
            partition = 0;
        }
        return partition;
    }
}

/**
 * 驱动类
 */
public class ComparableDriver {

    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(PartitionDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(ComparableMapper.class);
        job.setReducerClass(ComparableReducer.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(FlowBean.class);
        job.setMapOutputValueClass(Text.class);

        // 设置最后输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        // 关联自定义分区实现类
        job.setPartitionerClass(CustomPartitioner.class);
        // 设置ReduceTask数量
        job.setNumReduceTasks(5);

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);

    }
}
```
#### Combiner合并
##### 原理
- Combiner是MR程序中Mapper和Reducer之外的一种组件
- Combiner组件的父类就是Reducer
- Combiner和Reducer的区别在于运行的位置，Combiner是在每一个MapTask所在的节点运行；Reducer是接收全局所有Mapper的输出结果；
- Combiner的意义就是对每一个MapTask的输出进行进行局部汇总，以减小网络传输量。
- Combiner能够应用的前提是不能影响最终的业务逻辑，而且Combiner的输出K/V应该跟Reducer的输入K/V类型要对应起来
- Combiner不合适实现求平均值的逻辑
##### 自定义Combiner步骤
- 自定义Combiner类
- 继承Reducer类，并重写reduce()方法
- 在reduce()方法中实现自定义Combiner逻辑
- 在Job驱动类中设置
##### Combiner合并案例实操
```java
/**
 * 自定义Combiner类：继承Reducer类，并重写reduce()方法
 */
public class CombinerReducer extends Reducer<Text, FlowBean, Text, FlowBean> {
    private FlowBean outValue = new FlowBean();

    /**
     * 在reduce()方法中实现自定义Combiner逻辑
     */
    @Override
    protected void reduce(Text key, Iterable<FlowBean> values, Context context) throws IOException, InterruptedException {
        long upTotal = 0;
        long downTotal = 0;

        // 聚合计算
        for (FlowBean value : values) {
            upTotal += value.getUpFlow();
            downTotal += value.getDownFlow();
        }
        outValue.setUpFlow(upTotal);
        outValue.setDownFlow(downTotal);
        outValue.setSumFlow(upTotal + downTotal);

        // reduce输出
        context.write(key, outValue);
    }
}

/**
 * 在Job驱动类中设置
 */
public class CombinerDriver {
    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(PartitionDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(FlowMapper.class);
        job.setReducerClass(FlowReducer.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);

        // 设置最后输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        // 设置Combiner合并：在Job驱动类中设置
        job.setCombinerClass(CombinerReducer.class);

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }
}
```
### OutputFormat数据输出
OutputFormat是MapReduce输出的基类，所有实现MapReduce输出都实现了`OutputFormat`接口。
#### OutputFormat接口实现类
##### 默认实现类
MapReduce默认的输出格式是`TextOutputFormat`
##### 自定义OutputFormat实现类
- 应用场景：输出数据到MySQL/HBase/Elasticsearch/MongoDB等存储框架中
- 实现步骤：
    1、自定义一个类继承FileOutputFormat，关联自定义RecordWriter类；
    2、自定义一个类并继承RecordWriter，具体改写输出数据的方法write()方法
    3、job设置关联输出流对象
#### 自定义OutputFormat案例实战
```java
/**
 * 自定义一个类继承FileOutputFormat，关联自定义RecordWriter类；
 */
public class LogOutputFormat extends FileOutputFormat<Text, NullWritable> {

    //初始化自定义输出对象
    @Override
    public RecordWriter<Text, NullWritable> getRecordWriter(TaskAttemptContext job) {
        return new LogRecordWriter(job);
    }
}

/**
 * 自定义一个类并继承RecordWriter，具体改写输出数据的方法write()方法
 */
public class LogRecordWriter extends RecordWriter<Text, NullWritable> {

    private FSDataOutputStream atguiguOut;
    private FSDataOutputStream otherOut;

    //初始化输出流对象
    public LogRecordWriter(TaskAttemptContext job) {
        try {
            // 初始化配置参数
            FileSystem fs = FileSystem.get(job.getConfiguration());
            // 初始化输出流对象
            atguiguOut = fs.create(new Path("D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\out\\atguigu"));
            otherOut = fs.create(new Path("D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\out\\other"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //重写输出逻辑
    @Override
    public void write(Text key, NullWritable value) {
        try {
            String log = key.toString();
            if (null == log || log.length() < 1){
                return;
            }
            if (log.contains("atguigu")){
                atguiguOut.writeBytes(log + "\n");
            } else {
                otherOut.writeBytes(log + "\n");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //关闭输出流对象
    @Override
    public void close(TaskAttemptContext context) throws IOException {
        if (null != atguiguOut){
            atguiguOut.close();
        }
        if (null != otherOut){
            otherOut.close();
        }
    }
    
}

/**
 * job设置关联输出流对象
 */
public class LogDriver {
    public static void drive(String[] args) throws IOException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联driver驱动类jar
        job.setJarByClass(LogDriver.class);

        // 关联Mapper/Reducer类
        job.setMapperClass(LogMapper.class);
        job.setReducerClass(LogReducer.class);

        // 设置Mapper输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(NullWritable.class);

        // 设置最终输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        // 设置自定义输出类
        job.setOutputFormatClass(LogOutputFormat.class);

        // 6、设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }
}

```
### MapReduce内核源码解析
#### MapTask工作机制
MapTask过程可以分为5个阶段：Read、Map、Collect、溢写、Merge
##### Read阶段
MapTask通过InputFormat获得的`RecordReader对象`，从输入`InputSplit`中解析出一个个的key/value。
##### Map阶段
该节点主要是将解析出的key/value交给用户编写的`map方法`处理，并输出一系列新的key/value。
##### Collect收集阶段
在用户编写map方法中，当数据处理完成后，在`context.write()方法`后会调用`OutputCollector.collect()`输出方法。在该函数内部，它会将生成的key/value分区（调用`Partitioner`），并写入一个`环形缓冲区`中。
##### 溢写(Spill)阶段
当环形缓冲区满后（80%容量），MapReduce会将数据写到本地磁盘上，生成一个`临时文件`。需要注意的是，将数据写入本地磁盘之前，先要对数据进行一次`本地排序`，并在必要时对数据进行`合并`、`压缩`等操作。
###### 溢写详情
- `步骤一`：利用`快速排序`算法对缓存`区内`的数据进行排序。排序逻辑：`先按照分区编号Partition进行排序，然后按照key进行排序`。经过排序后，数据`以分区为单位聚集`在一起，且同一分区内所有数据按照key有序。
- `步骤二`：按照`分区编号`由小到大依次将每个分区中的数据写入任务工作目录下的`临时文件`output/spillN.out（N表示当前溢写次数）中。如果用户设置了Combiner，则`写入文件之前`，对每个分区中的数据进行一次`聚集`操作。
- `步骤三`：将分区数据的元信息写到`内存索引数据结构SpillRecord`中，其中每个分区的元信息包括：`在临时文件中的偏移量`、`压缩前数据大小`和`压缩后数据大小`。如果当前内存索引大小超过1MB，则将`内存索引`写到文件output/spillN.out.index中。
##### Merge阶段
当所有数据处理完成后，MapTask对所有临时文件进行一次`合并`，以确保最终只会生成一个数据大文件。
并保存到文件output/file.out中，同时生成相应的`索引文件`output/file.out.index。
在进行`文件合并`过程中，MapTask以分区为单位进行合并。对于某个分区，它将采用`多轮递归合并`的方式。每轮合并mapreduce.task.io.sort.factor（默认10）个文件，并将产生的文件重新加入`待合并列表`中，对文件排序后，重复以上过程，直到`最终得到一个大文件`。
让每个MapTask最终只生成一个数据大文件，`可避免同时打开大量文件和同时读取大量小文件产生的随机读取带来的开销`。
#### ReduceTask工作机制
ReduceTask过程可以分为三个阶段：Copy、Sort、Reduce
```java
copyPhase = getProgress().addPhase("copy");
sortPhase  = getProgress().addPhase("sort");
reducePhase = getProgress().addPhase("reduce");
```
##### Copy阶段
ReduceTask从各个MapTask上`远程拷贝`一片数据，并针对某一片数据，如果其大小超过一定阈值，则写到磁盘上，否则直接放到内存中。
##### Sort阶段
在远程拷贝数据的同时，ReduceTask启动了两个后台线程对内存和磁盘上的文件进行合并，以防止内存使用过多或磁盘上文件过多。按照MapReduce语义，用户编写reduce()函数输入数据是按key进行聚集的一组数据。为了将key相同的数据聚在一起，Hadoop采用了基于`排序`的策略。由于各个MapTask已经实现对自己的处理结果进行了局部排序，因此，ReduceTask只需对所有数据进行一次`归并排序`即可。
##### Reduce阶段
reduce()函数将计算结果写到HDFS上。
#### 并行度决定机制
##### MapTask
MapTask并行度由切片个数决定，切片个数由输入文件和切片规则决定。
##### ReduceTask
ReduceTask的并行度同样影响整个Job的执行并发度和执行效率，但与MapTask的并发数由切片数决定不同，ReduceTask数量的决定是可以直接手动设置：
```java
// 默认值是1，手动设置为4
job.setNumReduceTasks(4);
```
- ReduceTask=0，表示没有Reduce阶段，输出文件个数和Map个数一致
- ReduceTask默认值就是1，所以输出文件个数为一个
- 如果数据分布不均匀，就有可能在Reduce阶段产生`数据倾斜`
- ReduceTask数量并不是任意设置，还要考虑业务逻辑需求，有些情况下需要计算全局汇总结果，就只能有1个ReduceTask
- 具体设置多少个ReduceTask合理，需要根据集群性能而定
- 如果分区数不是1，但是ReduceTask为1，是否执行分区过程。答案是：不执行分区过程。因为在MapTask的源码中，分区的前提是先判断ReduceNum个数是否大于1，不大于1则不执行分区。
#### MapTask & ReduceTask源码解析
##### MapTask源码解析流程
```java
// 自定义的map方法的写出，进入
context.write(k, NullWritable.get());
    // 输出流执行输出方法
    output.write(key, value);  
        // MapTask727行，收集方法，进入两次 
        collector.collect(key, value,partitioner.getPartition(key, value, partitions));
            // 默认分区器
	        HashPartitioner(); 
        // MapTask1082行 map端所有的kv全部写出后会走下面的close方法
        collect();
            // MapTask732行
	        close();
                // 溢出刷写方法，MapTask735行，提前打个断点，进入
	            collector.flush();
                    // 溢写排序，MapTask1505行，进入
                    sortAndSpill();
                        // 溢写排序方法（QuickSort），MapTask1625行，进入
                        sorter = ReflectionUtils.newInstance(job.getClass(MRJobConfig.MAP_SORT_CLASS, QuickSort.class,IndexedSorter.class), job);
	                    sorter.sort();
                    // 合并文件，MapTask1527行，进入
                    mergeParts();
	            // MapTask739行,收集器关闭,即将进入ReduceTask
                collector.close();
```
##### ReduceTask源码解析流程
```java
// 初始化ReduceTask的三个阶段
if (isMapOrReduce()) {
    copyPhase = getProgress().addPhase("copy");
    sortPhase  = getProgress().addPhase("sort");
    reducePhase = getProgress().addPhase("reduce");
}
// 初始化ReduceTask节点
initialize();
// 执行ReduceTask阶段的Shuffle过程
init(shuffleContext);
    // 获取MapTask数量
    totalMaps = job.getNumMapTasks();
    // 合并方法，合并所有MapTask中的同类型分区数据
    merger = createMergeManager(context); 
            // 内存合并
            this.inMemoryMerger = createInMemoryMerger();
            // 磁盘合并
            this.onDiskMerger = new OnDiskMerger(this);
    rIter = shuffleConsumerPlugin.run();
        // 开始抓取数据，Shuffle第107行，提前打断点
		eventFetcher.start();
		// 抓取结束，Shuffle第141行，提前打断点
		eventFetcher.shutDown();  
        // copy阶段完成，Shuffle第151行
		copyPhase.complete();
        // 开始排序阶段，Shuffle第152行
		taskStatus.setPhase(TaskStatus.Phase.SORT);
    // 排序阶段完成，即将进入reduce阶段 reduceTask382行
	sortPhase.complete();
// reduce阶段调用的就是我们自定义的reduce方法，会被调用多次
reduce();
    // reduce完成之前，会最后调用一次Reducer里面的cleanup方法
	cleanup(context); 
```
### Join应用
#### Reduce Join
`Map端`：为来自不同表或文件的key/value，打标签以区别不同来源的记录。然后用`关联字段`作为key，其余部分和新加的标志作为value，最后进行输出。
`Reduce端`：在Reduce端以`关联字段`作为key的分组已经完成，我们只需要在每一个分组当中将那些来源于不同文件的记录（在Map阶段已经打标志）分开，最后进行合并就ok了。
##### 需求分析
通过将关联条件作为Map输出的key，将两表满足Join条件的数据并携带数据所来源的文件信息，发往同一个ReduceTask，在Reduce中进行数据的串联。
```
                    order                                       pd                                    result
        ---------------------------                    ---------------------              ---------------------------------
       |  id  |  pid   |  amount   |                  |   pid  |   pname    |            |   id  |  pname      |  amount   |
        ---------------------------           |         ---------------------             --------------------------------- 
       | 1001 |  01    |    1      |       ———|———    |    01  |   小米     |     ===>   |  1001 |   小米      |     1     |
        ---------------------------           |         ---------------------             ---------------------------------
       | 1002 |  02    |    2      |                  |    02  |   华为     |            |  1002 |   华为      |     2     |
        ---------------------------                    ---------------------              ---------------------------------
```
##### 实战
```java
/**
 * Content：设置一张合并后的结果表（order + pd）
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

/**
 * Content：自定义Mapper处理类
 */
public class JoinMapper extends Mapper<LongWritable, Text, Text, TableBean> {

    // 表名称(来源文件名)
    private String fileName = "";

    private Text outKey = new Text();
    private TableBean outValue = new TableBean();

    /**
     * 获取文件名称(每个MapTask只会执行一遍)
     */
    @Override
    protected void setup(Context context) {
        // 获取对应文件名称
        InputSplit split = context.getInputSplit();
        FileSplit fileSplit = (FileSplit) split;
        fileName = fileSplit.getPath().getName();
    }

    /**
     * 自定义逻辑的 map 方法
     */
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        if (null == line || line.length() < 1){
            return;
        }
        String[] worlds = line.split("\t");
        // 获取对应文件名称
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

        // map 结果输出
        context.write(outKey, outValue);
    }
}

/**
 * Content：自定义Reducer处理类
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

/**
 * Content：reducejoin的驱动类
 */
public class JoinDriver {
    public static void drive(String[] args) throws IOException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(JoinDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(JoinMapper.class);
        job.setReducerClass(JoinReducer.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(TableBean.class);

        // 设置最后输出类型
        job.setOutputKeyClass(TableBean.class);
        job.setOutputValueClass(NullWritable.class);

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }
}
```
##### 总结
缺点：这种方式中，合并的操作是在Reduce阶段完成，`Reduce端的处理压力太大`，Map节点的运算负载则很低，`资源利用率不高`，且在Reduce阶段极易产生数据倾斜。
#### Mapper Join
##### 适用场景
Map Join 合适处理大小表关联的情况，比如：A表数据量1G，B表数据量10M，这种情况适用Map Join处理比较有优势。
##### 优点
思考：在Reduce端处理过多的表，非常容易产生数据倾斜。怎么办？
在Map端缓存多张小表，提前处理业务逻辑，这样增加Map端业务，减少Reduce端数据的压力，尽可能的减少数据倾斜。
##### DistributedCache
- 在Mapper的setup阶段，将文件读取到缓存集合中。
- 在Driver驱动类中加载缓存。
##### 实战
```java
/**
 * TableBean 如上
 */

/**
 * Content：自定义Mapper处理类
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

/**
 * Content：map join 驱动类
 */
public class JoinDriver {
    public static void drive(String[] args) throws IOException, URISyntaxException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(JoinDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(JoinMapper.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(TableBean.class);
        job.setMapOutputValueClass(NullWritable.class);

        // 设置最后输出类型
        job.setOutputKeyClass(TableBean.class);
        job.setOutputValueClass(NullWritable.class);

        // 设置取消reduce阶段
        job.setNumReduceTasks(0);

        // 设置缓存数据
        job.addCacheFile(new URI("file:///C:/workspace/idea/springboot/hadoop-demo/src/main/resources/input/tablecache/pd.txt"));

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }
}
```
### ETL清洗
ETL，是英文Extract-Transform-Load的缩写，用来描述将数据从来源端经过抽取（Extract）、转换（Transform）、加载（Load）至目的端的过程。ETL一词较常用在数据仓库，但其对象并不限于数据仓库
在运行核心业务MapReduce程序之前，往往要先对数据进行清洗，清理掉不符合用户要求的数据。`清理的过程往往只需要运行Mapper程序，不需要运行Reduce程序`。
#### 需求分析
需要在Map阶段对输入的数据根据规则进行过滤清洗。去除日志中字段个数小于等于15的日志。
#### 实战
```java
/**
 * Content：etl的自定义Mapper处理类
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
        if (!pass){
            // 不符合条件就退出
            return;
        }
        
        // 符合需求的数据输出
        context.write(value, NullWritable.get());
    }
}

/**
 * Content：etl的驱动类
 */
public class ETLDriver {
    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        job.setJarByClass(ETLDriver.class);

        // 只需要关联 Mapper 即可
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
### MapReduce 总结
#### 输入数据接口：InputFormat
- 默认使用的实现类是：TextInputFormat
- TextInputFormat的功能逻辑是：一次读一行文本，然后将该行的`起始偏移量`作为key，`行内容`作为value返回。
- CombineTextInputFormat可以把多个小文件合并成一个切片处理，提高处理效率。
#### 逻辑处理接口：Mapper 
用户根据业务需求实现其中三个方法： setup()  ->  map()  ->  cleanup ()

`setup`：一般用于初始化一些资源
`map`：用户自定义处理逻辑
`cleanup`：一般用于关闭资源 
#### Partitioner分区
- 有默认实现 HashPartitioner，逻辑是根据key的哈希值和numReduces来返回一个分区号；默认逻辑：`key.hashCode() & Integer.MAXVALUE % numReduces`
- 如果业务上有特别的需求，可以自定义分区。需要`继承`Partitioner抽象类，并`实现`getPartition()方法
#### Comparable排序
- 当我们用自定义的对象作为key来输出时，就必须要实现WritableComparable接口，重写其中的compareTo()方法。
- 部分排序：对最终输出的每一个`文件`进行内部排序。
- 全排序：对所有数据进行排序，通常只有一个`Reduce`。
- 二次排序：排序的条件有两个。
#### Combiner合并
Combiner合并可以提高程序执行效率，减少IO传输。但是使用时必须不能影响原有的业务处理结果。
#### 逻辑处理接口：Reducer
用户根据业务需求实现其中三个方法： setup()  ->  reduce()  ->  cleanup ()
#### 输出数据接口：OutputFormat
- 默认实现类是TextOutputFormat，功能逻辑是：将每一个KV对，向目标文本文件输出一行。
- 用户还可以自定义OutputFormat。需要自定义OutputFormat和自定义RecordWriter类
