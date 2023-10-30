# kafka核心技术与实战
Kafka是由Apache软件基金会开发的一个开源流处理平台，由Scala和Java编写。Kafka是一种高吞吐量的分布式发布订阅消息系统，它可以处理消费者在网站中的所有动作流数据。 这种动作（网页浏览，搜索和其他用户的行动）是在现代网络上的许多社会功能的一个关键因素。 这些数据通常是由于吞吐量的要求而通过处理日志和日志聚合来解决。 对于像Hadoop一样的日志数据和离线分析系统，但又要求实时处理的限制，这是一个可行的解决方案。Kafka的目的是通过Hadoop的并行加载机制来统一线上和离线的消息处理，也是为了通过集群来提供实时的消息。
## kafka入门
### kafka是消息引擎系统
Apache Kafka 是一款开源的**消息引擎系统**。在国外有一个专门的名字叫 Messaging System，而在国内的书籍很多作者翻译成为“消息系统”。这是在片面强调消息主题的作用，而忽视了kafka该有的消息传递属性，kafka消息传递就像引擎一样，为生产端和消费端源源不断地传递着消息。
```

            ----------      produce    -------    consume     ----------
           | System A |   ----------> | kafka | -----------> | System B |
            ----------                 -------                ----------
            
```
#### 消息引擎传输的对象是消息
思考一个问题：既然消息引擎(kafka)是用来在不同系统之间进行消息传递的，那么如何设计这个消息格式才能保证在不同系统之间传递的可重用性和通用性？

| 厂商       | 序列化框架           |
|----------|-----------------|
| Google   | Protocol Buffer |
| Facebook | Thrift          |
| kafka    | 二进制的字节序列（byte）  |
#### 消息引擎传递消息的方式
如何传输消息属于消息引擎设计机制的一部分
##### 点对点模型
日常生活打电话就属于这种模型：A呼B的号码，只有B能接收到电话请求，其他人接收不到A的电话请求。
```

            ----------      produce    -------    consume     ----------
           | System A |   ----------> | kafka | -----------> | System B |
            ----------                 -------                ----------
            
```
##### 发布/订阅模型
它有一个主题（Topic）的概念，你可以理解成逻辑语义相近的消息容器。该模型也有发送方和接收方，只不过提法不同。发送方也称为发布者（Publisher），接收方称为订阅者（Subscriber）。

**日常案例**：一个或者多个报社向天涯报刊(topic)发布报纸，报刊会根据订阅本报刊的人群推送报纸。
```


            -------------                                                            --------------
           | Publisher 1 |----                   -----------              --------->| Subscriber 1 |
            -------------     |                 |   kafka   |            |           --------------
                              |    produce      |  -------  |   consume  |       
              .........   ----|---------------> | | topic | | -----------|--------->   ..........
                              |                 |  -------  |            |
            -------------     |                  -----------             |           --------------
           | Publisher N |----                                            --------->| Subscriber N |
            -------------                                                            --------------
            
```
#### 消息引擎的作用
为什么系统 A 不能直接发送消息给系统 B，中间还要隔一个消息引擎呢？

**答案**：削峰填谷，所谓的“削峰填谷”就是指缓冲上下游瞬时突发流量，使其更平滑。特别是对于那种发送能力很强的上游系统，如果没有消息引擎的保护，“脆弱”的下游系统可能会直接被压垮导致全链路服务“雪崩”。

### kafka术语
#### broker
kafka 的服务端运行时会产生一个 broker 服务进程，故一个 kafka 集群是由多个 broker 组成，broker 负责接收和处理客户端发送过来的请求，以及对消息进行持久化。一台服务器可以运行多个 broker 进程，但是为了 kafka 集群的高可用性，常见的做法是把 broker 分散运行在不同的机器上。
#### topic
topic 是承载消息的逻辑容器，在实际使用中多用来区分具体的业务。比如支付类消息和订单类消息分别使用不同的 topic 来区分各自的业务类型。
#### partition
- 每个 topic 会划分一个或多个 partition，它是一组有序的消息日志，可以理解为是存储**消息** 的队列
- 生产者发布的**消息**只能存储到 topic 中某个 partition，类似抢锁机制
- **消息**在中 partition 的编号（offset）是从 0 开始
#### offset
偏移量，即**消息**在 partition 中的位置编号，offset 在 partition 中的编号一旦确定就不会发生改变。后面尽量的消息都是以追加的方式写入 partition，故 offset 是单调递增的。
#### replica
副本（replica），kafka 为了保证高可用性，提供两个策略：一个是集群部署时 broker 运行在不同的机器上；一个是 partition 的副本维护。即每个 partition 都会有一个或多个副本，其中只能有 1 个领导副本（leader replica）和 N-1 个追随副本（follower replica）。每个 partition 的多个副本会分别散落在不同的 broker 上。

- 领导副本：负责 partition 对**消息**的读写操作，当领导副本损坏，可通过选举机制从追随副本选出新的领导副本
- 追随副本：负责从领导副本那边同步**消息**数据（会存在一定的时间延时）

**问题思考**：为什么 kafka 副本策略不设计成和 MySQL 主从机制（主副本负责写，从副本负责读）一样的策略呢？

**答案**：Kafka副本不对外提供服务的意义: 如果允许follower副本对外提供读服务（主写从读），首先会存在数据一致性的问题，消息从主节点同步到从节点需要时间，可能造成主从节点的数据不一致。主写从读无非就是为了减轻leader节点的压力，将读请求的负载均衡到follower节点，如果Kafka的分区相对均匀地分散到各个broker上，同样可以达到负载均衡的效果，没必要刻意实现主写从读增加代码实现的复杂程度。
#### producer
生产者，就是向 topic 生产消息的应用程序。
#### consumer
消费者，就是从 topic 消费消息的应用程序。
#### consumer offset
消费偏移量：表征消费者消费进度，每个消费者都有自己的消费者位移。
#### consumer group
消费者组：多个消费者实例共同组成的一个组，同时消费多个分区以实现高吞吐。
#### rebalance
重平衡，kafka 中一个大名鼎鼎的消息重分配机制。假设一个消费组内的 consumer A 宕机了，kafka 能够自动检测到，并且把 consumer A 原先负责的 partition 重新分配给其他活着的 consumer。

### kafka 持久化数据原理
AOF（Append-only-file），不错就是 redis 数据持久化方式之一，只不过 redis 是追加编辑数据的命令，而 kafka 是追加消息数据到日志（Log）来保存数据。一个日志就是磁盘上一个只能追加写（Append-only）消息的物理文件。
#### 保证日志在磁盘的可持续存储
kafka 为了保存日志数据能够可持续在磁盘上存储提供了两种策略：日志分段机制（Log Segment）、周期性检查过期删除

- 日志分段机制：在 Kafka 底层，一个日志又进一步细分成多个日志段，消息被追加写到当前最新的日志段中，当写满了一个日志段后，Kafka 会自动切分出一个新的日志段，并将老的日志段封存起来。
- 周期性检查过期删除：Kafka 在后台还有定时任务会定期地检查老的日志段是否能够被删除，从而实现回收磁盘空间的目的。

### kafka 三层架构
- 主题层：每个主题可以设置一个或多个分区，每个分区又可以设置一个或多个副本
- 分区层：每个分区的副本中有且只有一个是领导副本并对外提供读写功能，其余的都是追随副本只能同步领导副本的数据
- 消息层：分区中包含若干条消息，每条消息的位移从 0 开始，依次递增。

### kafka 不只是消息引擎系统
kafka 还是一个分布式流处理平台（Distributed Streaming Platform），Kafka 社区于 0.10.0.0 版本正式推出了流处理组件 Kafka Streams。

#### kafka 三个特性
- 提供一套 API 实现生产者和消费者
- 降低网络传输和磁盘存储开销
- 实现高伸缩性架构

#### Kafka Streams 优势
- kafka 更容易实现端到端的正确性，正确性一直是批处理的强项，而实现正确性的基石则是要求框架能提供精确一次处理语义，即处理一条消息有且只有一次机会能够影响系统状态。
- kafka 对于流式计算的定位，官网上明确标识 Kafka Streams 是一个用于搭建实时流处理的客户端库而非是一个完整的功能系统。

##### 精确一次处理语义（exactly once）
举个例子，如果我们使用Kafka计算某网页的PV——我们将每次网页访问都作为一个消息发送的Kafka。PV的计算就是我们统计Kafka总共接收了多少条这样的消息即可。精确一次处理语义表示每次网页访问都会产生且只会产生一条消息，否则有可能产生多条消息或压根不产生消息。

#### kafka 三个作用
- 消息引擎系统
- 分布式流式处理平台
- 分布式存储系统（当前没有人在生产环境下使用）

### kafka 种类
- Apache kafka：也称社区版 Kafka。优势在于迭代速度快，社区响应度高，使用它可以让你有更高的把控度；缺陷在于仅提供基础核心组件，缺失一些高级的特性。
- Confluent kafka：Confluent 公司提供的 kafka，优势在于集成了很多高特效且由 kafka 原班人马打造，质量上有保证；缺陷在于国内的相关文档资料不全，普及率低，没有太多可参考的案例。
- CDH/HDP kafka：大数据云公司提供的 kafka，内嵌 Apache kafka。优势在于操作简单，节省运维成本；缺陷在于把控度低，演进速度慢。

#### 如何选用合适的 kafka
- 如果仅仅需要一个消息引擎系统亦或是简单的流程处理应用场景，同时需要对系统有较大的把控度，推荐使用 Apache kafka
- 如果需要用到 kafka 的一些高级特性功能，推荐使用 Confluent kafka
- 如果需要快速搭建消息引擎系统，或者需要搭建是多框架构成的数据平台且 kafka 只是其中的一个组件，推荐使用 CDH/HDP kafka

### kafka 版本介绍
- 0.7 基础消息队列功能
- 0.8 引入副本功能 
  - 0.8.2.0 引入新版本 Producer API 
- 0.9.0.0 增加了基础的安全认证 / 权限功能，同时使用 Java 重写了新版本消费者 API，另外还引入了 Kafka Connect 组件用于实现高性能的数据抽取. 
- 0.10.0.0 引入了 Kafka Streams。 
  - 0.10.1 和 0.10.2  主要功能变更都是在 Kafka Streams 组件上. 
- 0.11.0.0 引入了两个重量级的功能变更：
  - 提供幂等性 Producer API 以及事务（Transaction） API；
- 0.11.0.3  这个版本的消息引擎功能已经非常完善了。
  - 二.对 Kafka 消息格式做了重构。
- 1.0 和 2.0 这两个大版本主要还是 Kafka Streams 的各种改进，在消息引擎方面并未引入太多的重大功能特性。

## kafka 的基本使用
### kafka 集群部署方案
#### 操作系统
- window
- Linux
- macOS
##### 特性比较
- 主流的 I/O 模型通常有 5 种类型：阻塞式 I/O、非阻塞式 I/O、I/O 多路复用、信号驱动 I/O 和异步 I/O。
- Java 中 Socket 对象的阻塞模式和非阻塞模式就对应于前两种模型；
- Linux 中的系统调用 select 函数就属于 I/O 多路复用模型；
- 大名鼎鼎的 epoll 系统调用则介于第三种和第四种模型之间；
- 至于第五种模型，其实很少有 Linux 系统支持，反而是 Windows 系统提供了一个叫 IOCP 线程模型属于这一种。
- 实际上 Kafka 客户端底层使用了 Java 的 selector；

|        | I/O模型  | 网络传输效率 | 社区支持度 |
|--------|--------|--------|-------|
| window | select | 传统模式   | 无保证   |
| Linux  | epoll  | 零拷贝    | 有保证   |
| macOS  |        |        |       |

##### 总结
- Kafka 部署在 Linux 上是有优势的，因为能够获得更高效的 I/O 性能
- 在 Linux 部署 Kafka 能够享受到零拷贝技术所带来的快速数据传输特性
- Windows 平台上部署 Kafka 只适合于个人测试或用于功能验证，千万不要应用于生产环境

#### 磁盘
##### SSD 和机械磁盘
- SSD：随机读写快，性能优势大；单价高
- 机械磁盘：随机读写慢，性能低，容易损坏；价格便宜 
- kafka 使用往磁盘中读写数据多数是使用顺序读写，一定程度上避免了机械磁盘的缺点，SSD随机读写性能也没有得到体现，所以选择机械磁盘部署kafka集群即可。

##### 磁盘阵列（RAID）和 kafka 负载均衡
- 磁盘阵列：提供冗余的磁盘存储空间、提供负载均衡（大厂会使用硬件上的负载均衡）
- kafka：自己实现软件上的冗余机制来提供高可用性，通过分区概念自行实现软件上的负载均衡

##### 规划集群磁盘容量
###### 需求
假设你所在公司有个业务每天需要向 Kafka 集群发送 1 亿条消息，每条消息保存两份以防止数据丢失，另外消息默认保存两周时间。现在假设消息的平均大小是 1KB，那么你能说出你的 Kafka 集群需要为这个业务预留多少磁盘空间吗？
###### 方案
每天 1 亿条 1KB 大小的消息，保存两份且留存两周的时间，那么总的空间大小就等于 1 亿 * 1KB * 2 / 1000 / 1000 = 200GB。一般情况下 Kafka 集群除了消息数据还有其他类型的数据，比如索引数据等，故我们再为这些数据预留出 10% 的磁盘空间，因此总的存储容量就是 220GB。既然要保存两周，那么整体容量即为 220GB * 14，大约 3TB 左右。Kafka 支持数据的压缩，假设压缩比是 0.75，那么最后你需要规划的存储空间就是 0.75 * 3 = 2.25TB。
###### 计算要素
- 新增消息数量
- 消息保存时间
- 平均消息大小
- 副本数量
- 是否启用压缩（压缩率）
###### 计算公式
集群容量 >= 新增消息数量 * 平均消息大小 * 副本数量 * 消息保存时间 * 压缩率

##### 带宽
对于 Kafka 这种通过网络大量进行数据传输的框架而言，带宽特别容易成为瓶颈。
###### 需求
假设你公司的机房环境是千兆网络，即 1Gbps，现在你有个业务，其业务目标或 SLA 是在 1 小时内处理 1TB 的业务数据。那么问题来了，你到底需要多少台 Kafka 服务器来完成这个业务呢？
###### 方案
- 假设每个 kafka 节点都是部署在专属服务器上（不存在其他应用服务）
- 假设 kafka 服务在稳定情况下占用服务器带宽最多不超过 70% 资源（上限阈值）
- 假设 kafka 服务在正常使用中只占到上限阈值的 1/3，剩下的 2/3 用做 kafka 应对突发情况的预留资源（吞吐量）

###### 计算要素
- 待处理数据量：1TB = 1024GB = 1048576MB 
- 处理时间：1h = 3600s
- 带宽：1Gbps = 1GB/s
- 上限阈值：0.7 * 1Gbps = 700MB/s
- 吞吐量：700MB/s * 0.34 = 240MB/s
- 副本数

###### 计算公式
集群机器数量 >= 待处理数据量 * 8（网络传输按位传） / 处理时间 / 吞吐量 * 副本数

##### 总结
与其盲目上马一套 Kafka 环境然后事后费力调整，不如在一开始就思考好实际场景下业务所需的集群环境，在考量部署方案时需要通盘考虑，不能仅从单个维度上进行评估。

| 因素   | 考量点                    | 建议                                    |
|------|------------------------|---------------------------------------|
| 操作系统 | 操作系统I/O模型，网络传输         | 将 kafka 部署在 Linux 上                   |
| 磁盘   | 磁盘I/O性能                | 普通环境使用机械磁盘，不需要考虑 RAID                 |
| 磁盘容量 | 消息数量、保存时间、消息大小、副本数；    | 使用中建议预留 20% ~ 30% 的磁盘空间               |
| 带宽   | 根据实际的带宽资源、业务SLA评估服务器数量 | 对于千兆带宽，建议每台服务器按照 700Mbps 来计算，避免大流量下丢包 |

### 集群参数配置
#### 静态参数（static configs）
所谓静态参数，是指你必须在 Kafka 的配置文件 server.properties 中进行设置的参数，不管你是新增、修改还是删除。同时，你必须重启 Broker 进程才能令它们生效。

```properties
# （重要参数）指定了 Broker 需要使用的若干个文件目录路径，没有默认值，需要自定义，最好保证每个目录都是独立的物理磁盘（1、提升读写性能；2、实现故障转移——Failover）。
log.dirs = /home/kafka1,/home/kafka2,/home/kafka3

# （一般参数）单个文件目录路径，是 log.dirs 参数的补充，初始化了 log.dirs 就不需要设置此参数
log.dir = /home/kafka1

# 注册中心配置参数，一般用 zookeeper 来管理 kafka 集群的所有元数据，包括 broker、topic、partition、leader 等信息（多kafka集群共用zk情况下需在后面拼接/KafkaClusterName）
zookeeper.connect = zk1:2181,zk2:2181,zk3:2181/KafkaClusterName

# 指定内网 client 可以连接 kafka 服务的网络协议
listeners = PLAINTEXT://localhost:9092

# 指定外网 client 可以连接 kafka 服务的网络协议
advisor.listeners = PLAINTEXT://PLAINTEXT:9092,CONTROLLER://localhost:9093

# 是否允许自动创建 Topic
auto.create.topics.enable = false

# 是否允许 Unclean Leader 选举（true = 允许从 OSR 队列中选取一个副本作为新的 leader；false = 只能从 ISR 队列中选 leader；ISR——完全同步数据的副本队列，OSR——不完全同步数据的副本队列）
unclean.leader.election.enable = false

# 是否允许定期进行 Leader 选举（true = 不管当前 leader 有没有故障都会进行选举，替换当前 leader，可能会影响性能）
auto.leader.rebalance.enable = false

# 指定 kafka 消息数据被保存的时间，可以根据需求指定（hours|minutes|ms）格式参数（优先级：ms > minutes > hours）
log.retention.hours = 7 * 24

# 指定 broker 为消息保存的磁盘容量（-1 = 可以理解为总磁盘大小）
log.retention.bytes = -1

# 控制 Broker 能够接收的一个批次消息的最大字节数（默认的 1000012）
message.max.bytes = 1024 * 1024

```
#### 主题级别参数（kafka-configs）
而主题级别参数的设置则有所不同，Kafka 提供了专门的 kafka-configs 命令来修改它们。
#### JVM、OS 级别参数
它们的设置方法比较通用化。

## 客户端实践及原理剖析
### 生产者消息分区机制原理剖析
#### 为什么要分区？
kafka有三层结构：主题、分区、消息；

- 主题对应着副本确保集群高可用性，对数据进行业务分类隔离
- 分区负责消息的负载均衡，多个分区存储消息提高集群的吞吐量

##### 为什么不用 topic 做负载均衡？
不使用多topic做负载均衡，意义在于对业务屏蔽该逻辑。业务只需要对topic进行发送，指定负载均衡策略即可，发送端sdk、服务端和消费端会处理。
#### 常见分区策略
分区策略有点类似网络传输过程中节点间的路由选择
##### 轮询（默认分区策略）
每个分区都能轮流给到消息：A、B、C三个分区分配4个消息（msg1、msg2、msg3、msg4），每个分区按顺序（A > B > C）分配，多出部分重复执行分配逻辑。

```
         ------       ------       -------
        | 分区A |     | msg1 |     | msg4 |
         ------       ------       -------
         ------       ------
        | 分区B |     | msg2 |
         ------       ------
         ------       ------
        | 分区C |     | msg3 |
         ------       ------
```

- 优点：有非常优秀的负载均衡表现，它总是能保证消息最大限度地被平均分配到所有分区上。
- 缺点：由于每个分区消费者效率的不确定性，可能会造成固定分区的消息积压
##### 随机（老版本分区策略）
字面含义就是把消息在分区中随机抽取一个分区存放，本质上看随机策略也是力求将数据均匀地打散到各个分区，但从实际表现来看，它要逊于轮询策略，所以如果追求数据的均匀分布，还是使用轮询策略比较好。
```json
List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
return ThreadLocalRandom.current().nextInt(partitions.size());
```
##### 按消息键保序
Kafka 允许为每条消息定义消息键，简称为 Key。一旦消息被定义了 Key，那么你就可以保证同一个 Key 的所有消息都进入到相同的分区里面，由于每个分区下的消息处理都是有顺序的，故这个策略被称为按消息键保序策略。
```java
List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
return Math.abs(key.hashCode()) % partitions.size();
```
##### 地理位置
基于地理位置的分区策略，一般都是只针对大型 kafka 集群，特别是跨城市、跨国际、跨洲际。

### 生产者压缩算法剖析
kafka 的消息层次都分为：消息集合（message set）、消息（message），一个消息集合包含若干条消息日志项（message item），而消息日子项才是真正封装消息的地方。在 kafka 底层的消息日志是由一系列的消息集合组成，而通常不会具体操作一条条消息，他总是在消息集合这个层面上进行写入操作。

**改进**：在 V1 版本中每条消息都进行 CRC 校验，压缩过程把多条消息进行压缩然后保存到外层消息的消息体字段中；在 V2 版本中消息的 CRC 校验和压缩都以消息集合的颗粒度进行。这个改进对 CPU 有很大的性能提升。
#### 怎么压缩？
- 常用压缩算法：LZ4、Snappy、zstd、GZIP
- Producer 端：通过配置参数`compression.type`指定压缩算法类型
- Broker 端：也有一个参数叫`compression.type`但这个参数的默认值是 producer，这表示 Broker 端会沿用 Producer 端使用的压缩算法。如若 Broker 端设置了和 Producer 端不同的 compression.type 值，就一定要小心了，因为可能会发生预料之外的压缩 / 解压缩操作，通常表现为 Broker 端 CPU 使用率飙升。

##### Broker 端进行压缩（尽量避免）
- Broker 端指定了和 Producer 端不同的压缩算法：compression.type 参数值设置不同
- Broker 端发生了消息格式转换：主要是为了兼容老版本的消息格式，在 kafka 集群中存在多个版本的消息格式非常常见。Broker 端为了兼容老版本的格式会把新版本的消息格式会进行格式转换，这个过程就涉及到了消息解压和重新压缩，除此之外 kafka 还丧失了 Zero Copy 特性，所以格式转换对性能是有很大影响的。

##### 何时解压？
Producer 端压缩消息集合时会把压缩算法封装到消息集合中，当 Consumer 端读拿到压缩消息后先取出压缩算法，在根据算法类型进行消息解压。
- 正常流程：Producer 端压缩消息 -> Broker 端保持(解压 - 校验 - 压缩) -> Consumer 端解压消息
- 非常流程：Producer 端压缩消息 -> Broker 端（解压 - 格式转换 - 压缩）消息 -> Consumer 端解压消息

##### 启用压缩条件
kafka 集群有效使用压缩算法可以极大地节省 Producer、Broker、Consumer 端间的网络带宽资源和 Broker 端的磁盘空间，但是也随之会带来各端的 CPU 资源消耗，就看具体项目的资源配置了。
- Producer 端：CPU 资源很充足，带宽资源紧缺
- Broker 端：CPU 资源充足，磁盘空间紧缺，带宽资源紧缺
- Consumer 端：CPU 资源充足，带宽资源紧缺

##### 压缩算法比较
- 吞吐量：LZ4 > Snappy > zstd > GZIP
- 压缩率：zstd > LZ4 > GZIP > Snappy

### 无消息丢失配置怎么实现
kafka 只对”已提交“的消息（committed message）做有限度的持久化保证。

- **已提交的消息**：提交到 kafka 的 message 被 broker 成功接收，并写入到分区的日志文件后，kafka 会认为这条 message 是“已提交”状态。<sup><mark>向 Kafka 发送数据并不是真要等数据被写入磁盘才会认为成功，而是只要数据被写入到操作系统的页缓存（Page Cache）上就可以了，随后操作系统根据 LRU 算法会定期将页缓存上的“脏”数据落盘到物理磁盘上。这个定期就是由提交时间来确定的，默认是 5 秒。</mark></sup>
- **有限度的持久化保证**：需要保证 kafka 集群最低存活条件，也就说必有要有存活的 broker 供生产者/消费者做读写操作。

#### 消息丢失案例
##### 生产者丢失数据
使用了无回调操作的异步发送消息接口<sup><mark>producer.send(msg)，这种发送方式有个有趣的名字，叫“fire and forget”，翻译一下就是“发射后不管”</mark></sup>，再出现网络抖动消息未到达 broker、消息格式不合规<sup><mark>消息太大了，超过了 Broker 的承受能力</mark></sup>导致 broker 拒收。这时 kafka 不认为消息是“已提交”的。

>解决方案：  
Producer 永远要使用带有回调通知的发送 API，也就是说不要使用 producer.send(msg)，而要使用 producer.send(msg, callback)。在 callback 回调方法中可以得知消息是否提交成功。

##### 消费者丢失数据
1. 消费端是读取 broker 上保存的消息，所以这里的数据丢失一般是指，由于消费端的位移<sup>offset</sup>更新逻辑出错<sup><mark>第一步是读书，第二步是更新书签页。如果这两步的顺序颠倒了，就可能出现这样的场景</mark></sup>，就会导致队列中某些消息未能正常消费。

>解决方案：  
维持先消费消息（阅读），再更新位移（书签）的顺序即可。这样就能最大限度地保证消息不丢失。

2. 消费端多线程异步消费消息，每个线程都是独立的，不能预知每个线程的执行情况。这时如果开启自动更新位移逻辑，要是线程执行失败，也会付出现位移已更新，但消息还未消费的情况。

>解决方案：  
如果是多线程异步处理消费消息，Consumer 程序不要开启自动提交位移，而是要应用程序手动提交位移。

##### broker 丢失数据
当前 kafka 集群中所有的 broker 都宕机了，也就是没法接收 producer 生产的消息时。Kafka 依然不认为这条消息属于已提交消息，故对它不做任何持久化保证。

#### 最佳实践
1. 不要使用 producer.send(msg)，而要使用 producer.send(msg, callback)。记住，一定要使用带有回调通知的 send 方法。
2. 设置 retries 为一个较大的值。这里的 retries 同样是 Producer 的参数，对应前面提到的 Producer 自动重试。当出现网络的瞬时抖动时，消息发送可能会失败，此时配置了 retries > 0 的 Producer 能够自动重试消息发送，避免消息丢失。
3. 设置 unclean.leader.election.enable = false。这是 Broker 端的参数，它控制的是哪些 Broker 有资格竞选分区的 Leader。如果一个 Broker 落后原先的 Leader 太多，那么它一旦成为新的 Leader，必然会造成消息的丢失<sup><mark>这也是一种虽然有Broker节点存活，但消息还是丢失的情况</mark></sup>。故一般都要将该参数设置成 false，即不允许这种情况的发生。
4. 设置 replication.factor >= 3。这也是 Broker 端的参数。其实这里想表述的是，最好将消息多保存几份，毕竟目前防止消息丢失的主要机制就是冗余。
5. 设置 acks = all。acks 是 Producer 的一个参数，代表了你对“已提交”消息的定义。如果设置成 all，则表明所有正常的副本 Broker 都要接收到消息，该消息才算是“已提交”。这是最高等级的“已提交”定义。<sup><mark>acks 和 min.insync.replicas 区别： replication.refactor是副本replica总数， min.insync.replicas是要求确保至少有多少个replica副本写入后才算是提交成功，这个参数是个硬指标；acks=all是个动态指标，确保当前能正常工作的replica副本都写入后才算是提交成功。举个例子：比如，此时副本总数3，即replication.refactor = 3，设置min.insync.replicas=2，acks=all，那如果所有副本都正常工作，消息要都写入三个副本，才算提交成功，此时这个min.insync.replicas=2下限值不起作用。如果其中一个副本因为某些原因挂了，此时acks=all的动态约束就是写入两个副本即可，触达了min.insync.replicas=2这个下限约束。如果三个副本挂了两个，此时ack=all的约束就变成了1个副本，但是因为有min.insync.replicas=2这个下限约束，写入就会不成功。</mark></sup>
6. 设置 min.insync.replicas > 1。这依然是 Broker 端参数，控制的是消息至少要被写入到多少个副本才算是“已提交”。设置成大于 1 可以提升消息持久性。在实际环境中千万不要使用默认值 1。
7. 确保 replication.factor > min.insync.replicas。如果两者相等，那么只要有一个副本挂机，整个分区就无法正常工作了。我们不仅要改善消息的持久性，防止数据丢失，还要在不降低可用性的基础上完成。推荐设置成 replication.factor = min.insync.replicas + 1。
8. 确保消息消费完成再提交。Consumer 端有个参数 enable.auto.commit，最好把它设置成 false，并采用手动提交位移的方式。就像前面说的，这对于单 Consumer 多线程处理的场景而言是至关重要的。

### kafka 拦截器
其基本思想就是允许应用程序在不修改逻辑的情况下，动态地实现一组可插拔的事件处理逻辑链。它能够在主业务操作的前后多个时间点上插入对应的“拦截”逻辑。可以将一组拦截器串连成一个大的拦截器，Kafka 会按照添加顺序依次执行拦截器逻辑。
#### 生产端拦截器
- 拦截点：发送前、发送成功后（副本ack后）
```java
public class ProducerKafkaInterceptor implements ProducerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ProducerKafkaInterceptor.class);

    @Override
    public ProducerRecord onSend(ProducerRecord record) {
        // 在发送消息之前对消息进行处理
        logger.info("============================ producer：在发送消息之前对消息进行处理 ============================");
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // 消息被确认或发送失败时的回调方法
        logger.info("============================ producer：消息被确认或发送失败时的回调方法 ============================");
    }

    @Override
    public void close() {
        // 关闭拦截器时的清理操作
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 拦截器的配置方法
    }
}
```
#### 消费端拦截器
- 拦截点：消费前、消费成功后（提交消费位置offset）
```java
public class ConsumerKafkaInterceptor implements ConsumerInterceptor<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerKafkaInterceptor.class);

    @Override
    public void configure(Map<String, ?> configs) {
        // 在这里进行相关配置
    }

    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        // 在这里处理消息被消费之前的逻辑
        // 返回经过处理的ConsumerRecords对象
        logger.info("============================ consumer：在这里处理消息被消费之前的逻辑 ============================");
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // 在这里处理消费者提交位移之前的逻辑
        logger.info("============================ consumer：在这里处理消费者提交位移之前的逻辑 ============================");
    }

    @Override
    public void close() {
        // 在这里进行资源释放
    }

}
```
#### 注册拦截器
##### 代码注册
```java
@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<?, ?> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, ConsumerKafkaInterceptor.class.getName());
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    public ProducerFactory<?, ?> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, ProducerKafkaInterceptor.class.getName());
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

}
```
##### 配置文件注册
```properties
spring.kafka.producer.properties.interceptor.classes=pf.bluemoon.com.kafka.interceptor.ProducerKafkaInterceptor
spring.kafka.consumer.properties.interceptor.classes=pf.bluemoon.com.kafka.interceptor.CustomerKafkaInterceptor
```
#### 经典案例
Kafka 拦截器可以应用于包括客户端监控、端到端系统性能检测、消息审计等多种功能在内的场景。
