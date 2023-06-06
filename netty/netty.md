# netty
## 线程模型基本介绍
### 目前存在的线程模型
- 传统阻塞I/O服务模型
- Reactor模型
### Reactor三剑客
- 单Reactor + 单线程：前台接待员和服务员是同一个人，全程为顾客服务
- 单Reactor + 多线程：一个前台接待员 + 多个服务员，共同为顾客服务
- 主从Reactor + 多线程：多个前台接待员 + 多个服务员，共同为顾客服务
#### 优点
- 响应快，不必为单个同步时间所阻塞，虽然 Reactor 本身依然是同步的
- 实现简单，可以最大程度的避免复杂的多线程及同步问题，并且避免了多线程或多进程的切换开销
- 扩展性好，可以通过增加 Reactor 实例个数来充分利用 CPU 资源
- 复用性好，Reactor 模型本身与具体事件处理逻辑无关，具有很高的复用性
### Netty线程模式
Netty 主要基于主从 Reactor 多线程模型做了一定的改进，其中主从 Reactor 多线程模型有多个 Reactor。
## 原理解析
接下来通过图文并茂的方式对各个线程模型进行原理解剖，并且分析出每个模型的优缺点。
### 传统阻塞I/O服务模型
#### 架构图
```
                                        ----------------------------------------  
                                       |   应用程序                             |
                                       |  ------------------------------------  |
                                       | |   处理线程                         | |
                                       | |  --------------------------------  | |
     --------      request             | | |              handler           | | |
    | client | <-----------------------> | |  ------   ----------   ------  | | |
     --------      response            | | | | read | | 业务处理 | | send | | | |
                                       | | |  ------   ----------   ------  | | |
                                       | |  --------------------------------  | |
                                       |  ------------------------------------  |
                                       |                                        |
                                       |  ------------------------------------  |
                                       | |   处理线程                         | |
     --------      request             | |  --------------------------------  | |
    | client | <-----------------------> | |              handler           | | |
     --------      response            | | |  ------   ----------   ------  | | |
                                       | | | | read | | 业务处理 | | send | | | |
                                       | | |  ------   ----------   ------  | | |
                                       | |  --------------------------------  | |
                                       |  ------------------------------------  |
                                        ----------------------------------------

```
- 对象：client、handler
- API：read、业务处理、send
#### 模型特点
- 采用阻塞 I/O 模式额 read 输入的数据
- 需要为每个请求连接创建独立的线程来完成：读取数据、业务处理、数据返回
#### 优点
流程简单，易于实现
#### 缺点
- 当并发量大时，会创建大量的线程处理请求，占用很大的系统资源
- 线程创建后，如果当前线程暂时没有数据可读，该线程就会阻塞在 read 操作，造成线程资源浪费
#### 优化方案
- 基于 I/O 复用模型（Reactor）：多个连接共用一个阻塞对象，应用程序只需要在一个阻塞对象等待，无需阻塞等待所有连接。当某个连接有新的数据可以处理时，操作系统通知应用程序，线程从阻塞状态返回，开始进行业务处理。Reactor 对应的叫法:
  - 反应器模式 
  - 分发者模式(Dispatcher) 
  - 通知者模式(notifier)
- 基于线程池资源复用模型：不必再为每个连接创建线程，将连接完成后的业务处理任务分配给线程进行处理，一个线程可以处理多个连接的业务
#### I/O复用 + 线程池 = Reactor 模式基本设计思想
```
                                        ----------------------------------------  
                                       |   应用程序                             |
                                       |  ------------------------------------  |
                                       | |   处理线程                         | |
                                       | |  --------------------------------  | |
     --------      event               | | |              handler           | | |
    | client | <-------------          | | |  ------   ----------   ------  | | |
     --------                |         | | | | read | | 业务处理 | | send | | | |
                             |         | | |  ------   ----------   ------  | | |
                             |         | |  --------------------------------  | |
                             |         |  ------------------------------------  |
                             |         |                ^                       |
                             |         |                | dispatch              |
                             |         |     -----------------------            |
                             |         |    |  ServiceHandler       |           |
                             |------------> |   ---------------     |           |
                             |         |    |  | EventDispatch |    |           |
                             |         |    |   ---------------     |           |
                             |         |     -----------------------            |
                             |         |                | dispatch              |
                             |         |                V                       |
                             |         |  ------------------------------------  |
                             |         | |   处理线程                         | |
     --------      event     |         | |  --------------------------------  | |
    | client | <-------------          | | |              handler           | | |
     --------                          | | |  ------   ----------   ------  | | |
                                       | | | | read | | 业务处理 | | send | | | |
                                       | | |  ------   ----------   ------  | | |
                                       | |  --------------------------------  | |
                                       |  ------------------------------------  |
                                        ----------------------------------------

对象：client、ServiceHandler、EventHandler
API：EventDispatch、read、业务处理、send
```
- Reactor 模式，通过把输入一个或多个 event 同时传递给 ServiceHandler 的模式(基于事件驱动)
- ServiceHandler 处理传入的多个请求,并将它们 dispatch 到相应的处理线程， 因此 Reactor 模式也叫 Dispatcher 模式
- Reactor 模式使用 I/O 复用监听事件, 收到事件后，分发给某个线程(进程), 这点就是网络服务器处理高并发的关键
### Reactor模式的核心组件
#### Reactor
Reactor 在一个单独的线程中运行，负责监听（select）和分发（dispatch）事件，把事件给到适当的处理器（handler）来执行逻辑。类似于公司的行政前台，负责接待应聘者，并应聘者指引到具体的面试官。
#### Handlers
完成 I/O 事件处理的实际事件，类似于面试流程中的实际处理者（面试官）。Reactor 通过调度适当的 handler 处理 I/O 事件，处理程序执行非阻塞操作。
### Reactor模式分类
根据 Reactor 的数量和处理资源池线程的数量不同，有 3 种典型的实现
#### 单Reactor单线程
##### 原理图
```
                                            
                                                ---------------------------------------------------------
                                               |  应用程序                                               |
                                               |  -----------------------------------------------------  |
                                               | | 单线程                                              | |
         --------     request                  | |   -------------------------                         | |
        | client | --------------------        | |  |      Reactor            |                        | |
         --------                      |----------->|  --------   ----------  |                        | |
                                       |       | |  | | select | | dispatch | |                        | |
                                       |       | |  |  --------   ----------  |                        | |
                                       |       | |   -------------------------                         | |
                                       |       | |           |            |                            | |
                                       |       | |  建立连接  |            |  处理请求                  | |
                                       |       | |           V            V                            | | 
                                       |       | |   ------------    --------------------------------  | |
         --------     request          |       | |  |  Acceptor  |  |              Handler           | | |
        | client | --------------------        | |  |  --------  |  |  ------   ----------   ------  | | |
         --------                              | |  | | accept | |  | | read | | 业务处理 | | send |  | | |
                                               | |  |  --------  |  |  ------   ----------   ------  | | |
                                               | |   ------------    --------------------------------  | |
                                               |  -----------------------------------------------------  |
                                                ---------------------------------------------------------

对象：client、Reactor、Acceptor、Handler
API：EventDispatch、read、业务处理、send
```
##### 方案说明
- Select 是前面 I/O 复用模型介绍的标准网络编程 API，可以实现应用程序通过一个阻塞对象监听多路连接请求
- Reactor 对象通过 Select 监控客户端请求事件，收到事件后通过 Dispatch 进行分发
- 如果是建立连接请求事件，则由 Acceptor 通过 accept 处理连接请求，然后创建一个 Handler 对象完成后的后续业务处理
- 如果不是建立连接事件，则 Reactor 会直接分发到对应的 Handler 来处理
- Handler 会完成【Read → 业务处理 → Send】完整的业务流程
##### 方案分析
- 优点：模型简单，没有多线程、进程通信、竞争的问题，全部流程都在一个线程中完成
- 缺点：
  - 性能问题，无法充分利用**多核CPU性能**，多客户端请求情况下，单线程容易产生**阻塞等待**；
  - 可靠性问题，线程意外终止或者进入死循环，则会导致整个**系统不可用**。
#### 单Reactor多线程
##### 原理图
```

                                            
                                                ---------------------------------------------------------
                                               |  应用程序                                               |
                                               |  -----------------------------------------------------  |
                                               | | 主线程                                              | |
         --------     request                  | |   -------------------------                         | |
        | client | --------------------        | |  |      Reactor            |                        | |
         --------                      |----------->|  --------   ----------  |                        | |
                                       |       | |  | | select | | dispatch | |                        | |
                                       |       | |  |  --------   ----------  |                        | |
                                       |       | |   -------------------------                         | |
                                       |       | |           |            |                            | |
                                       |       | |  建立连接  |            |  处理请求                  | |
                                       |       | |           V            V                            | | 
                                       |       | |   ------------    -------------    -------------    | |
         --------     request          |       | |  |  Acceptor  |  |  Handler    |  |  Handler    |   | |
        | client | --------------------        | |  |  --------  |  |  ---------  |  |  ---------  |   | |
         --------                              | |  | | accept | |  | |read/send| |  | |read/send| |   | |
                                               | |  |  --------  |  |  ---------  |  |  ---------  |   | |
                                               | |   ------------    -------------    -------------    | |
                                               |  -------------------------|---------------|-----------  |
                                               |                           |               |             |
                                               |                           |               |             |
                                               |                -----------                |             |
                                               |  -------------|---------------------------|----------   |
                                               | |  线程池     V                           V          |  |
                                               | |     ------------------      ------------------     |  |
                                               | |    | 线程1            |    | 线程1            |    |  |
                                               | |    |  --------------  |    |  --------------  |    |  |
                                               | |    | |    Worker    | |    | |    Worker    | |    |  |
                                               | |    | |  ----------  | |    | |  ----------  | |    |  |
                                               | |    | | | 业务处理  | | |    | | | 业务处理 | | |    |  |
                                               | |    | |  ----------  | |    | |  ----------  | |    |  |
                                               | |    |  --------------  |    |  --------------  |    |  |
                                               | |     ------------------      ------------------     |  |
                                               |  ----------------------------------------------------   |
                                                ---------------------------------------------------------


对象：Client、Reactor、Acceptor、Handler、Worker
API：select、dispatch、accept、read、send、业务处理
```
##### 方案说明
- Reactor 对象通过 select 监控客户端请求事件, 收到事件后，通过 dispatch 进行分发
  - 如果建立连接请求, 则由 Acceptor 通过 accept 处理连接请求, 然后创建一个 Handler 对象处理完成连接后的各种事件
  - 如果不是连接请求，则由 dispatch 分发调用连接对应的 handler 来处理
- handler 只负责响应事件，不做具体的业务处理, 通过 read 读取数据后，会调用线程池中某个业务线程负责实际的逻辑处理
- worker 线程池会分配独立线程完成真正的逻辑处理，并将结果返回给 handler
- handler 收到响应后，通过 send 将结果返回给 client
##### 方案分析
- 优点：可以充分的利用多核 CPU 的处理能力
- 缺点：多线程数据共享和访问比较复杂，主线程 reactor 处理所有的事件监听和响应，在高并发场景容易出现性能瓶颈。
#### 主从Reactor多线程
##### 原理图
```

                                            
                                                ---------------------------------------------------------
                                               |  应用程序                                               |
                                               |  -----------------------------------------------------  |
                                               | | 主线程                                              | |
         --------     request                  | |   -------------------------          ------------   | |
        | client | --------------------        | |  |       MainReactor       |        |  Acceptor  |  | |
         --------                      |----------->|  --------   ----------  |建立连接 |  --------  |  | |
                                       |       | |  | | select | | dispatch | | -----> | | accept | |  | |
                                       |       | |  |  --------   ----------  |        |  --------  |  | |
                                       |       | |   -------------------------          ------------   | |
                                       |       |  -------------------|---------------------------------  |
                                       |       |                     |                                   |
                                       |       |  -------------------|---------------------------------  | 
                                       |       | | Reactor 子线程    V                                 | |
                                       |       | |         ----------------------------                | |
                                       |       | |        |         SubReactor         |               | |
                                       |       | |        |   --------    ----------   |               | |
                                       |       | |        |  | select |  | dispatch |  |               | |
                                       |       | |        |   --------    ----------   |               | |
                                       |       | |         ----------------------------                | |
                                       |       | |            请求处理   |                             | |
                                       |       | |        ---------------------------------            | |
                                       |       | |        V               V               V            | |      
                                       |       | |   ------------    ------------    ------------      | |
         --------     request          |       | |  |  Handler1  |  |  Handler2  |  |  Handler3  |     | |
        | client | --------------------        | |  |  --------- |  |  --------- |  |  --------- |     | |
         --------                              | |  | |read/send||  | |read/send||  | |read/send||     | |
                                               | |  |  --------- |  |  --------- |  |  --------- |     | |
                                               | |   ------------    ------------    ------------      | |
                                               |  --------|----------------|---------------|-----------  |
                                               |          |                |               |             |
                                               |           ----------------|---------------              |
                                               |                           V                             |
                                               |  ----------------------------------------------------   |
                                               | |  线程池                                            |  |
                                               | |     ------------------      ------------------     |  |
                                               | |    | 线程1            |    | 线程2            |    |  |
                                               | |    |  --------------  |    |  --------------  |    |  |
                                               | |    | |    Worker    | |    | |    Worker    | |    |  |
                                               | |    | |  ----------  | |    | |  ----------  | |    |  |
                                               | |    | | | 业务处理  | | |    | | | 业务处理 | | |    |  |
                                               | |    | |  ----------  | |    | |  ----------  | |    |  |
                                               | |    |  --------------  |    |  --------------  |    |  |
                                               | |     ------------------      ------------------     |  |
                                               |  ----------------------------------------------------   |
                                                ---------------------------------------------------------


对象：Client、MainReactor、SubReactor、Acceptor、Handler、Worker
API：select、dispatch、accept、read、send、业务处理
```
##### 方案说明
- Reactor 主线程 MainReactor 对象通过 select 监听连接事件，收到事件后，通过 Acceptor 处理连接事件
- 当 Acceptor 处理连接事件后，MainReactor 将连接分配给 SubReactor
- SubReactor 将连接加入到**连接队列**进行监听，并创建 handler 进行各种事件处理
- 当有新事件发生时，SubReactor 就会调用对应的 handler 处理
- handler 通过 read 读取数据，分发给后面的 worker 线程处理
- worker 线程池分配独立的 worker 线程进行业务处理，并返回结果
- handler 收到响应的结果后，再通过 send 将结果返回给 client
- Reactor 主线程可以对应多个 Reactor 子线程，即 MainReactor 可以关联多个 SubReactor
##### 方案分析
- 优点：
  - 父线程与子线程的数据交互简单职责明确，父线程只需要接收新连接，子线程完成后续业务处理。
  - 父线程与子线程的数据交互简单，Reactor 主线程只需要把新连接传给子线程，子线程无需返回数据。
- 缺点：
  - 编程复杂度搞
- 应用场景：Nginx 主从 Reactor 多线程模型，Memcached 主从多线程，Nginx 主从多线程模型。
#### Netty 模型
##### 原理图 - 简单版
Netty 主要基于主从 Reactors 多线程模型做了一些改进，其中主从 Reactor 多线程模型有多个 Reactor
```
                                              -------------------------        -------------------------
                                             | BossGroup               |      | WorkerGroup             |
                                       ----> |  ----------   --------  |      |  ----------             |
                                      |      | | selector | | accept | |      | | selector |            |
                 ---------            |      |  ----------   --------  |      |  ----------             |
                | client1 |-----------|       -------------------------        -------------------------
                 ---------            |                  |                            ^             |
                                      |                  |                            |             |
                                      |                  V                            |             V
                 ---------            |            ---------------          ------------------    ---------
                | client2 |-----------            | SocketChannel | -----> | NIOSocketChannel |  | handler |
                 ---------                         ---------------          ------------------    ---------

1、BossGroup 线程维护 Selector，只关注 Acceptor
2、当接收到 Accept 事件，获取到对应的 SocketChannel，封装成 NIOSocketChannel 并注册到 Worker 线程（事件循环），并进行维护
3、当 Worker 监听到 selector 中通道发生自己感兴趣的事件后，就把事件交给对应的 handler 进行处理
```
##### 原理图 - 进阶版
```
               ---------------------                        ---------------------
              | ServerSocketChannel |                      | ServerSocketChannel |              
               ---------------------                        ---------------------
                        |                                            |
                  注册  V                                      注册   V
          --------------------------------           --------------------------------
         |  ----------------------------  |         |  ----------------------------  |
         | |    -------------------     | |         | |    -------------------     | |
         | |   |     selector      |    | |         | |   |     selector      |    | |
         | |    -------------------     | |         | |    -------------------     | |
         | |             |              | |         | |             |              | |
         | |             V              | |         | |             V              | |
         | |  ------------------------  | |         | |  ------------------------  | |
         | | |            ----------  | | |         | | |            ----------  | | |
         | | |           |          | | | |         | | |           |          | | | |
         | | |           V          | | | |         | | |           V          | | | |
         | | |  ------------------  | | | |         | | |  ------------------  | | | |
         | | | |   poll  select   | | | | |         | | | |   poll  select   | | | | |
         | | |  ------------------  | | | |         | | |  ------------------  | | | |
         | | |           |          | | | |         | | |           |          | | | |
         | | |           V          | | | |         | | |           V          | | | |
         | | |  ------------------  | | | |         | | |  ------------------  | | | |
         | | | |      handle      | | | | | ------> | | | |      handle      | | | | |
         | | |  ------------------  | | | |         | | |  ------------------  | | | |
         | | |           |          | | | |         | | |           |          | | | |
         | | |           V          | | | |         | | |           V          | | | |
         | | |  ------------------  | | | |         | | |  ------------------  | | | |
         | | | |    task queue    | | | | |         | | | |    task queue    | | | | |
         | | |  ------------------  | | | |         | | |  ------------------  | | | |
         | | |           |          | | | |         | | |           |          | | | |
         | | |            ----------  | | |         | | |            ----------  | | |
         | | |                        | | |         | | |                        | | |
         | | |        Thread          | | |         | | |        Thread          | | |
         | |  ------------------------  | |         | |  ------------------------  | |
         | |                            | |         | |                            | |
         | |        NioEventLoop        | |         | |        NioEventLoop        | |    
         |  ----------------------------  |         |  ----------------------------  |
         |                                |         |                                |
         |            BossGroup           |         |           WorkerGroup          |
          --------------------------------           --------------------------------

1、poll select ：轮询监听I/O事件
2、handle ：处理I/O事件
3、task queue ： 处理任务队列
```

##### 原理图 - 详细版
```
     ---------              --------------------------------                     -------------------------------- 
    | client1 |-----       | BossGroup                      |                   | WorkerGroup                    |
     ---------      |      |  ----------------------------  |                   |  ----------------------------  |
                    |------> |        NioEventGroup       | |           --------> |        NioEventGroup       | |
     ---------      |      | |  ----------   -----------  | |          |        | |  ----------   -----------  | |
    | client2 |-----|      | | | Selector | | TaskQueue | | |          |        | | | Selector | | TaskQueue | | |
     ---------      |      | |  ----------   -----------  | |          |        | |  ----------   -----------  | |
                    |      |  ----------------------------  |          |        |  ----------------------------  |
                    |       -----------------|--------------           |         -----------------|-------------- 
                    |                        |                         |                          |
                    |                        V accept                  | registe                  V read/send
     ---------      |                 ---------------                  |                    --------------- 
    | client3 |-----                 |     step1:    |                 |                   |     step1:    |
     ---------               ------> |     select    | ------          |           ------> |     select    | -------  
                            |         ---------------        |         |          |         ---------------         |                ------------------
                            |                                |         |          |                                 |               |     Pipeline     |
                            |                                |         |          |                                 |               |  --------------  |
                            |         NioEventLoop           | --------           |            NioEventLoop         | ------------> | |ChannelHandler| |
                            |                                |                    |                                 |               |  --------------  |
                            |                                |                    |                                 |               |      .......     |
                            |                                V                    |                                 V               |  --------------  |
                       -------------           ---------------------          -------------            ---------------------        | |ChannelHandler| |
                      |    step3:   |         |        step2:       |        |    step3:   |          |       step2:        |       |  --------------  |
                      | runAllTasks |  <----  | processSelectedKeys |        | runAllTasks |  <-----  | processSelectedKeys |        ------------------
                       -------------           ---------------------          -------------            ---------------------

```
##### 原理解析
- Netty 抽象出两组线程池：BossGroup 专门负责接收客户端的请求；WorkerGroup 专门负责网络的读写任务
- BossGroup 和 WorkerGroup 类型都是 NioEventLoopGroup
- NioEventLoopGroup 相当于一个事件循环组，这个组中包含有多个事件循环，每一个事件循环都是 NioEventLoop
- NioEventLoop 是一个不断循环执行 TaskHandler 的线程，每个 NioEventLoop 都有一个 selector，用于监听绑定在其上的 socket 的网络通讯
- NioEventLoopGroup 可以有多个线程，即可以包含多个 NioEventLoop
- 每个 Boss NioEventLoop 循环执行的步骤有 3 步：
  - 轮询 accept 事件
  - 处理 accept 事件，与 client 建立联系，生成 NioSocketChannel，并将其注册到某个 Worker NioEventLoop 上的 selector
  - 处理 TaskQueue 中的任务，即 runAllTasks
- 每个 Worker NioEventLoop 处理业务时会使用 pipeline（管道），pipeline 中包含了 channel，即通过 pipeline 可以获取到对应通道，管道中维护了很多的 handler
##### 实战案例
- 服务端代码演示

```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * @Author chaoyou
 * @Date Create in 13:16 2022/11/12
 * @Modified by
 * @Version 1.0.0
 * @Description 服务端
 */
public class NettyServer {
  public static void main(String[] args) {
    /**
     * 创建 BossGroup 和 WorkerGroup
     *
     *      1. 创建两个线程组 bossGroup 和 workerGroup
     *      2. bossGroup 只是处理连接请求 , 真正的和客户端业务处理，会交给 workerGroup 完成
     *      3. 两个都是无限循环
     *      4. bossGroup 和 workerGroup 含有的子线程(NioEventLoop)的个数
     *      5. 默认实际 cpu 核数 * 2
     */
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      // 创建服务器端的启动对象，配置参数
      ServerBootstrap bootstrap = new ServerBootstrap();
      // 使用链式编程来进行设置
      bootstrap.group(bossGroup, workerGroup) // 设置两个线程组
              .channel(NioServerSocketChannel.class) // 使用 NioSocketChannel 作为服务器的通道实现
              .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列得到连接个数
              .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持活动连接状态
              .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道测试对象(匿名对象)
                // 给 pipeline 设置处理器
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                  // 往 pipeline 通道中加入 handler
                  ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    /**
                     * 读取数据实现(这里我们可以读取客户端发送的消息)
                     *
                     * @param ctx 上下文对象, 含有 管道 pipeline , 通道 channel, 地址
                     * @param msg 就是客户端发送的数据 默认 Object
                     * @throws Exception
                     */
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                      System.out.println("服务器读取线程：" + Thread.currentThread().getName());
                      System.out.println("server ctx：" + ctx);
                      // 从上下文中取出管道（本质是一个双向链表）
                      ChannelPipeline pipeline = ctx.pipeline();
                      // 从上下文中获取通信通道
                      Channel channel = ctx.channel();
                      // 将 msg 转成一个 ByteBuf
                      // ByteBuf 是 Netty 提供的，不是 NIO 的 ByteBuffer.
                      ByteBuf buf = (ByteBuf) msg;
                      System.out.println("客户端发送消息是：" + buf.toString(CharsetUtil.UTF_8));
                      System.out.println("客户端地址：" + channel.remoteAddress());
                    }

                    /**
                     * 数据读取完毕才触发
                     *
                     * @param ctx
                     * @throws Exception
                     */
                    @Override
                    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                      // writeAndFlush 是 write + flush
                      // 将数据写入到缓存，并刷新
                      // 向客户端响应消息，对数据进行编码
                      ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵", CharsetUtil.UTF_8));
                    }

                    /**
                     * 处理异常, 一般是需要关闭通道
                     *
                     * @param ctx
                     * @param cause
                     * @throws Exception
                     */
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                      ctx.close();
                    }
                  });
                }
              }); // 给我们的 workerGroup 的 EventLoop 对应的管道设置处理器
      System.out.println(".....服务器 is ready...");
      // 绑定一个端口并且同步, 生成了一个 ChannelFuture 对象
      // 启动服务器(并绑定端口)
      ChannelFuture cf = bootstrap.bind(6688).sync();
      // 对关闭通道进行监听
      cf.channel().closeFuture().sync();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

- 客户端代码演示
```java
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * @Author chaoyou
 * @Date Create in 14:03 2022/11/12
 * @Modified by
 * @Version 1.0.0
 * @Description 客户端
 */
public class NettyClient {
    public static void main(String[] args) {
        //客户端需要一个事件循环组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建客户端启动对象
            // 注意客户端使用的不是 ServerBootstrap 而是 Bootstrap
            Bootstrap bootstrap = new Bootstrap();
            // 设置相关参数
            bootstrap.group(group) // 设置线程组
                    .channel(NioSocketChannel.class) // 设置客户端通道的实现类(反射)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                                /**
                                 * 当通道就绪就会触发该方法
                                 *
                                 * @param ctx
                                 * @throws Exception
                                 */
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("client：" + ctx);
                                    // 给服务端发送消息
                                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, server: (>^ω^<)喵", CharsetUtil.UTF_8));
                                }

                                /**
                                 * 当通道有读取事件时，会触发
                                 *
                                 * @param ctx
                                 * @param msg
                                 * @throws Exception
                                 */
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    ByteBuf buf = (ByteBuf) msg;
                                    System.out.println("服务器回复的消息:" + buf.toString(CharsetUtil.UTF_8));
                                    System.out.println("服务器的地址： "+ ctx.channel().remoteAddress());
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            }); // 加入自己的处理器
                        }
                    });
            System.out.println("客户端 ok..");
            // 启动客户端去连接服务器端
            // 关于 ChannelFuture 要分析，涉及到 netty 的异步模型
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 6688).sync();
            // 给关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}
```
##### 实战案例——改进版
任务队列中的 Task 有 3 种典型使用场景
- 用户程序自定义的普通任务
- 用户自定义定时任务
- 非当前 Reactor 线程调用 Channel 的各种方法
  - 例如在推送系统的业务线程里面，根据用户的标识，找到对应的 Channel 引用，然后调用 Write 类方法向该用户推送消息，就会进入到这种场景。最终的 Write 会提交到任务队列中后被异步消费
```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * @Author chaoyou
 * @Date Create in 13:16 2022/11/12
 * @Modified by
 * @Version 1.0.0
 * @Description 服务端
 */
public class NettyServer {
  public static void main(String[] args) {
    /**
     * 创建 BossGroup 和 WorkerGroup
     *
     *      1. 创建两个线程组 bossGroup 和 workerGroup
     *      2. bossGroup 只是处理连接请求 , 真正的和客户端业务处理，会交给 workerGroup 完成
     *      3. 两个都是无限循环
     *      4. bossGroup 和 workerGroup 含有的子线程(NioEventLoop)的个数
     *      5. 默认实际 cpu 核数 * 2
     */
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      // 创建服务器端的启动对象，配置参数
      ServerBootstrap bootstrap = new ServerBootstrap();
      // 使用链式编程来进行设置
      bootstrap.group(bossGroup, workerGroup) // 设置两个线程组
              .channel(NioServerSocketChannel.class) // 使用 NioSocketChannel 作为服务器的通道实现
              .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列得到连接个数
              .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持活动连接状态
              .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道测试对象(匿名对象)
                // 给 pipeline 设置处理器
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                  // 往 pipeline 通道中加入 handler
                  ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    /**
                     * 读取数据实现(这里我们可以读取客户端发送的消息)
                     *
                     * @param ctx 上下文对象, 含有 管道 pipeline , 通道 channel, 地址
                     * @param msg 就是客户端发送的数据 默认 Object
                     * @throws Exception
                     */
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                      /**
                       * 比如这里我们有一个非常耗时长的业务-> 异步执行 -> 提交该 channel 对应的 NIOEventLoop 的 taskQueue 中
                       */

                      // 解决方案 1 用户程序自定义的普通任务
                      ctx.channel().eventLoop().execute(new Runnable() {
                        @Override
                        public void run() {
                          try {
                            System.out.println("服务器读取线程 " + Thread.currentThread().getName());
                            System.out.println("server ctx =" + ctx);
                            System.out.println("看看 channel 和 pipeline 的关系");
                            Channel channel = ctx.channel();
                            // 本质是一个双向链接, 出站入站
                            ChannelPipeline pipeline = ctx.pipeline();
                            // 将 msg 转成一个 ByteBuf
                            // ByteBuf 是 Netty 提供的，不是 NIO 的 ByteBuffer.
                            ByteBuf buf = (ByteBuf) msg;
                            System.out.println("客户端发送消息是:" + buf.toString(CharsetUtil.UTF_8));
                            System.out.println("客户端地址:" + channel.remoteAddress());
                          } catch (Exception e){
                            logger.error("服务端异常信息：{}", e.getMessage());
                          }
                        }
                      });

                      // 解决方案 2 : 用户自定义定时任务
                      ctx.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                          try {
                            System.out.println("服务器读取线程 " + Thread.currentThread().getName());
                            System.out.println("server ctx =" + ctx);
                            System.out.println("看看 channel 和 pipeline 的关系");
                            Channel channel = ctx.channel();
                            // 本质是一个双向链接, 出站入站
                            ChannelPipeline pipeline = ctx.pipeline();
                            // 将 msg 转成一个 ByteBuf
                            // ByteBuf 是 Netty 提供的，不是 NIO 的 ByteBuffer.
                            ByteBuf buf = (ByteBuf) msg;
                            System.out.println("客户端发送消息是:" + buf.toString(CharsetUtil.UTF_8));
                            System.out.println("客户端地址:" + channel.remoteAddress());
                          } catch (Exception e){
                            logger.error("服务端异常信息：{}", e.getMessage());
                          }
                        }
                      }, 5, TimeUnit.SECONDS);
                    }

                    /**
                     * 数据读取完毕才触发
                     *
                     * @param ctx
                     * @throws Exception
                     */
                    @Override
                    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                      // writeAndFlush 是 write + flush
                      // 将数据写入到缓存，并刷新
                      // 向客户端响应消息，对数据进行编码
                      ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵", CharsetUtil.UTF_8));
                    }

                    /**
                     * 处理异常, 一般是需要关闭通道
                     *
                     * @param ctx
                     * @param cause
                     * @throws Exception
                     */
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                      ctx.close();
                    }
                  });
                }
              }); // 给我们的 workerGroup 的 EventLoop 对应的管道设置处理器
      System.out.println(".....服务器 is ready...");
      // 绑定一个端口并且同步, 生成了一个 ChannelFuture 对象
      // 启动服务器(并绑定端口)
      ChannelFuture cf = bootstrap.bind(6688).sync();
      // 对关闭通道进行监听
      cf.channel().closeFuture().sync();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```
