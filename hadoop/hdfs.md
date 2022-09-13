# HDFS
## HDFS概述
### 产出背景及定义
#### 背景
随着大数据的来临，在单个机器上的磁盘文件系统已经不能满足高速增长的数据量。那么就需要分配更多的机器来进行存储，但是机器多了又不好管理`比如数据的上下链路问题等`，所以迫切需要一种可以管理多台机器上文件的系统。这就是分布式文件管理系统，HDFS只是分布式文件管理系统中的一种。
#### 定义
HDFS`Hadoop Distributed File System`：它是一个文件系统，用于存储文件，通过`目录树`来定位文件；其次，它是`分布式`的，由很多服务器联合起来实现其功能，集群中的服务器有各自的角色。
使用场景：适合`一次写入，多次读出`的场景。一个文件经过创建、写入和关闭之后就不需要改变。
### HDFS优缺点
#### 优点
- 高容错性：数据自动保存为多个副本，一个副本丢失后，集群会重新创建一份副本，提高容错性。
- 合适处理大数据：数据规模`能够处理GB、TB甚至PB级别的数据`；文件规模`能够处理百万规模以上的文件数量`
- 可构建在廉价的机器上，通过多副本机制，增强系统的高可用性
#### 缺点
- 不合适低延时数据访问，比如毫秒级别的数据响应速度，hdfs是做不到的
- 无法对大量小文件进行存储，NameNode存储文件目录和块信息需要固定内存`每个文件大概150byte`，大量存储小文件会耗尽NameNode的内存容量；每个文件读取时需要寻址时间，如果小文件的寻址时间超出读取时间，就违背了hdfs的设计目标。
- 不支持并发写入、文件随机修改，一个文件不允许多线程并发访问；仅支持数据append`追加`，不支持文件的`随机修改`。
### HDFS组织架构
#### NameNode：类似于一个master（管理员）
- 管理HDFS的名称空间(每个数据块的名字)
- 配置副本策略
- 管理数据块`block`的映射信息
- 处理client端的读写请求
#### DataNode：类似于slave，处理master的操作指令
- 数据块的实际存储地
- 执行对block的读写指令
#### client：MapReduce，就是用户自己编写的运算jar
- 文件切片：文件上传到HDFS时，client会将文件切分成一个个的Block。然后上传到DataNode。
- 与NameNode交互，获取Block在DataNode的位置信息
- 与DataNode交互，对Block进行读取/写入操作
- client提供一些命令来操作HDFS，比如NameNode格式化
- client可以通过一些命令来访问HDFS，比如对HDFS增删查改操作
#### Secondary NameNode：类似于NameNode的秘书，并非NameNode的热备
- 辅助NameNode，分担其工作量，比如定期合并Fsimage和Edits，并把结果推送给NameNode
- 在紧急情况下，可辅助恢复NameNode`集群会新建NameNode，并从NN2复制Fsimage和Edits，所以会丢失宕机前的一份Fsimage`
### HDFS文件块大小（面试重点）
HDFS中的文件在物理上是分块存储（Block），块的大小可以通过配置参数（dfs.blocksize）来规定，在Hadoop2.x/3.x版本中默认是128M，1.x版本是64M。
- 集群中的block有：block1、block2、block3、block4、....blockn
- 如果寻址时间约为10ms，即查找到目标block的时间为10ms
- 寻址时间为传输时间的1%时，则为最佳状态（专家结论）
- 目前磁盘的传输速率普遍为100MB/s
#### 思考：为什么块大小设置为128MB
- HDFS的Block设置太小，会增加总Block的寻址时间，程序一直在寻址Block的起始位置；
- 如果Block设置的太大，从磁盘传输数据的时间会明显大于定位这个Block开始位置所需的时间。导致增加程序读取文件所需的时间
- HDFS的Block大小设置主要取决于磁盘传输速率
#### 注意
如果磁盘是SSD，则传输速率普遍为200~300MB/s，所以如果服务器使用的是固态硬盘可以把Block大小设置为256M。反之，如果服务器是普通硬盘的话就要把Block大小设置为128M。
## HDFS的Shell操作（开发重点）
### 基本语法
hadoop fs 具体命令和hdfs dfs具体命令是完全相同的
### 命令大全
```shell script
# 查询Hadoop命令大全
hadoop fs

# 响应结果
Usage: hadoop fs [generic options]
	[-appendToFile <localsrc> ... <dst>]
	[-cat [-ignoreCrc] <src> ...]
	[-checksum <src> ...]
	[-chgrp [-R] GROUP PATH...]
	[-chmod [-R] <MODE[,MODE]... | OCTALMODE> PATH...]
	[-chown [-R] [OWNER][:[GROUP]] PATH...]
	[-copyFromLocal [-f] [-p] [-l] [-d] [-t <thread count>] <localsrc> ... <dst>]
	[-copyToLocal [-f] [-p] [-ignoreCrc] [-crc] <src> ... <localdst>]
	[-count [-q] [-h] [-v] [-t [<storage type>]] [-u] [-x] [-e] <path> ...]
	[-cp [-f] [-p | -p[topax]] [-d] <src> ... <dst>]
	[-createSnapshot <snapshotDir> [<snapshotName>]]
	[-deleteSnapshot <snapshotDir> <snapshotName>]
	[-df [-h] [<path> ...]]
	[-du [-s] [-h] [-v] [-x] <path> ...]
	[-expunge]
	[-find <path> ... <expression> ...]
	[-get [-f] [-p] [-ignoreCrc] [-crc] <src> ... <localdst>]
	[-getfacl [-R] <path>]
	[-getfattr [-R] {-n name | -d} [-e en] <path>]
	[-getmerge [-nl] [-skip-empty-file] <src> <localdst>]
	[-head <file>]
	[-help [cmd ...]]
	[-ls [-C] [-d] [-h] [-q] [-R] [-t] [-S] [-r] [-u] [-e] [<path> ...]]
	[-mkdir [-p] <path> ...]
	[-moveFromLocal <localsrc> ... <dst>]
	[-moveToLocal <src> <localdst>]
	[-mv <src> ... <dst>]
	[-put [-f] [-p] [-l] [-d] <localsrc> ... <dst>]
	[-renameSnapshot <snapshotDir> <oldName> <newName>]
	[-rm [-f] [-r|-R] [-skipTrash] [-safely] <src> ...]
	[-rmdir [--ignore-fail-on-non-empty] <dir> ...]
	[-setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec> <path>]]
	[-setfattr {-n name [-v value] | -x name} <path>]
	[-setrep [-R] [-w] <rep> <path> ...]
	[-stat [format] <path> ...]
	[-tail [-f] [-s <sleep interval>] <file>]
	[-test -[defsz] <path>]
	[-text [-ignoreCrc] <src> ...]
	[-touch [-a] [-m] [-t TIMESTAMP ] [-c] <path> ...]
	[-touchz <path> ...]
	[-truncate [-w] <length> <path> ...]
	[-usage [cmd ...]]
```
### 常用命令实战
```shell script
# -help：输出这个命令参数
hadoop fs -help mv

# 在HDFS上创建/sanguo目录
hadoop fs -mkdir /sanguo

# -moveFromLocal：从本地剪切liubei.txt文件，并粘贴到HDFS上的/sanguo目录
hadoop fs -moveFromLocal ./liubei.txt /sanguo

# -copyFromLocal：从本地文件系统中拷贝guanyu.txt文件到HDFS上的/sanguo目录
hadoop fs -copyFromLocal ./guanyu.txt /sanguo
# -put：等同于copyFromLocal，生产环境更习惯用put
hadoop fs -put guanyu.txt /sanguo

# -appendToFile：把zhangfei.txt文件到HDFS上已经存在的liubei.txt文件末尾
hadoop fs -appendToFile ./zhangfei.txt /sanguo/liubei.txt

# -copyToLocal：从HDFS拷贝liubei.txt到本地
hadoop fs -copyToLocal /sanguo/liubei.txt ./
# -get：等同于copyToLocal，生产环境更习惯用get
hadoop fs -get /sanguo/liubei.txt ./

# -ls: 显示HDFS的sanguo目录下的信息
hadoop fs -ls /sanguo

# -cat：显示liubei.txt文件内容
hadoop fs -cat liubei.txt

# -chgrp、-chmod、-chown：Linux文件系统中的用法一样，修改liubei.txt文件所属权限
hadoop fs -chmod 666 /sanguo/liubei.txt
hadoop fs -chown username:username /sanguo/liubei.txt

# -cp：从HDFS的sanguo目录下liubei.txt文件拷贝到xiyouji目录上
hadoop fs -cp /sanguo/liubei.txt /xiyouji

# -mv：在HDFS上的sanguo目录下的liubei.txt移动到/xiyouji目录下
hadoop fs -mv /sanguo/liubei.txt /xiyouji

# -tail：显示liubei.txt文件的末尾1kb的数据
hadoop fs -tail /sanguo/liubei.txt

# -rm：删除文件或文件夹；删除sanguo目录下的liubei.txt
hadoop fs -rm /sanguo/liubei.txt

# -rm -r：递归删除sanguo目录及目录里面内容
hadoop fs -rm -r /sanguo

# -du统计sanguo目录的大小信息
hadoop fs -du -s -h /sanguo
# 响应结果：8表示文件大小；24表示8*3个副本；/sanguo表示查看的目录
8  24  /sanguo

# -du统计sanguo目录下子文件列表的大小信息
hadoop fs -du -h /sanguo

# -setrep：设置HDFS中/sanguo/liubei.txt文件的副本数量
hadoop fs -setrep 10 /sanguo/liubei.txt
```
- 注意：这里设置的副本数只是记录在NameNode的元数据中，是否真的会有这么多副本，还得看DataNode的数量。因为目前只有3台设备，最多也就3个副本，只有节点数的增加到10台时，副本数才能达到10。
## HDFS的API案例实战
### 测试参数优先级
#### hadoop默认配置文件hdfs-site-default.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<configuration>
	<property>
        <!-- 设置副本数 -->
		<name>dfs.replication</name>
         <value>1</value>
	</property>
</configuration>
```
#### hadoop项目中etc/hadoop/hdfs-site.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<configuration>
	<property>
        <!-- 设置副本数 -->
		<name>dfs.replication</name>
         <value>2</value>
	</property>
</configuration>
```
#### 把hdfs-site.xml拷贝到项目的resource路径
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<configuration>
	<property>
        <!-- 设置副本数 -->
		<name>dfs.replication</name>
         <value>3</value>
	</property>
</configuration>
```
#### 编写硬编码方式
```java
public class Test{

    @Test
    public void testCopyFromLocalFile() throws IOException, InterruptedException, URISyntaxException {
    
        // 1 获取文件系统
        Configuration configuration = new Configuration();
        // 设置副本数
        configuration.set("dfs.replication", "4");
        FileSystem fs = FileSystem.get(new URI("hdfs://hadoop102:8020"), configuration, "atguigu");
    
        // 2 上传文件（参数1：本地文件路径， 参数2：hdfs存储路径）
        fs.copyFromLocalFile(new Path("d:/sunwukong.txt"), new Path("/xiyou/huaguoshan"));
    
        // 3 关闭资源
        fs.close();
    }
}
```
参数优先级排序：（1）客户端代码中设置的值 > （2）ClassPath下的用户自定义配置文件 > （3）服务器的自定义配置（xxx-site.xml） > （4）服务器的默认配置（xxx-default.xml）
## HDFS的读写流程(面试重点)
### HDFS写数据流程
#### 剖析文件上传流程
- 客户端通过Distributed FileSystem模块向`NameNode`请求上传文件，NameNode检查当前客户端是否有权限上传文件，检查父目录、目标文件是否存在。
- NameNode返回文件上传请求的响应结果`是否可以`。
- 客户端请求第一个`Block`上传到哪些`DataNode`服务器上。
- NameNode返回3个DataNode节点，分别为dn1、dn2、dn3。
- 客户端通过FSDataOutputStream模块请求向dn1上传数据，dn1收到请求会继续调用dn2，然后dn2调用dn3，将这个通信管道`dn1 -> dn2 -> dn3`建立完成。
- dn1、dn2、dn3`逐级应答`客户端：dn3 -> dn2 -> dn1
- 客户端开始往dn1上传第一个Block（先从磁盘读取数据放到一个本地内存缓存），FSDataOutputStream以`Packet`为单位向DataNode传输数据`每个Packet由多个chunk组成`，dn1收到一个Packet就会传给dn2，dn2传给dn3；dn1每传一个packet会放入一个`ack队列`等待应答。FSDataOutputStream会创建一个ack队列等待dn1的`ack响应`，如果FSDataOutputStream的ack队列中某个ack一直没有得到相应，客户端会认为这个Packet没有传输成功，会重新向dn1传输这个Packet。
- 当一个Block传输完成之后，客户端再次请求NameNode上传第二个Block的服务器。（重复执行3-7步）。
#### 网络拓扑-节点距离计算
在HDFS写数据的过程中，NameNode会选择距离待上传数据最近距离的DataNode接收数据。那么这个最近距离怎么计算呢？
- 节点距离：两个节点到达最近的共同祖先的距离总和。
```
                                    hadoop集群
  ----------------------------------------------------------------------------
 |             数据中心d1                             数据中心d2                |
 |  ---------------------------------     ---------------------------------   |
 | | 机架r1       机架r2      机架r3  |   | 机架r4       机架r5      机架r6  |  |
 | | -------     -------     ------- |   | -------     -------     ------- |  |
 | || ----- |   | ----- |   | ----- ||   || ----- |   | ----- |   | ----- ||  |
 | ||| n-0 ||   || n-0 ||   || n-0 |||   ||| n-0 ||   || n-0 ||   || n-0 |||  |   
 | || ----- |   | ----- |   | ----- ||   || ----- |   | ----- |   | ----- ||  |
 | || ----- |   | ----- |   | ----- ||   || ----- |   | ----- |   | ----- ||  |
 | ||| n-1 ||   || n-1 ||   || n-1 |||   ||| n-1 ||   || n-1 ||   || n-1 |||  |
 | || ----- |   | ----- |   | ----- ||   || ----- |   | ----- |   | ----- ||  |
 | || ----- |   | ----- |   | ----- ||   || ----- |   | ----- |   | ----- ||  |
 | ||| n-2 ||   || n-2 ||   || n-2 |||   ||| n-2 ||   || n-2 ||   || n-2 |||  |
 | || ----- |   | ----- |   | ----- ||   || ----- |   | ----- |   | ----- ||  |
 | | -------     -------     ------- |   | -------     -------     ------- |  |
 |  ---------------------------------     ---------------------------------   |
  ----------------------------------------------------------------------------

Distance(/d1/r1/n0, d1/r1/n0) = 0 | 表示同一个节点
Distance(/d1/r1/n0, d1/r1/n1) = 2 | 表示同一机架的不同节点
Distance(/d1/r1/n0, d1/r2/n0) = 4 | 表示不同机架上的节点
Distance(/d1/r1/n0, d2/r1/n0) = 6 | 表示不同数据中心的节点
  
```
#### 机架感知（副本存储节点选择）
##### 机架感知说明
- [官方说明](http://hadoop.apache.org/docs/r3.1.3/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html#Data_Replication)
```
# 第一个副本节点在本地机器上
one replica on the local machine

# 第二个副本节点在不同机架上
another replica on a node in a different (remote) rack

# 最后一个副本在第二个节点的同机架上的不同节点
the last on a different node in the same remote rack
```
- 源码说明

Crtl + n 查找BlockPlacementPolicyDefault，在该类中查找chooseTargetInOrder方法。
##### Hadoop3.1.3副本节点选择
- 第一个副本在client所处的节点上，如果client在集群外，随机选一个。
- 第二步副本在另一个机架的随机一个节点上
- 第三个副本在第二个副本所在机架的随机节点上
### HDFS读数据流程
- 客户端通过DistributedFileSystem向NameNode请求下载文件
- NameNode校验客户端是否有权限下载文件
- NameNode通过查询元数据，找到文件块所在的DataNode地址
- 挑选一台DataNode（就近原则+节点负载，然后随机）服务器，请求读取数据
- DataNode开始传输数据给客户端（从磁盘里面读取数据输入流，以Packet为单位来做校验）
- 客户端以Packet为单位接收，先在本地缓存，然后写入目标文件
## NameNode和Secondary NameNode
### NN和2NN工作机制
```                  
                                               NameNode                                                 Secondary NameNode
                              ------------------------------------------                    ------------------------------------------
                             |  --------------------------------------  | 1、请求是否需要  |          CheckPoint触发条件：              |
                             | | Mata(元数据) Tree                    | |    CheckPoint    |              ① 定时时间到                 |
                             | |（每个block占用150byte的Mata内存空间） |  | <--------------- |              ② Edits中的数据满了          |
                             | | 内存数据增删改                        |  | 2、请求执行      |            ----------------              |
                             |  --------------------------------------  |    CheckPoint    |           |      内存      |             |
                             |              ^   1.加载编辑日志   ^       | <--------------- |            ----------------              |
                             |  3.记录      |      和镜像文件    |       |                  | 7.生成新的   |           ^ 6.加载到内存    |
    --------                 |  操作日志    |      到内存        |       |                  |   Fsimage    V           |  并合并计算    |
   | client | -------------> |  -------------------     -----------     |                  |  -----------------     ---------         |
    -------- 2.Mata的增删改   | |edit_inprogress_001|   |  fsimage  | ---|-----------    ---|-|fsimage.checkpoit|   |         |        |
             ../hdfs/ss.avi  |  -------------------     -----------     |           |  |   |  -----------------    |         |        |
                             |        4.滚动正在 |     ^ 9.重命名Fsimage |           |  |   |  5.拷贝到2NN    ---------        |        |
                             |         的Edits   |    -|--------------- |8.拷贝到NN  ---|---|-------------> | fsimage |       |        |
                             |                   |   |fsimage.checkpoit||<-------------    |                 ---------       |        |
                             |                   V    ----------------- |                  |                                 |        |
                             |          -------------------             |    5.拷贝到2NN   |                         -----------       |
                             |         |     edits_001     | -----------|------------------|----------------------> | edits_001 |     |
                             |          -------------------             |                  |                         -----------      |
                             |          -------------------             |                  |                                          |
                             |         |edit_inprogress_002|            |                  |                                          |          
                             |          -------------------             |                  |                                          |
                              -------------------------------------------                   ------------------------------------------


```
#### 第一阶段：NameNode启动
- 第一次启动NameNode格式化后，创建`Fsimage`和`Edits`文件。如果不是第一次启动，直接加`载编辑日志`（Edits）和`镜像文件`（Fsimage）到内存。
    - Fsimage文件：存储着当前内存中的元数据
    - Edits文件：存储的是`上次更新滚动到现在`所有对元数据进行增删改的`操作日志`
- 客户端对元数据进行增删改的请求。
    - NameNode`追加`操作日志到Edits文件
- 更新滚动日志
    - 生产新的Edits文件`edit_inprogress_001`
    - 旧的Edits文件执行滚动更新操作
- NameNode在内存中对元数据进行增删改。
    - 在新的Edits文件中追加操作日志
#### 第二阶段：Secondary NameNode工作
- Secondary NameNode`定时询问`NameNode是否需要CheckPoint。直接带回NameNode是否检查的结果。
- Secondary NameNode请求执行CheckPoint。
- NameNode滚动正在写的Edits日志。
- 将滚动前的旧Edits文件和fsimage文件拷贝到Secondary NameNode。
- Secondary NameNode加载旧Edits文件和fsimage文件到内存，并`合并`。
    - 2NN把Fsimage文件的元数据`加载`到内存中
    - 2NN依次`读取`每一次对元数据增删改的操作日志
    - 2NN根据操作日志逻辑对元数据进行重新`计算`
- 生成新的镜像文件fsimage.chkpoint。
    - Edits文件和Fsimage文件合并完成后，把计算完成的元数据写入`fsimage.chkpoint`文件
- `拷贝`fsimage.chkpoint到NameNode。
- NameNode将fsimage.chkpoint重新命名成fsimage`覆盖`。
### Fsimage和Edits解析
NameNode在刚被格式化之后，hadoop会在数据目录`../data/tmp/dfs/name/current`下产生四个文件：fsimage_XXX、edits_XXX、seen_txid、VERSION
#### fsimage_XXX
HDFS文件系统元数据的一个`永久性的检查点`，其中包含HDFS文件系统的`所有目录`和`文件index`的序列化信息。
```xml
<fsimage>
    <version>
        <layoutVersion>-64</layoutVersion>
        <onDiskVersion>1</onDiskVersion>
        <oivRevision>ba631c436b806728f8ec2f54ab1e289526c90579</oivRevision>
    </version>
    <NameSection>
        <namespaceId>1481657005</namespaceId>
        <genstampV1>1000</genstampV1>
        <genstampV2>1001</genstampV2>
        <genstampV1Limit>0</genstampV1Limit>
        <lastAllocatedBlockId>1073741825</lastAllocatedBlockId>
        <txid>43</txid>
    </NameSection>
    <!-- 5大副本策略 -->
    <ErasureCodingSection>
        <erasureCodingPolicy>
            <policyId>1</policyId>
            <policyName>RS-6-3-1024k</policyName>
            <cellSize>1048576</cellSize>
            <policyState>DISABLED</policyState>
            <ecSchema>
                <codecName>rs</codecName>
                <dataUnits>6</dataUnits>
                <parityUnits>3</parityUnits>
            </ecSchema>
        </erasureCodingPolicy>
        <erasureCodingPolicy>
            <policyId>2</policyId>
            <policyName>RS-3-2-1024k</policyName>
            <cellSize>1048576</cellSize>
            <policyState>DISABLED</policyState>
            <ecSchema>
                <codecName>rs</codecName>
                <dataUnits>3</dataUnits>
                <parityUnits>2</parityUnits>
            </ecSchema>
        </erasureCodingPolicy>
        <erasureCodingPolicy>
            <policyId>3</policyId>
            <policyName>RS-LEGACY-6-3-1024k</policyName>
            <cellSize>1048576</cellSize>
            <policyState>DISABLED</policyState>
            <ecSchema>
                <codecName>rs-legacy</codecName>
                <dataUnits>6</dataUnits>
                <parityUnits>3</parityUnits>
            </ecSchema>
        </erasureCodingPolicy>
        <erasureCodingPolicy>
            <policyId>4</policyId>
            <policyName>XOR-2-1-1024k</policyName>
            <cellSize>1048576</cellSize>
            <policyState>DISABLED</policyState>
            <ecSchema>
                <codecName>xor</codecName>
                <dataUnits>2</dataUnits>
                <parityUnits>1</parityUnits>
            </ecSchema>
        </erasureCodingPolicy>
        <erasureCodingPolicy>
            <policyId>5</policyId>
            <policyName>RS-10-4-1024k</policyName>
            <cellSize>1048576</cellSize>
            <policyState>DISABLED</policyState>
            <ecSchema>
                <codecName>rs</codecName>
                <dataUnits>10</dataUnits>
                <parityUnits>4</parityUnits>
            </ecSchema>
        </erasureCodingPolicy>
    </ErasureCodingSection>
    <!-- 文件节点信息 -->
    <INodeSection>
        <lastInodeId>16393</lastInodeId>
        <numInodes>9</numInodes>
        <!-- 根节点 -->
        <inode>
            <id>16385</id>
            <type>DIRECTORY</type>
            <name/>
            <mtime>1661764599780</mtime>
            <permission>chaoyou:supergroup:0755</permission>
            <nsquota>9223372036854775807</nsquota>
            <dsquota>-1</dsquota>
        </inode>
        <!-- 临时目录 -->
        <inode>
            <id>16386</id>
            <type>DIRECTORY</type>
            <name>tmp</name>
            <mtime>1661698758870</mtime>
            <permission>chaoyou:supergroup:0770</permission>
            <nsquota>-1</nsquota>
            <dsquota>-1</dsquota>
        </inode>
        <!-- 自定义目录 -->
        <inode>
            <id>16392</id>
            <type>DIRECTORY</type>
            <name>sanguo</name>
            <mtime>1661765068764</mtime>
            <permission>chaoyou:supergroup:0755</permission>
            <nsquota>-1</nsquota>
            <dsquota>-1</dsquota>
        </inode>
        <!-- 自定义文件 -->
        <inode>
            <id>16393</id>
            <type>FILE</type>
            <name>liubei.txt</name>
            <replication>3</replication>
            <mtime>1661765068750</mtime>
            <atime>1661765067542</atime>
            <!-- 文件所在块的大小 -->
            <preferredBlockSize>134217728</preferredBlockSize>
            <permission>chaoyou:supergroup:0644</permission>
            <!-- 文件所在块列表 -->
            <blocks>
                <!-- 块的元数据 -->
                <block>
                    <id>1073741825</id>
                    <genstamp>1001</genstamp>
                    <numBytes>8</numBytes>
                </block>
            </blocks>
            <!-- 文件存储策略 -->
            <storagePolicyId>0</storagePolicyId>
        </inode>
    </INodeSection>
    <INodeReferenceSection/>
    <SnapshotSection>
        <snapshotCounter>0</snapshotCounter>
        <numSnapshots>0</numSnapshots>
    </SnapshotSection>
    <!-- 节点目录树 -->
    <INodeDirectorySection>
        <directory>
            <parent>16385</parent>
            <child>16392</child>
            <child>16386</child>
        </directory>
        <directory>
            <parent>16386</parent>
            <child>16387</child>
        </directory>
        <directory>
            <parent>16387</parent>
            <child>16388</child>
        </directory>
        <directory>
            <parent>16388</parent>
            <child>16389</child>
        </directory>
        <directory>
            <parent>16389</parent>
            <child>16390</child>
            <child>16391</child>
        </directory>
        <directory>
            <parent>16392</parent>
            <child>16393</child>
        </directory>
    </INodeDirectorySection>
    <FileUnderConstructionSection/>
    <!-- 加密管理 -->
    <SecretManagerSection>
        <currentId>0</currentId>
        <tokenSequenceNumber>0</tokenSequenceNumber>
        <numDelegationKeys>0</numDelegationKeys>
        <numTokens>0</numTokens>
    </SecretManagerSection>
    <!-- 缓存管理 -->
    <CacheManagerSection>
        <nextDirectiveId>1</nextDirectiveId>
        <numDirectives>0</numDirectives>
        <numPools>0</numPools>
    </CacheManagerSection>
</fsimage>
```
`思考`：可以看出，Fsimage中没有记录块所对应DataNode，为什么？
- 在集群启动后，要求DataNode上报数据块信息，并间隔一段时间后再次上报。
#### edits_XXX
存放HDFS文件系统的所有`更新操作`的路径，文件系统客户端执行的所有写操作日志首先会被记录到Edits文件中。
```xml
<EDITS>
    <EDITS_VERSION>-64</EDITS_VERSION>
    <RECORD>
        <OPCODE>OP_START_LOG_SEGMENT</OPCODE>
        <DATA>
            <TXID>16</TXID>
        </DATA>
    </RECORD>
    <RECORD>
        <OPCODE>OP_ADD</OPCODE>
        <DATA>
            <TXID>17</TXID>
            <LENGTH>0</LENGTH>
            <INODEID>16393</INODEID>
            <PATH>/sanguo/liubei.txt._COPYING_</PATH>
            <REPLICATION>3</REPLICATION>
            <MTIME>1661765067542</MTIME>
            <ATIME>1661765067542</ATIME>
            <BLOCKSIZE>134217728</BLOCKSIZE>
            <CLIENT_NAME>DFSClient_NONMAPREDUCE_2068756111_1</CLIENT_NAME>
            <CLIENT_MACHINE>192.168.228.201</CLIENT_MACHINE>
            <OVERWRITE>true</OVERWRITE>
            <PERMISSION_STATUS>
                <USERNAME>chaoyou</USERNAME>
                <GROUPNAME>supergroup</GROUPNAME>
                <MODE>420</MODE>
            </PERMISSION_STATUS>
            <ERASURE_CODING_POLICY_ID>0</ERASURE_CODING_POLICY_ID>
            <RPC_CLIENTID>d3232117-7775-4ab4-abd8-3294bbe4db2a</RPC_CLIENTID>
            <RPC_CALLID>3</RPC_CALLID>
        </DATA>
    </RECORD>
    <RECORD>
        <OPCODE>OP_ALLOCATE_BLOCK_ID</OPCODE>
        <DATA>
            <TXID>18</TXID>
            <BLOCK_ID>1073741825</BLOCK_ID>
        </DATA>
    </RECORD>
    <RECORD>
        <OPCODE>OP_SET_GENSTAMP_V2</OPCODE>
        <DATA>
            <TXID>19</TXID>
            <GENSTAMPV2>1001</GENSTAMPV2>
        </DATA>
    </RECORD>
    <RECORD>
        <OPCODE>OP_ADD_BLOCK</OPCODE>
        <DATA>
            <TXID>20</TXID>
            <PATH>/sanguo/liubei.txt._COPYING_</PATH>
            <BLOCK>
                <BLOCK_ID>1073741825</BLOCK_ID>
                <NUM_BYTES>0</NUM_BYTES>
                <GENSTAMP>1001</GENSTAMP>
            </BLOCK>
            <RPC_CLIENTID/>
            <RPC_CALLID>-2</RPC_CALLID>
        </DATA>
    </RECORD>
    <RECORD>
        <OPCODE>OP_CLOSE</OPCODE>
        <DATA>
            <TXID>21</TXID>
            <LENGTH>0</LENGTH>
            <INODEID>0</INODEID>
            <PATH>/sanguo/liubei.txt._COPYING_</PATH>
            <REPLICATION>3</REPLICATION>
            <MTIME>1661765068750</MTIME>
            <ATIME>1661765067542</ATIME>
            <BLOCKSIZE>134217728</BLOCKSIZE>
            <CLIENT_NAME/>
            <CLIENT_MACHINE/>
            <OVERWRITE>false</OVERWRITE>
            <BLOCK>
                <BLOCK_ID>1073741825</BLOCK_ID>
                <NUM_BYTES>8</NUM_BYTES>
                <GENSTAMP>1001</GENSTAMP>
            </BLOCK>
            <PERMISSION_STATUS>
                <USERNAME>chaoyou</USERNAME>
                <GROUPNAME>supergroup</GROUPNAME>
                <MODE>420</MODE>
            </PERMISSION_STATUS>
        </DATA>
    </RECORD>
    <RECORD>
        <OPCODE>OP_RENAME_OLD</OPCODE>
        <DATA>
            <TXID>22</TXID>
            <LENGTH>0</LENGTH>
            <SRC>/sanguo/liubei.txt._COPYING_</SRC>
            <DST>/sanguo/liubei.txt</DST>
            <TIMESTAMP>1661765068764</TIMESTAMP>
            <RPC_CLIENTID>d3232117-7775-4ab4-abd8-3294bbe4db2a</RPC_CLIENTID>
            <RPC_CALLID>9</RPC_CALLID>
        </DATA>
    </RECORD>
    <RECORD>
        <OPCODE>OP_END_LOG_SEGMENT</OPCODE>
        <DATA>
            <!-- 操作动作编号 -->
            <TXID>23</TXID>
        </DATA>
    </RECORD>
</EDITS>
```
思考：NameNode如何确定下次开机启动的时候合并哪些Edits？
```java
public class QuorumJournalManager implements JournalManager {
    @Override
    public EditLogOutputStream startLogSegment(long txId, int layoutVersion)
      throws IOException {
    Preconditions.checkState(isActiveWriter,
        "must recover segments before starting a new one");
    QuorumCall<AsyncLogger, Void> q = loggers.startLogSegment(txId,
        layoutVersion);
    loggers.waitForWriteQuorum(q, startSegmentTimeoutMs,
        "startLogSegment(" + txId + ")");
    return new QuorumOutputStream(loggers, txId, outputBufferCapacity,
        writeTxnsTimeoutMs, layoutVersion);
    }

    @Override
    synchronized public EditLogOutputStream startLogSegment(long txid,
      int layoutVersion) throws IOException {
    try {
      currentInProgress = NNStorage.getInProgressEditsFile(sd, txid);
      EditLogOutputStream stm = new EditLogFileOutputStream(conf,
          currentInProgress, outputBufferCapacity);
      stm.create(layoutVersion);
      return stm;
    } catch (IOException e) {
      LOG.warn("Unable to start log segment " + txid +
          " at " + currentInProgress + ": " +
          e.getLocalizedMessage());
      errorReporter.reportErrorOnFile(currentInProgress);
      throw e;
    }
    }

    @VisibleForTesting
      public static String getInProgressEditsFileName(long startTxId) {
        return getNameNodeFileName(NameNodeFile.EDITS_INPROGRESS, startTxId);
      }

    private static String getNameNodeFileName(NameNodeFile nnf, long txid) {
        return String.format("%s_%019d", nnf.getName(), txid);
      }
}
```
由源码可知NameNode是根据txid和layoutVersion确定要合并的Edits文件，layoutVersion则是fsimage文件的后缀版本号，txid是seen_txid文件的数字
#### seen_txid
```
# 当前接收操作日志的Edits文件编号，如：edits_inprogress_0000000000000000051
51
```
#### VERSION
VERSION文件保存的信息如下：
```
#Sat Sep 03 10:05:23 CST 2022
namespaceID=1481657005
clusterID=CID-9d4acb38-dce6-4851-b2c9-4a3aa8f03083
cTime=1661698288651
storageType=NAME_NODE
blockpoolID=BP-988932025-192.168.228.201-1661698288651
layoutVersion=-64
```
### CheckPoint时间设置
#### 通常情况下，SecondaryNameNode每隔一小时执行一次。
可以在`hdfs-default.xml`文件配置2NN的CheckPoint心跳
- 通常情况下，SecondaryNameNode每隔一小时执行一次。
```xml
<property>
  <name>dfs.namenode.checkpoint.period</name>
  <value>3600s</value>
</property>
```
- 一分钟检查一次操作次数，当操作次数达到1百万时，SecondaryNameNode执行一次。
```xml
<property>
  <name>dfs.namenode.checkpoint.txns</name>
  <value>1000000</value>
  <description>edits_inprogress_xxx文件记录操作动作次数</description>
</property>

<property>
  <name>dfs.namenode.checkpoint.check.period</name>
  <value>60s</value>
  <description> 1分钟检查一次操作次数</description>
</property>
```
## DataNode
数据节点：hadoop集群真正用于存储物理数据的机器
### DataNode工作机制
``` 
                      -------------------------------------------------
                     |                     NameNode                    |     
                ---->|   -------------------------------------------   |-----------
               | ----|  |                   元数据                  |  |<--------  |
               | |   |   -------------------------------------------   |         | |
               | |    -------------------------------------------------           
             1 | |                             ^ |                                X
               | | 2                           | |                               
               | |                           3 | | 4                             | |
               | |                             | |                            5  | |  6  
               | V                             | V                               | V
        -----------------                -----------------                -----------------
       |    DataNode1    |              |    DataNode2    |              |    DataNode3    |
       |  -------------  |              |  -------------  |              |  -------------  |
       | |    block1   | |              | |    block2   | |              | |    block3   | |
       |  -------------  |              |  -------------  |              |  -------------  |
       |  -------------  |              |  -------------  |              |  -------------  |
       | |    block2   | |              | |    block3   | |              | |    block1   | |
       |  -------------  |              |  -------------  |              |  -------------  |
        -----------------                -----------------                -----------------
Block中的内容：数据、数据长度、校验和（CheckSum）、时间戳

1：DataNode启动后会主动向NameNode会汇报自己所拥有的块信息
2：NameNode会响应已经收到DataNode的块信息
3：DataNode会定期（默认：6小时）扫描自己拥有的块信息
4：DataNode会定期（默认：6小时）向NameNode汇报自己的块信息
5：DataNode会和NameNode建立心跳（默认：3s）的方式确保通信正常，NameNode会响应给DataNode的操作命令
6：超过10分钟+10次心跳的时间没有收到DataNode的心跳，NameNode则认为该DataNode已经不可用。
```
- 1、一个数据块在DataNode上以文件形式存储在磁盘上，包括两个文件，一个是`数据本身`，一个是`元数据`包括数据块的长度，块数据的校验和，以及时间戳。
- 2、DataNode启动后自动向NameNode注册，通过后，后续以`周期性`（6小时）的向NameNode上报所有的块信息。
- 3、DataNode向NameNode汇报块信息之前，`会先扫描一遍自己拥有的块信息`。
```xml
<property>
	<name>dfs.blockreport.intervalMsec</name>
	<value>21600000</value>
	<description>Determines block reporting interval in milliseconds.</description>
</property>
```
DN扫描自己节点块信息列表的时间，默认6小时
```xml
<property>
	<name>dfs.datanode.directoryscan.interval</name>
	<value>21600s</value>
	<description>
        Interval in seconds for Datanode to scan data directories and 
            reconcile the difference between blocks in memory and on the disk.
	    Support multiple time unit suffix(case insensitive), as described
	        in dfs.heartbeat.interval.
	</description>
</property>
```
- 4、心跳是每3秒一次，心跳作用是`DataNode向NameNode发送自己运行状态`，并且心跳返回结果`带有NameNode给该DataNode的命令`，如复制块数据到另一台机器，或删除某个数据块。
- 5、如果超过`10分钟+30秒`没有收到某个DataNode的心跳，则认为该节点不可用。NameNode后续将不会再和这个DataNode交互，直到这个DataNode重新启动。
- 6、集群运行中可以安全加入和退出一些机器。

### 数据完整性
`思考`：如果DataNode中存储的数据是控制高铁信号灯的红灯信号（1）和绿灯信号（0），但是存储该数据的DataNode损坏了，一直显示是绿灯，是否很危险？如何确认DataNode中的block是否损坏？
#### DataNode解决方案
- 在DataNode创建Block时会对Block计算出一个`CheckSum`。
```java
@Metric MutableRate blockChecksumOp;
```
- 当DataNode读取Block的时候，它会计算`CheckSum`。
- 如果计算后的CheckSum，与Block创建时值不一样，说明Block已经损坏。
- Client读取其他DataNode上的Block。
- 常见的校验算法crc（32），md5（128），sha1（160）
- DataNode在其文件创建后`周期验证`CheckSum。
### 掉线时限参数设置
- DataNode进程死亡或者网络故障造成DataNode无法与NameNode通信
- NameNode不会立即把DataNode判定为死亡，要经过一段`超时时长`的时间
- HDFS默认的超时时长是10分钟+10次心跳
- 超时时长的计算公式：timeout = 2 * dfs.namenode.heartbeat.recheck-interval + 10 * dfs.heartbeat.interval
```xml
<property>
    <name>dfs.namenode.heartbeat.recheck-interval</name>
    <value>300000</value>
    <desciption>NameNode心跳复查间隔（默认单位：ms）</desciption>
</property>

<property>
    <name>dfs.heartbeat.interval</name>
    <value>3</value>
    <desciption>DataNode心跳间隔（默认单位：s）</desciption>
</property>

```
