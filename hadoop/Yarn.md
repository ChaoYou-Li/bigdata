# Yarn资源调度器
`Yarn`是一个资源调度平台，负责为运算程序提供服务器运算资源，相当于一个分布式的`操作系统平台`，而`MapReduce`等运算程序则相当于运行于`操作系统之上的应用程序`。
## Yarn基础架构
YARN主要由`ResourceManager`、`NodeManager`、`ApplicationMaster`和`Container`等组件构成。
### ResourceManager（RM）
- 处理客户端请求
- 监控NodeManager
- 启动和监控ApplicationManager
- 资源分配和调度
### NodeManager（NM）
- 管理单个节点上资源
- 处理来自ResourceManager的命令
- 处理来自ApplicationManager的命令
### ApplicationManager（AM）
- 为应用程序申请资源，并把资源分配给内部任务（MapTask/ReduceTask）
- 任务（MapTask/ReduceTask）的监控和容错
### Container
Container是Yarn中的资源抽象，它封装了某个节点上的多维度资源，如内存、CPU、磁盘、网络等。
## Yarn工作机制
- MapReduce将job提交到客户端所在的节点
```java
// 7、提交
boolean completion = job.waitForCompletion(true);
```
- YarnRunner/LocalRunner会向ResourceManager申请一个Application
```java
// 6）提交Job,返回提交状态
status = submitClient.submitJob(jobId, submitJobDir.toString(), job.getCredentials());
    //（1）构造启动 MR AM 所需的所有信息
    ApplicationSubmissionContext appContext = createApplicationSubmissionContext(conf, jobSubmitDir, ts);
    //（2）创建 Application
    ApplicationId applicationId = resMgrDelegate.submitApplication(appContext);
    //（3）创建 MrAppMaster 并执行调度
    ApplicationReport appMaster = resMgrDelegate.getApplicationReport(applicationId);
```
- ResourceManager将Application所需的资源(临时文件路径、AppId、资源所在节点路径等)返回给YarnRunner/LocalRunner
- Application根据资源路径向HDFS申请输入资源，FileInputFormat会把输入资源切片放到临时文件中
- Application向HDFS提交完资源请求后，向ResourceManager申请运行MrAppMaster
- ResourceManager会将收到的请求初始化成Task，并存放到FIFO调度队列中
- ResourceManager为空闲NodeManager调度Task执行
- NodeManager会为MrAppMaster创建Container，并运行
- Container从HDFS临时文件中拷贝资源到本地
- MrAppMaster根据切片数量向ResourceManager申请MapTask资源
- ResourceManager把队列中的MapTask任务分配给NodeManager运行
- NodeManager为MapTask创建容器，并且运行
- MrAppMaster等待所有MapTask运行完毕后，向ResourceManager申请运行ReduceTask所需资源
- ReduceTask会根据自己的分区标识到MapTask对应的分区拉去数据
- 程序运行完毕，MrAppMaster会向ResourceManager申请注销自己
## Job提交全过程
### job提交
第一步：Client调用`job.waitForCompletion`方法，向整个集群提交`MapReduce`作业
```java
// 7、提交
boolean completion = job.waitForCompletion(true);
```
第二步：Client向RM申请一个作业id
```java
JobID jobId = submitClient.getNewJobID();
job.setJobID(jobId);
```
第三步：RM给Client返回该job资源的提交路径（临时存储路径）和作业Id
```java
JobID jobId = submitClient.getNewJobID();
job.setJobID(jobId);
Path submitJobDir = new Path(jobStagingArea, jobId.toString());
```
第四步：Client提交jar包（MapReduce）、切片信息（split）和配置文件（job.xml）到指定的资源提交路径（临时存储路径）。
```java
// 将作业jar放到临时存储文件
copyAndConfigureFiles(job, submitJobDir);

// 资源切片，并把切片放到临时存储文件
int maps = writeSplits(job, submitJobDir);

// 把作业配置参数job.xml放到临时存储文件
writeConf(conf, submitJobFile);
```
第五步：Client提交完资源后，向RM申请运行MrAppMaster。
```java
ApplicationSubmissionContext appContext = createApplicationSubmissionContext(conf, jobSubmitDir, ts);
ApplicationId applicationId = resMgrDelegate.submitApplication(appContext);
ApplicationReport appMaster = resMgrDelegate.getApplicationReport(applicationId);
```
### 作业初始化
第六步：当RM收到Client的请求后，将该job添加到容量调度器中。
第七步：某一个空闲的NM领取到该Job。
第八步：该NM创建Container，并产生MrAppMaster。
第九步：下载Client提交的资源到本地。
### 任务分配
第十步：MrAppMaster根据临时存储文件中的切片数量决定向RM申请运行多少个MapTask任务的资源。
第十一步：RM将运行MapTask任务分配给另外两个NodeManager，另两个NodeManager分别领取任务并创建容器。
### 任务运行
第十二步：MR向两个接收到任务的NodeManager发送程序启动脚本，这两个NodeManager分别启动MapTask，MapTask对数据分区排序。
第十三步：MrAppMaster等待所有MapTask运行完毕后，向RM申请容器，运行ReduceTask。
第十四步：ReduceTask向MapTask获取相应分区的数据。
第十五步：程序运行完毕后，MR会向RM申请注销自己。
### 进度和状态更新
YARN中的任务将其进度和状态(包括counter)返回给应用管理器, 客户端每秒(通过mapreduce.client.progressmonitor.pollinterval设置)向应用管理器请求进度更新, 展示给用户。
### 作业完成
除了向应用管理器请求作业进度外, 客户端每5秒都会通过调用waitForCompletion()来检查作业是否完成。时间间隔可以通过mapreduce.client.completion.pollinterval来设置。作业完成之后, 应用管理器和Container会清理工作状态。作业的信息会被转到作业历史服务器进行存储以备之后用户核查。
## Yarn调度器和调度算法
目前，Hadoop作业调度器主要有三种：`FIFO`、容量`Capacity Scheduler`和公平`Fair Scheduler`。
- Apache Hadoop3.1.3默认的资源调度器是Capacity Scheduler。
- CDH框架默认调度器是Fair Scheduler。
- 具体详情设置：yarn-default.xml
```xml
<property>
    <description>The class to use as the resource scheduler.</description>
    <name>yarn.resourcemanager.scheduler.class</name>
    <value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler</value>
</property>
```
### 先进先出调度器（FIFO Scheduler）
FIFO调度器（First In First Out）：单`队列`，根据提交作业的先后顺序，先来先服务。
```
                          put                -----------------     
             ------------------------------ | ResourceManager | 
             |                               -----------------
             |  -----                                |______________  pop
             | | job |                                              |              -----
             |  -----                                               V             | job | 
             |           --------------------------------------------------        -----           -------------
             |-------->     | jobn | ..... | job3 | job2 | job1 | job0 |     ---------------->    | NodeManager |
                         --------------------------------------------------                        -------------

```
- 优点：简单易懂；
- 缺点：不支持多队列，生产环境很少使用；
### 容量调度器（Capacity Scheduler）
Capacity Scheduler是`Yahoo`开发的多用户调度器。
#### 调度器特点
```
                          put                -----------------     
          --------------------------------- | ResourceManager | 
          |     -----                        -----------------
          |    | job |                             |_______________________  pop__________________
          |     -----                                                                             |
          V                                                                                       V
        -----            -----------------------------------------------------------            -----
       |  s  |-------->     | jobn | .....(20%资源) | job3 | job2 | job1 | job0 |     ------>  |  s  |     -----
       |  t  |           -----------------------------------------------------------           |  t  |    | job |
       |  r  |           -----------------------------------------------------------           |  r  |     -----       -------------
       |  a  |-------->     | jobn | .....(50%资源) | job3 | job2 | job1 | job0 |     ------>  |  a  | -------------> | NodeManager |
       |  t  |           -----------------------------------------------------------           |  t  |                 -------------
       |  e  |           -----------------------------------------------------------           |  e  |
       |  g  |-------->     | jobn | .....(30%资源) | job3 | job2 | job1 | job0 |     ------>  |  g  |
       |  y  |           -----------------------------------------------------------           |  y  |   
        -----                                                                                   -----

```
- 多队列：每个队列可配置一定的资源量，每个队列采用`FIFO调度策略`
- 容量保证：管理员可为每个队列`设置min、max资源量`
- 灵活性：如果一个队列的资源有剩余，可以暂时`共享`给其他需要`资源`的队列，而一旦该队列`有新的应用程序提交`，则其他队列借调的资源会`归还`给该队列
- 多租户：支持多用户共享集群和多应用程序同时运行。为了防止同一个用户的作业独占队列中的资源，调度器会对`同一个用户提交的作业所占资源量进行限定`。
#### 调度器资源分配算法
```
   root                                                                          
    |----queueA   20%
    |      |----user1
    |      |      |----job1
    |      |----user2                                                            ------
    |             |----job2                                                     | root |
    |----queueB   50%                                                            ------
    |----queueC   30%                            ___________________________________|__________________________________
           |----user3                           |                                   |                                  |
                  |----job3                     V                                   V                                  V
                  |----job4                  --------                           --------                           --------          
                                            | queueA |                         | queueB |                         | queueC |
                                             --------                           --------                           -------- 
                                   ______________|____________                                            _____________|_____________
                                  |                           |                                          |                           |
                                  V                           V                                          V                           V
                            -------------                -------------                             -------------                -------------
                           | job1(user1) |              | job2(user2) |                           | job3(user3) |              | job4(user3) |
                            -------------                -------------                             -------------                -------------
                                  |
                                  V
                          ----------------
                         |  ------------  |
                         | | Container1 | |
                         | | (MapTask)  | |
                         |  ------------  |
                         |  ------------  |
                         | | Container2 | |
                         | | (MapTask)  | |
                         |  ------------  |
                         |  ------------  |
                         | | Container2 | |
                         | |(ReduceTask)| |
                         |  ------------  |
                          ----------------
```
##### 队列资源分配
从root开始，使用`深度优先算法`，优先选择资源`占用率最低`的队列分配资源
##### 作业资源分配
默认按照提交作业的`优先级`和`提交时间`顺序分配资源
##### 容器资源分配
按照`容器的优先级`分配资源；如果优先级相同，按照`数据本地性`原则：
- 高：任务和数据在`同一节点`
- 中：任务和数据在`不同一节点` & `同一机架`
- 低：任务和数据在`不同一节点` & `不同一机架`
### 公平调度器（Fair Scheduler）
Fair Schedulere是`Facebook`开发的多用户调度器。
#### 调度器特点
```
                          put                -----------------     
          --------------------------------- | ResourceManager | 
          |     -----                        -----------------
          |    | job |                             |_______________________  pop__________________
          |     -----                                                                             |
          V                                                                                       V
        -----            -----------------------------------------------------------            -----
       |  s  |-------->     | jobn | .....(20%资源) | job3 | job2 | job1 | job0 |     ------>  |  s  |     -----
       |  t  |           -----------------------------------------------------------           |  t  |    | job |
       |  r  |           -----------------------------------------------------------           |  r  |     -----       -------------
       |  a  |-------->     | jobn | .....(50%资源) | job3 | job2 | job1 | job0 |     ------>  |  a  | -------------> | NodeManager |
       |  t  |           -----------------------------------------------------------           |  t  |                 -------------
       |  e  |           -----------------------------------------------------------           |  e  |
       |  g  |-------->     | jobn | .....(30%资源) | job3 | job2 | job1 | job0 |     ------>  |  g  |
       |  y  |           -----------------------------------------------------------           |  y  |   
        -----                                                                                   -----

```
##### 与容量调度器相同点
- 多队列：每个队列可配置一定的资源量，每个队列采用`FIFO调度策略`
- 容量保证：管理员可为每个队列`设置min、max资源量`
- 灵活性：如果一个队列的资源有剩余，可以暂时`共享`给其他需要`资源`的队列，而一旦该队列`有新的应用程序提交`，则其他队列借调的资源会`归还`给该队列
- 多租户：支持多用户共享集群和多应用程序同时运行。为了防止同一个用户的作业独占队列中的资源，调度器会对`同一个用户提交的作业所占资源量进行限定`。
##### 与容量调度器不同点
###### 核心调度策略不同
- 容量调度器：优先选择资源`利用率`低的队列；
- 公平调度器：优先选择对资源`缺额`比例大的队列
###### 每个队列定制资源分配方式
- 容量调度器：FIFO、DRF；
- 公平调度器：FIFO、FAIR、DRF
#### 调度器——缺额
```
         理想：
             ----------------------------------------
            |                 100M                   |
             ----------------------------------------
                                              | 20M  |
         现实：                               |< -- >|  
             -------------------------------- | 缺额 |
            |                 80M            ||      |
             -------------------------------- |      |
```
- 公平调度器设计目标：在时间尺度上，所有作业都能获得`公平的资源`。某一时刻一个作业`应获资源`和`实际获取资源`的差距叫`缺额`
- 调度器会`优先为缺额大的作业分配资源`
#### 队列资源分配
##### FIFO策略
公平调度器每个队列资源分配策略：如果选择`FIFO`，此时公平调度器`效果`相当于容量调度器
##### Fair策略
Fail策略（默认）是一种基于min/max公平算法实现的资源多路复用方式。默认情况下，每个队列内部采用该方式分配资源。这意味着，如果一个队列中同时运行的应用程序会平均分配资源它的资源`应用程序min/max的资源限定是前提`。
###### 资源分配流程（公平策略）
- 选择队列
- 选择作业
- 选择容器
```
                                        ---------------------------------------
                                       | 分别计算比较对象的（实际最小资源份额、|
                                       | 是否饥饿、资源分配比、资源使用权重比）|
                                        ---------------------------------------
                                                            |
                                                            |
                                                            V
                                                  /--------------------\
          ------------     其中有一个饥饿      /                          \
         |  饥饿优先  |  < ---------------  |    判断两种比较对象饥饿状态    |  --------------
          ------------                         \                          /                |
                                                  \--------------------/                   |
                                                            |                              |
                                                     都饥饿  |                             |
                                                            V                              |
                                               --------------------------                  |
                                              |     资源分配小者优先      |                  |
                                              | 相同，则按照提交时间顺序   |                 |
                                               --------------------------                  |
                                                                                           |
                                                                                           |
                                               --------------------------                  |
                                              | 资源使用权重比小者优先； |                   | 
                                              | 相同，则按照提交时间顺序 | <------------------                   
                                               --------------------------

> 实际最小资源份额：mindshare = Min（资源需求量、配置的最小资源）
> 是否饥饿：isNeedy = 资源使用量 < mindshare（实际最小资源份额）
> 资源分配比：minShareRadio = 资源使用量 / Max（mindshare，1）
> 资源使用权重比：useToWeightRadio = 资源使用量 / 权重
```
#### 资源分配算法
```
   root                                                                          
    |----queueA   20%
    |      |----user1
    |      |      |----job1
    |      |----user2                                                            ------
    |             |----job2                                                     | root |
    |----queueB   50%                                                            ------
    |----queueC   30%                            ___________________________________|__________________________________
           |----user3                           |                                   |                                  |
                  |----job3                     V                                   V                                  V
                  |----job4                  --------                           --------                           --------          
                                            | queueA |                         | queueB |                         | queueC |
                                             --------                           --------                           -------- 
                                   ______________|____________                                            _____________|_____________
                                  |                           |                                          |                           |
                                  V                           V                                          V                           V
                            -------------                -------------                             -------------                -------------
                           | job1(user1) |              | job2(user2) |                           | job3(user3) |              | job4(user3) |
                            -------------                -------------                             -------------                -------------
                                  |
                                  V
                          ----------------
                         |  ------------  |
                         | | Container1 | |
                         | | (MapTask)  | |
                         |  ------------  |
                         |  ------------  |
                         | | Container2 | |
                         | | (MapTask)  | |
                         |  ------------  |
                         |  ------------  |
                         | | Container2 | |
                         | |(ReduceTask)| |
                         |  ------------  |
                          ----------------
```
##### 队列资源分配
- 需求：集群总资源100，有三个队列（queueA、queueB、queueC）
- 资源分配：queueA -> 20、queueB -> 50、queueC -> 30
```
    第一次计算：
        avg：100 / 3 = 33.33
        queueA：33.33 -> +13.33
        queueB：33.33 -> -16.67
        queueC：33.33 -> +3.33

    第二次计算：
        avg：(13.33 + 3.33) / 1 = 16.66
        queueA：33.33 - 13.33 = 20
        queueB：33.33 + 16.66 = 50
        queueC：33.33 - 3.33 = 30
```
##### 作业资源分配
###### 不加权（关注点是Job的个数）
- 需求：有一条队列总资源数12个，有个4个job
- 资源分配：job1 -> 1，job2 -> 2，job3 -> 6，job4 -> 5
```
    第一次计算：
        avg：13 / 4 = 3
        job1：3 -> +2
        job2：3 -> +1
        job3：3 -> -3
        job4：3 -> -2

    第二次计算：
        avg：(2 + 1) / 2 = 1.5
        job1：3 - 1 = 2
        job2：3 - 2 = 1
        job3：3 + 1.5 = 4.5 -> -1.5
        job4：3 + 1.4 = 4.5 -> -0.5
    第n次计算：把空闲资源分配给job3和job4，直到它们俩运行完毕
```
###### 加权（关注点是Job的权重）
- 需求：有一条队列总资源数16个，有个4个job
- 资源分配：job1 -> 4，job2 -> 2，job3 -> 10，job4 -> 4
- 资源权重：job1 -> 5，job2 -> 8，job3 -> 1，job4 -> 2
```
    第一次计算：
        avg：16 / (5 + 8 + 1 + 2) = 1
        job1：5 * 1 -> +1
        job2：8 * 1 -> +6
        job3：1 * 1 -> -9
        job4：2 * 1 -> -2

    第二次计算：
        avg：(6 + 1) / (1 + 2) = 7/3
        job1：5 - 1 = 4
        job2：8 - 6 = 2
        job3：1 + 1 * 7/3 -> -6.67
        job4：2 + 2 * 7/3 -> +2.66
    第三次计算：
        avg：2.66 / 1 = 2.66
        job1：5 - 1 = 4
        job2：8 - 6 = 2
        job3：3.33 + 2.66 -> -4
        job4：2 + 2 * 7/3 - 2.66 = 4
    第n次计算：把空闲资源分配给job3，直到它运行完毕
```
##### DRF策略
DRF（Dominant Resource Fairness）前面所述的资源都是单一标准，例如只考虑内存（也是Yarn默认情况）。但是很多时候我们资源有很多种，例如`内存`，`CPU`，`网络带宽`等，这样我们很难衡量两个应用应该分配的`资源比例`。

- 那么在Yarn中，我们用DRF来决定如何调度：
例如集群一共有100CPU和10T内存，应用A需要（2CPU，300GB），应用B需要（6CPU，100GB）。则两个应用分别需要A（2%CPU，3%内存）和B（6%CPU，1%内存）的资源，这就意味着A是`内存主导`的，B是`CPU主导`的，针对这种情况，我们可以选择`DRF策略`对不同应用进行不同资源（CPU和内存）的一个不同比例的限制。
## Yarn常用命令
```shell script
# yarn application 查看任务
yarn application -list
# yarn application logs 查看应用日志
yarn logs -applicationId <ApplicationId>
# yarn container logs 查看容器日志
yarn logs -applicationId <ApplicationId> -containerId <ContainerId>
# yarn applicationattempt查看尝试运行的任务：列出所有Application尝试的列表
yarn applicationattempt -list <ApplicationId>
# yarn applicationattempt查看尝试运行的任务：打印ApplicationAttemp状态
yarn applicationattempt -status <ApplicationAttemptId>
# yarn container查看容器：列出所有Container
yarn container -list <ApplicationAttemptId>
# yarn container查看容器：打印Container状态
yarn container -status <ContainerId>
# yarn node查看节点状态：列出所有节点
yarn node -list -all
# yarn rmadmin更新配置：加载队列配置
yarn rmadmin -refreshQueues
# yarn queue查看队列：打印队列信息
yarn queue -status <QueueName>
```
## Yarn生产环境核心参数
### 默认配置
可以在yarn-default.xml文件上面进行查看
```xml
<configuration>
    <property>
        <description>设置ResourceManager使用的调度器，默认使用容量调度器</description>
        <name>yarn.resourcemanager.scheduler.class</name>
        <value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler</value>
    </property>

    <property>
        <description>ResourceManager处理器请求的线程数量，默认50</description>
        <name>yarn.resourcemanager.scheduler.client.thread-count</name>
        <value>50</value>
    </property>

    <property>
        <description>是否让yarn自己检测硬件进行配置，默认false</description>
        <name>yarn.nodemanager.resource.detect-hardware-capabilities</name>
        <value>false</value>
    </property>

    <property>
        <description>是否将虚拟核数当做CPU的物理核数，默认false</description>
        <name>yarn.nodemanager.resource.count-logical-processors-as-cores</name>
        <value>false</value>
    </property>

    <property>
        <description>虚拟核数和物理核数乘数，例如：4核8线程，该参数就应设置为2.0，默认1.0</description>
        <name>yarn.nodemanager.resource.pcores-vcores-multiplier</name>
        <value>1.0</value>
    </property>

    <property>
        <description>
            设置NodeManager使用内存；
            如果设置为-1，且yarn.nodemanager.resource.detect-hardware-capabilities = true，让yarn自己检测硬件进行配置；
            默认8GB
        </description>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>-1</value>
    </property>

    <property>
        <description>
            NodeManager为系统保留多少内存；
            如果设置为-1，且yarn.nodemanager.resource.memory-mb = -1，yarn.nodemanager.resource.detect-hardware-capabilities = true；
            默认：20% * (system memory - 2*HADOOP_HEAPSIZE)
        </description>
        <name>yarn.nodemanager.resource.system-reserved-memory-mb</name>
        <value>-1</value>
    </property>

    <property>
        <description>
            设置NodeManager使用CPU核数；
            如果设置为-1，且yarn.nodemanager.resource.detect-hardware-capabilities = true；
            默认：8个
        </description>
        <name>yarn.nodemanager.resource.cpu-vcores</name>
        <value>-1</value>
    </property>

    <property>
        <description>是否开启物理内存检查限制container，默认打开</description>
        <name>yarn.nodemanager.pmem-check-enabled</name>
        <value>true</value>
    </property>

    <property>
        <description>是否开启虚拟内存检查限制container，默认打开</description>
        <name>yarn.nodemanager.vmem-check-enabled</name>
        <value>true</value>
    </property>

    <property>
        <description>虚拟内存和物理内存的比例，默认2.1</description>
        <name>yarn.nodemanager.vmem-pmem-ratio</name>
        <value>2.1</value>
    </property>

    <property>
        <description>容器最小内存；默认值：1G</description>
        <name>yarn.scheduler.minimum-allocation-mb</name>
        <value>1024</value>
    </property>

    <property>
        <description>容器最大内存；默认值：8G</description>
        <name>yarn.scheduler.maximum-allocation-mb</name>
        <value>8192</value>
    </property>

    <property>
        <description>容器最小CPU核数；默认值：1</description>
        <name>yarn.scheduler.minimum-allocation-vcores</name>
        <value>1</value>
    </property>
    
    <property>
        <description>容器最大CPU核数；默认值：4</description>
        <name>yarn.scheduler.maximum-allocation-vcores</name>
        <value>4</value>
    </property>
</configuration>
```
# Yarn案例实操
## Yarn生产环境核心参数配置案例
### 需求
从1G数据中，统计每个单词出现次数。服务器3台，每台配置4G内存，4核CPU，4线程。
### 需求分析
```
    切片计算：数据总量 / Math.max(minSize, Math.min(maxSize, blockSize))
        blockSize = 128MB；minSize = -1；maxSize = Long.MAX_VALUE;
        1GB / 128MB = 8 split

    Task计算：MapTask + ReduceTask + MrAppMaster
        MapTask = 8
        ReduceTask = 1
        MrAppMaster = 1
    
    节点任务量计算：
        平均每个节点运行10个 / 3台 ≈ 3个任务（4	3	3）
```
### 自定义配置
可以在yarn-site.xml修改配置
```xml
<configuration>
    <!-- 选择调度器，默认容量 -->
    <property>
    	<description>The class to use as the resource scheduler.</description>
    	<name>yarn.resourcemanager.scheduler.class</name>
    	<value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler</value>
    </property>
    
    <!-- ResourceManager处理调度器请求的线程数量,默认50；如果提交的任务数大于50，可以增加该值，但是不能超过3台 * 4线程 = 12线程（去除其他应用程序实际不能超过8） -->
    <property>
    	<description>Number of threads to handle scheduler interface.</description>
    	<name>yarn.resourcemanager.scheduler.client.thread-count</name>
    	<value>8</value>
    </property>
    
    <!-- 是否让yarn自动检测硬件进行配置，默认是false，如果该节点有很多其他应用程序，建议手动配置。如果该节点没有其他应用程序，可以采用自动 -->
    <property>
    	<description>Enable auto-detection of node capabilities such as
    	memory and CPU.
    	</description>
    	<name>yarn.nodemanager.resource.detect-hardware-capabilities</name>
    	<value>false</value>
    </property>
    
    <!-- 是否将虚拟核数当作CPU核数，默认是false，采用物理CPU核数 -->
    <property>
    	<description>Flag to determine if logical processors(such as
    	hyperthreads) should be counted as cores. Only applicable on Linux
    	when yarn.nodemanager.resource.cpu-vcores is set to -1 and
    	yarn.nodemanager.resource.detect-hardware-capabilities is true.
    	</description>
    	<name>yarn.nodemanager.resource.count-logical-processors-as-cores</name>
    	<value>false</value>
    </property>
    
    <!-- 虚拟核数和物理核数乘数，默认是1.0 -->
    <property>
    	<description>Multiplier to determine how to convert phyiscal cores to
    	vcores. This value is used if yarn.nodemanager.resource.cpu-vcores
    	is set to -1(which implies auto-calculate vcores) and
    	yarn.nodemanager.resource.detect-hardware-capabilities is set to true. The	number of vcores will be calculated as	number of CPUs * multiplier.
    	</description>
    	<name>yarn.nodemanager.resource.pcores-vcores-multiplier</name>
    	<value>1.0</value>
    </property>
    
    <!-- NodeManager使用内存数，默认8G，修改为4G内存 -->
    <property>
    	<description>Amount of physical memory, in MB, that can be allocated 
    	for containers. If set to -1 and
    	yarn.nodemanager.resource.detect-hardware-capabilities is true, it is
    	automatically calculated(in case of Windows and Linux).
    	In other cases, the default is 8192MB.
    	</description>
    	<name>yarn.nodemanager.resource.memory-mb</name>
    	<value>4096</value>
    </property>
    
    <!-- nodemanager的CPU核数，不按照硬件环境自动设定时默认是8个，修改为4个 -->
    <property>
    	<description>Number of vcores that can be allocated
    	for containers. This is used by the RM scheduler when allocating
    	resources for containers. This is not used to limit the number of
    	CPUs used by YARN containers. If it is set to -1 and
    	yarn.nodemanager.resource.detect-hardware-capabilities is true, it is
    	automatically determined from the hardware in case of Windows and Linux.
    	In other cases, number of vcores is 8 by default.</description>
    	<name>yarn.nodemanager.resource.cpu-vcores</name>
    	<value>4</value>
    </property>
    
    <!-- 容器最小内存，默认1G -->
    <property>
    	<description>The minimum allocation for every container request at the RM	in MBs. Memory requests lower than this will be set to the value of this	property. Additionally, a node manager that is configured to have less memory	than this value will be shut down by the resource manager.
    	</description>
    	<name>yarn.scheduler.minimum-allocation-mb</name>
    	<value>1024</value>
    </property>
    
    <!-- 容器最大内存，默认8G，修改为2G -->
    <property>
    	<description>The maximum allocation for every container request at the RM	in MBs. Memory requests higher than this will throw an	InvalidResourceRequestException.
    	</description>
    	<name>yarn.scheduler.maximum-allocation-mb</name>
    	<value>2048</value>
    </property>
    
    <!-- 容器最小CPU核数，默认1个 -->
    <property>
    	<description>The minimum allocation for every container request at the RM	in terms of virtual CPU cores. Requests lower than this will be set to the	value of this property. Additionally, a node manager that is configured to	have fewer virtual cores than this value will be shut down by the resource	manager.
    	</description>
    	<name>yarn.scheduler.minimum-allocation-vcores</name>
    	<value>1</value>
    </property>
    
    <!-- 容器最大CPU核数，默认4个，修改为2个 -->
    <property>
    	<description>The maximum allocation for every container request at the RM	in terms of virtual CPU cores. Requests higher than this will throw an
    	InvalidResourceRequestException.</description>
    	<name>yarn.scheduler.maximum-allocation-vcores</name>
    	<value>2</value>
    </property>
    
    <!-- 虚拟内存检查，默认打开，修改为关闭 -->
    <property>
    	<description>Whether virtual memory limits will be enforced for
    	containers.</description>
    	<name>yarn.nodemanager.vmem-check-enabled</name>
    	<value>false</value>
    </property>
    
    <!-- 虚拟内存和物理内存设置比例,默认2.1 -->
    <property>
    	<description>Ratio between virtual memory to physical memory when	setting memory limits for containers. Container allocations are	expressed in terms of physical memory, and virtual memory usage	is allowed to exceed this allocation by this ratio.
    	</description>
    	<name>yarn.nodemanager.vmem-pmem-ratio</name>
    	<value>2.1</value>
    </property>
</configuration>
```
#### 关闭虚拟内存检查的原因
```
        
                                                    服务器4G内存
                                 ------------------------
                                |   容器限制4G物理内存   |
                                | ---------------------- |
                物理内存        ||    实际物理内存4G    ||
                                | ---------------------- |
                                 ------------------------

                                        4G * 2.1 = 8.4G
                                 -------------------------------------------------------------
                                |     ----------------   |     -----------------------------  |
                虚拟内存        |    |  Java堆内存4G  |  |    | Linux系统为Java进程预留5.4G | |
                                |     ----------------   |     -----------------------------  |
                                 -------------------------------------------------------------

由上图可知，如果开启虚拟内存检查，Linux系统给Java堆分配内存规则：2.1 * JVM_HEAP = 4G
所以如果想要给JVM_HEAP = 4G，则需要8.4G的物理内存，如果4G物理内存，则JVM_HEAP=1.9G
```
### 分发配置
注意：如果集群的硬件资源不一致，要每个NodeManager单独配置
### 重启集群
修改完yarn-site.xml配置文件后需要重新启动ResourceManager节点
```shell script
# 关闭yarn
../sbin/stop-yarn.sh
# 启动yarn
../sbin/start-yarn.sh
```
### 执行应用程序
执行一个WordCount程序，验证配置是否成功
```shell script
hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.3.jar wordcount /input /output
```
### 在客户端观察yarn任务执行情况
http://hadoop103:8088/cluster/apps
## 容量调度器多队列提交案例
生产环境创建队列的依据
- 调度器默认就1个`default`队列，不能满足生产要求。
- 按照框架：hive、spark、flink 每个框架的任务放入指定的队列（企业用的不是特别多）
- 按照业务模块：登录注册、购物车、下单、业务部门1、业务部门2

创建多队列的优点
- 可以避免因为队列中某个job出现代码事故（递归、死循环），把所有资源全部耗尽。
- 实现任务的`降级`使用，特殊时期（1111，618）保证重要的任务队列资源充足。
业务部门1（重要） > 业务部门2（比较重要） > 下单（一般） > 购物车（一般） > 登录注册（次要）
### 需求
- 需求1：`default`队列占总内存的40%，最大资源容量占总资源60%，`hive`队列占总内存的60%，最大资源容量占总资源80%。
- 需求2：配置队列优先级
### 配置多队列的容量调度器
#### 在capacity-scheduler.xml中配置如下
- 修改如下配置
```xml
<configuration>
    <!-- 指定多队列，增加hive队列 -->
    <property>
        <name>yarn.scheduler.capacity.root.queues</name>
        <value>default,hive</value>
        <description>The queues at the this level (root is the root queue).</description>
    </property>
    
    <!-- 降低default队列资源额定容量为40%，默认100% -->
    <property>
        <name>yarn.scheduler.capacity.root.default.capacity</name>
        <value>40</value>
    </property>
    
    <!-- 降低default队列资源最大容量为60%，默认100% -->
    <property>
        <name>yarn.scheduler.capacity.root.default.maximum-capacity</name>
        <value>60</value>
    </property>

    <!-- 指定hive队列的资源额定容量 -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.capacity</name>
        <value>60</value>
    </property>
    
    <!-- 用户最多可以使用队列多少资源，1表示100% -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.user-limit-factor</name>
        <value>1</value>
    </property>
    
    <!-- 指定hive队列的资源最大容量 -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.maximum-capacity</name>
        <value>80</value>
    </property>
    
    <!-- 启动hive队列 -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.state</name>
        <value>RUNNING</value>
    </property>
    
    <!-- 哪些用户有权向队列提交作业 -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.acl_submit_applications</name>
        <value>*</value>
    </property>
    
    <!-- 哪些用户有权操作队列，管理员权限（查看/杀死） -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.acl_administer_queue</name>
        <value>*</value>
    </property>
    
    <!-- 哪些用户有权配置提交任务优先级 -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.acl_application_max_priority</name>
        <value>*</value>
    </property>
    
    <!-- 任务的超时时间设置：yarn application -appId appId -updateLifetime Timeout
    参考资料：https://blog.cloudera.com/enforcing-application-lifetime-slas-yarn/ -->
    
    <!-- 
        如果application指定了超时时间，则提交到该队列的application能够指定的最大超时时间不能超过该值。 
        -1表示没有超时时间
    -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.maximum-application-lifetime</name>
        <value>-1</value>
    </property>
    
    <!-- 
        如果application没指定超时时间，则用default-application-lifetime作为默认值 
        -1表示没有超时时间
    -->
    <property>
        <name>yarn.scheduler.capacity.root.hive.default-application-lifetime</name>
        <value>-1</value>
    </property>
</configuration>
```
- 把修改完的配置文件分发给集群中所有的机器
- 重启Yarn或者执行yarn rmadmin -refreshQueues刷新队列
```shell script
# 关闭yarn
../sbin/stop-yarn.sh
# 启动yarn
../sbin/start-yarn.sh

# 刷新队列
yarn rmadmin -refreshQueues
```
### 向Hive队列提交任务
- 命令行方式
```shell script
# 注: -D表示运行时改变参数值
hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.3.jar wordcount -D mapreduce.job.queuename=hive /input /output
```
- 硬编码方式
默认的任务提交都是提交到`default`队列的。如果希望向其他队列提交任务，需要在Driver代码中显式声明
```java
public class WcDrvier {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        Configuration conf = new Configuration();
        // 设置当前job提交到hive队列的配置
        conf.set("mapreduce.job.queuename","hive");

        //1. 获取一个Job实例
        Job job = Job.getInstance(conf);

        // .............

        //6. 提交Job
        boolean b = job.waitForCompletion(true);
        System.exit(b ? 0 : 1);
    }
}
```
### 任务优先级
容量调度器，支持配置任务的`优先级`。在资源紧张时，优先级高的任务将`优先获取资源`。默认情况，Yarn将所有任务的优先级限制为`0`。若想使用任务的优先级功能，须开放该限制。
- 修改yarn-site.xml文件，增加以下参数
```xml
<property>
    <name>yarn.cluster.max-application-priority</name>
    <value>5</value>
</property>
```
- 分发配置，并重启Yarn
```shell script
# 执行分发脚本，xsync这个脚本需要自己编写
xsync yarn-site.xml
# 关闭yarn
../sbin/stop-yarn.sh
# 启动yarn
../sbin/start-yarn.sh
```
- 模拟资源紧张环境，可连续提交以下任务，直到新提交的任务申请不到资源为止
```shell script
hadoop jar /opt/module/hadoop-3.1.3/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.3.jar pi 5 2000000
```
- 再次重新提交优先级高的任务
```shell script
hadoop jar /opt/module/hadoop-3.1.3/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.3.jar pi  -D mapreduce.job.priority=5 5 2000000
```
- 也可以通过以下命令修改正在执行的任务的优先级
```shell script
# ApplicationID应用标识，Priority优先级（12345....）
yarn application -appID <ApplicationID> -updatePriority <Priority>
```
## 公平调度器案例
### 需求
- 创建两个队列，分别是test和atguigu（以用户所属组命名）。期望实现以下效果：若用户提交任务时指定队列，则任务提交到指定队列运行；若未指定队列，test用户提交的任务到root.group.test队列运行，atguigu提交的任务到root.group.atguigu队列运行（注：group为用户所属组）。
- 公平调度器的配置涉及到两个文件，一个是yarn-site.xml，另一个是公平调度器队列分配文件fair-scheduler.xml（文件名可自定义）。
```
# 配置文件参考资料：
https://hadoop.apache.org/docs/r3.1.3/hadoop-yarn/hadoop-yarn-site/FairScheduler.html
# 任务队列放置规则参考资料：
https://blog.cloudera.com/untangling-apache-hadoop-yarn-part-4-fair-scheduler-queue-basics/
```
### 配置多队列的公平调度器
- 修改yarn-site.xml文件，加入以下参数
```xml
<property>
    <name>yarn.resourcemanager.scheduler.class</name>
    <value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.FairScheduler</value>
    <description>配置使用公平调度器</description>
</property>

<property>
    <name>yarn.scheduler.fair.allocation.file</name>
    <value>/opt/module/hadoop-3.1.3/etc/hadoop/fair-scheduler.xml</value>
    <description>指明公平调度器队列分配配置文件</description>
</property>

<property>
    <name>yarn.scheduler.fair.preemption</name>
    <value>false</value>
    <description>禁止队列间资源抢占</description>
</property>
```
- 配置fair-scheduler.xml
```xml
<?xml version="1.0"?>
<allocations>
  <!-- 单个队列中Application Master占用资源的最大比例,取值0-1 ，企业一般配置0.1 -->
  <queueMaxAMShareDefault>0.5</queueMaxAMShareDefault>
  <!-- 单个队列最大资源的默认值：内存4GB，CPU4核 -->
  <queueMaxResourcesDefault>4096mb,4vcores</queueMaxResourcesDefault>

  <!-- 自带一个默认的队列：default -->

  <!-- 增加一个队列：test -->
  <queue name="test">
    <!-- 队列最小资源：内存2GB，CPU2核 -->
    <minResources>2048mb,2vcores</minResources>
    <!-- 队列最大资源：内存4GB，CPU4核 -->
    <maxResources>4096mb,4vcores</maxResources>
    <!-- 队列中最多同时运行的应用数，默认50，根据线程数配置 -->
    <maxRunningApps>4</maxRunningApps>
    <!-- 队列中Application Master占用资源的最大比例 -->
    <maxAMShare>0.5</maxAMShare>
    <!-- 该队列资源权重,默认值为1.0 -->
    <weight>1.0</weight>
    <!-- 队列内部的资源分配策略 -->
    <schedulingPolicy>fair</schedulingPolicy>
  </queue>

  <!-- 增加一个队列：atguigu -->
  <queue name="atguigu" type="parent">
    <!-- 队列最小资源：内存2GB，CPU2核 -->
    <minResources>2048mb,2vcores</minResources>
    <!-- 队列最大资源：内存4GB，CPU4核 -->
    <maxResources>4096mb,4vcores</maxResources>
    <!-- 队列中最多同时运行的应用数，默认50，根据线程数配置 -->
    <maxRunningApps>4</maxRunningApps>
    <!-- 队列中Application Master占用资源的最大比例 -->
    <maxAMShare>0.5</maxAMShare>
    <!-- 该队列资源权重,默认值为1.0 -->
    <weight>1.0</weight>
    <!-- 队列内部的资源分配策略 -->
    <schedulingPolicy>fair</schedulingPolicy>
  </queue>

  <!-- 任务队列分配策略,可配置多层规则,从第一个规则开始匹配,直到匹配成功 -->
  <queuePlacementPolicy>
    <!-- 提交任务时指定队列,如未指定提交队列,则继续匹配下一个规则; false表示：如果指定队列不存在,不允许自动创建-->
    <rule name="specified" create="false"/>
    <!-- 提交到root.group.username队列,若root.group不存在,不允许自动创建；若root.group.user不存在,允许自动创建 -->
    <rule name="nestedUserQueue" create="true">
        <rule name="primaryGroup" create="false"/>
    </rule>
    <!-- 最后一个规则必须为reject或者default。Reject表示拒绝创建提交失败，default表示把任务提交到default队列 -->
    <rule name="reject" />
  </queuePlacementPolicy>
</allocations>
```
- 分发配置并重启Yarn
```shell script
# 分发配置文件
xsync yarn-site.xml
xsync fair-scheduler.xml

# 重启yarn
../sbin/stop-yarn.sh
../sbin/start-yarn.sh
```
### 测试提交任务
- 提交任务时指定队列，按照配置规则，任务会到指定的root.test队列 
```shell script
hadoop jar ../hadoop-3.1.3/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.3.jar pi -Dmapreduce.job.queuename=root.test 1 1
```
- 提交任务时不指定队列，按照配置规则，任务会到root.atguigu.atguigu队列
```shell script
hadoop jar ../hadoop-3.1.3/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.3.jar pi 1 1
```
## Yarn的Tool接口案例
### 需求
自己写的程序也可以动态修改参数。编写Yarn的Tool接口。
### 具体步骤
- 创建类WordCount并实现Tool接口
```java
public class WordCountTool implements Tool {

    private Configuration config;

    @Override
    public int run(String[] args) throws Exception {

        // 初始化作业配置参数
        Job job = Job.getInstance(config);

        // 关联作业的jar包
        job.setJarByClass(WordCountDriver.class);

        // 管理Mapper和Reducer类
        job.setMapperClass(WordCountMapper.class);
        job.setReducerClass(WordCountReducer.class);

        // 关联Mapper的输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        // 关联Reducer的输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // 关联Mapper的输入路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        // 关联Reducer的输出路径
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        // 提交作业
        return job.waitForCompletion(true) ? 0 : 1;
    }

    @Override
    public void setConf(Configuration conf) {
        this.config = conf;
    }

    @Override
    public Configuration getConf() {
        return this.config;
    }

    /**
     * MapTask
     */
    public static class WordCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

        private Text outK = new Text();
        private IntWritable outV = new IntWritable(1);

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();
            String[] words = line.split(" ");

            for (String word : words) {
                outK.set(word);

                context.write(outK, outV);
            }
        }
    }

    /**
     * ReduceTask
     */
    public static class WordCountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable outV = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {

            int sum = 0;

            for (IntWritable value : values) {
                sum += value.get();
            }
            outV.set(sum);

            context.write(key, outV);
        }
    }
}
```
- 新建WordCountDriver
```java
public class WordCountDriver {

    private static Tool tool;

    public static void drive(String[] args) throws Exception {
        // 1. 创建配置文件
        Configuration conf = new Configuration();

        // 2. 判断是否有tool接口
        switch (args[0]){
            case "wordcount":
                tool = new WordCountTool();
                break;
            default:
                throw new RuntimeException(" No such tool: "+ args[0] );
        }
        // 3. 用Tool执行程序
        // Arrays.copyOfRange 将老数组的元素放到新数组里面
        // 最后两个参数必须是input和output
        int run = ToolRunner.run(conf, tool, Arrays.copyOfRange(args, args.length - 2, args.length));

        System.exit(run);
    }

}
```
