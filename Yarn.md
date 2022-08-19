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
