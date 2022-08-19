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
