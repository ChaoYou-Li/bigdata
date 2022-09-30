# NIO入门
NIO：Non-Block I/O，意为非阻塞的I/O模型，而BIO（Block I/O）意为阻塞的I/O模型。

- BIO 以流的方式处理数据,而 NIO 以块的方式处理数据,块 I/O 的效率比流 I/O 高很多
- BIO 基于字节流和字符流进行操作，而 NIO 基于 Channel(通道)和 Buffer(缓冲区)进行操作，数据总是从通道读取到缓冲区中，或者从缓冲区写入到通道中。Selector(选择器)用于监听多个通道的事件（比如：连接请求，数据到达等），因此使用单个线程就可以监听多个客户端通道
## 三大核心组件
```
                                        --------
                                       | Thread |
                                        --------
                                            |
                                            V
                                       ----------
                                      | Selector |
                                       ----------
                                            |
                     -----------------------|-------------------------
                    |                       |                         |
                    V                       V                         V
                ---------               ---------                 ---------
               | Channel |             | Channel |               | Channel |
                ---------               ---------                 ---------
                    |                       |                         |
                    V                       V                         V
                ---------               ---------                 ---------
               |  Buffer |             |  Buffer |               |  Buffer |
                ---------               ---------                 ---------
                    |                       |                         |
                    V                       V                         V
                ---------               ---------                 ---------
               |   程序  |             |   程序  |               |   程序  |
                ---------               ---------                 ---------
```
- 每个 channel 都会对应一个 Buffer
- Selector 对应一个线程， 一个线程对应多个 channel(连接)
- 该图反应了有三个 channel 注册到 该 selector //程序
- 程序切换到哪个 channel 是有事件决定的, Event 就是一个重要的概念
- Selector 会根据不同的事件，在各个通道上切换
- Buffer 就是一个内存块 ， 底层是有一个数组
- 数据的读取写入是通过 Buffer, 这个和 BIO , BIO 中要么是输入流，或者是输出流, 不能双向，但是 NIO 的 Buffer 是可以读也可以写, 需要 flip 方法切换channel 是双向的, 可以返回底层操作系统的情况, 比如 Linux ， 底层的操作系统的通道也是双向的。
### Buffer
缓冲区（Buffer）：缓冲区本质上是一个可以读写数据的内存块，可以理解成是一个容器对象(含数组)，该对象提供了一组方法，可以更轻松地使用内存块，，缓冲区对象内置了一些机制，能够跟踪和记录缓冲区的状态变化情况。Channel 提供从文件、网络读取数据的渠道，但是读取或写入的数据都必须经由 Buffer。
#### 子类说明
Java的八大基本类型中除了boolean之外的其他七大类型均有缓冲类型：

- ByteBuffer
- CharBuffer
- ShortBuffer
- IntBuffer
- LongBuffer
- DoubleBuffer
- FloatBuffer
#### Buffer类解析
```java
package java.nio;

import java.util.Spliterator;

public abstract class Buffer {

    /**
     * Spliterators 的特点是遍历和拆分 Buffers 中维护的元素。
     */
    static final int SPLITERATOR_CHARACTERISTICS =
        Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;

    // Invariants: mark <= position <= limit <= capacity
    private int mark = -1;      // 标记
    private int position = 0;       // 位置，下一个读写操作元素的索引，每次对读写缓冲区数据时都会改变这个值，为下次读写做准备。
    private int limit;      // 表示缓冲区的操作终点，不能对缓冲区超过limit位置进行读写操作，且limit是可以被修改的。
    private int capacity;       // 容量，即可以容纳最大数据量；在缓冲区创建时被设定并且不能被修改

    /**
     * 返回缓冲区的容量值
     */
    public final int capacity() {
        return capacity;
    }

    /**
     * 返回此缓冲区的位置
     */
    public final int position() {
        return position;
    }

    /**
     * 设置此缓冲区的位置。 如果标记已定义且大于新位置，则将其丢弃。
     */
    public final Buffer position(int newPosition) {
        if ((newPosition > limit) || (newPosition < 0))
            throw createPositionException(newPosition);
        if (mark > newPosition) mark = -1;
        position = newPosition;
        return this;
    }

    /**
     * 返回次缓冲区的限制值
     */
    public final int limit() {
        return limit;
    }

    /**
     * 设置此缓冲区的限制。 如果头寸大于新限制，则将其设置为新限制。 如果标记已定义且大于新限制，则将其丢弃。
     */
    public final Buffer limit(int newLimit) {
        if ((newLimit > capacity) || (newLimit < 0))
            throw new IllegalArgumentException();
        limit = newLimit;
        if (position > newLimit) position = newLimit;
        if (mark > newLimit) mark = -1;
        return this;
    }

    /**
     * 将此缓冲区的标记设置在其位置。
     *
     * @return  This buffer
     */
    public final Buffer mark() {
        mark = position;
        return this;
    }

    /**
     * 将此缓冲区的位置重置为先前标记的位置。
     */
    public final Buffer reset() {
        int m = mark;
        if (m < 0)
            throw new InvalidMarkException();
        position = m;
        return this;
    }

    /**
     * 清除此缓冲区。 位置设置为零，限制设置为容量，标记被丢弃(数据并没有真正清除)
     */
    public final Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }

    /**
     * 翻转此缓冲区。 限制设置为当前位置，然后位置设置为零。 如果标记已定义，则将其丢弃。
     */
    public final Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * 回退这个缓冲区。 位置设置为零并且标记被丢弃。
     */
    public final Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * 返回当前位置和限制之间的元素数。
     */
    public final int remaining() {
        int rem = limit - position;
        return rem > 0 ? rem : 0;
    }

    /**
     * 告诉当前位置和限制之间是否有任何元素。
     */
    public final boolean hasRemaining() {
        return position < limit;
    }

    /**
     * 告诉这个缓冲区是否是只读的。
     */
    public abstract boolean isReadOnly();

    /**
     * 告知此缓冲区是否由可访问数组支持。
     */
    public abstract boolean hasArray();

    /**
     * 返回支持此缓冲区的数组
     */
    public abstract Object array();

    /**
     * 返回缓冲区第一个元素在此缓冲区的后备数组中的偏移量
     */
    public abstract int arrayOffset();

    /**
     * 告诉这个缓冲区是否是直接缓冲区
     */
    public abstract boolean isDirect();
}
```
#### 常用类型：ByteBuffer
```java
public abstract class ByteBuffer{
    /**
     * 创建缓冲区相关的
     */
    public static ByteBuffer allocateDirect(int capacity);      // 创建直接缓冲区
    public static ByteBuffer allocate(int capacity);      // 设置缓冲区的初始容量
    public static ByteBuffer wrap(byte[] array);    // 把一个数组放到缓冲区中使用
    public static ByteBuffer wrap(byte[] array, int offset, int length);    // 构造初始位置offset和上界length的缓冲区

    /**
     * 缓冲区存取相关API
     */
    public abstract byte get();     // 从当前位置position上get，get之后，position会自动+1
    public abstract byte get(int index);    // 从绝对位置get
    public abstract ByteBuffer put(byte b);     // 从当前位置添加，put之后，position会自动+1
    public abstract ByteBuffer put(int index, byte b);      // 从绝对位置上put
}
```
### Channel
- 通道是双向的，可以同时进行读写；流是单向的，只能读或者只能写
- 通道可以实现异步读写数据
- 通道可以从缓冲区读数据，也可以往缓冲区写数据
#### Channel接口
```java
package java.nio.channels;

import java.io.IOException;
import java.io.Closeable;

public interface Channel extends Closeable {

    /**
     * 判断这个通道是否开通
     */
    public boolean isOpen();

    /**
     * 关闭通道
     */
    public void close() throws IOException;
}
```
#### 常用实现
- FileChannel：FileChannel 用于文件的数据读写
- DataGramChannel：DatagramChannel 用于 UDP 的数据读写
- ServerSocketChannel：ServerSocketChanne 类似 ServerSocket，用于 TCP 的数据读写
- SocketChannel： SocketChannel 类似 Socket，用于 TCP 的数据读写
### Selector
#### 基本介绍
- Java 的 NIO，用非阻塞的 IO 方式。可以用一个线程，处理多个的客户端连接，就会使用到 Selector(选择器)
- Selector 能够检测多个注册的通道上是否有事件发生(注意:多个 Channel 以`事件`的方式可以注册到同一个Selector)，如果有事件发生，便获取事件然后针对每个事件进行相应的处理。这样就可以只用一个单线程去管理多个通道（请求连接）。
- 只有在 Channel 真正有读写事件发生时，才会进行读写，就大大地减少了系统开销，并且不必为每个连接都创建一个线程，不用去维护多个线程
- 避免了多线程之间的上下文切换导致的开销
#### 架构说明
```

                                        --------
                                       | Server |
                                        --------
                                            |
                                        --------
                                       | Thread |
                                        --------
                                            |
                                       ----------
                                      | Selector |
                                       ----------
                                            |
                     -----------------------|-------------------------
                    |                       |                         |
                ---------               ---------                 ---------
               | Channel |             | Channel |               | Channel |
                ---------               ---------                 ---------
                    |                       |                         |
                ---------               ---------                 ---------
               |  Buffer |             |  Buffer |               |  Buffer |
                ---------               ---------                 ---------
                    |                       |                         |
                ---------               ---------                 ---------
               |  Client |             |  Client |               |  Client |
                ---------               ---------                 ---------
```
- Netty 的 IO 线程 NioEventLoop 聚合了 Selector(选择器，也叫多路复用器)，可以同时并发处理成百上千个客户端连接。
- 当线程从某客户端 Socket 通道进行读写数据时，若没有数据可用时，该线程可以进行其他任务。
- 线程通常将非阻塞 IO 的空闲时间用于在其他通道上执行 IO 操作，所以单独的线程可以管理多个输入和输出通道。
- 由于读写操作都是非阻塞的，这就可以充分提升 IO 线程的运行效率，避免由于频繁 I/O 阻塞导致的线程挂起。
- 一个 I/O 线程可以并发处理 N 个客户端连接和读写操作，这从根本上解决了传统同步阻塞 I/O 一连接一线程模型，架构的性能、弹性伸缩能力和可靠性都得到了极大的提升。
#### Selector接口
```java
public abstract class Selector implements Closeable {
    /**
     * 初始化多路复用选择器对象
     */
    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /**
     * 获取选择器中所有的SelectionKeys
     */
    public abstract Set<SelectionKey> selectedKeys();

    /**
     * 监控所有注册到选择器的Channel，当有其中有I/O进来时，将对于Channel的SelectionKey加入到内部集合中并返回，参数用于设置监控间隔。
     */
    public abstract int select(long timeout)
        throws IOException;

    /**
     * 唤醒selector
     */
    public abstract Selector wakeup();

    /**
     * 不阻塞，立马返还
     */
    public abstract int selectNow() throws IOException;
}
```
#### NIO流程分析
- 当客户端连接时，会通过 ServerSocketChannel 得到 SocketChannel
- Selector 进行监听 select 方法, 返回有事件发生的通道的个数.
- 将 socketChannel 注册到 Selector 上, register(Selector sel, int ops), 一个 selector 上可以注册多个 SocketChannel
- 注册后返回一个 SelectionKey, 会和该 Selector 关联(集合)
- 进一步得到各个 SelectionKey (有事件发生)
- 在通过 SelectionKey 反向获取 SocketChannel , 方法 channel()
- 可以通过 得到的 channel , 完成业务处理
### SelectionKey
SelectionKey，表示 Selector 和网络通道的注册关系, 共四种：
- OP_ACCEPT：有新的网络连接可以 accept，值为 16
- OP_CONNECT：代表连接已经建立，值为 8
- OP_READ：代表读操作，值为 1
- OP_WRITE：代表写操作，值为 4
#### SelectionKey 源码解析
```java
public abstract class SelectionKey {

    /**
     * 返回与之关联的通道
     */
    public abstract SelectableChannel channel();

    /**
     * 返回与之关联的Selector对象
     */
    public abstract Selector selector();

    /**
     * 设置或改变监听事件
     */
    public abstract SelectionKey interestOps(int ops);

    /**
     * 代表读操作，值为 1
     */
    public static final int OP_READ = 1 << 0;

    /**
     * 代表写操作，值为 4
     */
    public static final int OP_WRITE = 1 << 2;

    /**
     * 代表连接已经建立，值为 8
     */
    public static final int OP_CONNECT = 1 << 3;

    /**
     * 有新的网络连接可以 accept，值为 16
     */
    public static final int OP_ACCEPT = 1 << 4;

    /**
     * 是否可读
     */
    public final boolean isReadable() {
        return (readyOps() & OP_READ) != 0;
    }

    /**
     * 是否可写
     */
    public final boolean isWritable() {
        return (readyOps() & OP_WRITE) != 0;
    }

    /**
     * 连接是否正常
     */
    public final boolean isConnectable() {
        return (readyOps() & OP_CONNECT) != 0;
    }

    /**
     * 是否可以accept
     */
    public final boolean isAcceptable() {
        return (readyOps() & OP_ACCEPT) != 0;
    }

    /**
     * 得到与之关联的共享数据
     */
    public final Object attachment() {
        return attachment;
    }
}
```
### ServerSocketChannel
ServerSocketChannel 在服务器端监听新的客户端 Socket 连接
#### ServerSocketChannel 源码解析
```java
public abstract class ServerSocketChannel
    extends AbstractSelectableChannel
    implements NetworkChannel {
    /**
     * 得到一个ServerSocketChannel通道对象
     */
    public static ServerSocketChannel open() throws IOException {
        return SelectorProvider.provider().openServerSocketChannel();
    }

    /**
     * 设置服务端端口号
     */
    public abstract ServerSocketChannel bind(SocketAddress local)
        throws IOException;

    /**
     * 接受一个连接，返回代表这个连接的通道对象
     */
    public abstract SocketChannel accept() throws IOException;
}
```
### SocketChannel
SocketChannel，网络 IO 通道，具体负责进行读写操作。NIO 把缓冲区的数据写入通道，或者把通道里的数据读到缓冲区。
#### SocketChannel 源码解析
```java
public abstract class SocketChannel
    extends AbstractSelectableChannel
    implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel{

    /**
     * 打开连接服务器的通道
     */
    public static SocketChannel open() throws IOException {
        return SelectorProvider.provider().openSocketChannel();
    }

    /**
     * 绑定远程服务器连接地址
     */
    public abstract SocketChannel bind(SocketAddress local)
        throws IOException;

    /**
     * 连接服务器
     */
    public abstract boolean connect(SocketAddress remote) throws IOException;

    /**
     * 完成连接套接字通道的过程（如果connect失败，可使用这个方法连接）
     */
    public abstract boolean finishConnect() throws IOException;

    /**
     * 从通道中读数据
     */
    public abstract int read(ByteBuffer dst) throws IOException;

    /**
     * 往通道中写数据
     */
    public abstract int write(ByteBuffer src) throws IOException;
}
```
## NIO与零拷贝
- 零拷贝是网络编程的关键，很多性能优化都离不开
- 在 Java 程序中，常用的零拷贝有 mmap(内存映射) 和 sendFile。那么，他们在 OS 里，到底是怎么样的一个的设计？我们分析 mmap 和 sendFile 这两个零拷贝
- 另外我们看下 NIO 中如何使用零拷贝
### 传统I/O模型
DMA: direct memory access 直接内存拷贝(不使用 CPU)
```
    ------------  DMA copy  ---------------  CPU copy  -------------  CPU copy  ---------------  DMA copy（3）  -----------------
   | hard drive | -------> | kernel buffer | -------> | user buffer | -------> | socket buffer | ------------> | protocol buffer |
    ------------            ---------------            -------------            ---------------                 -----------------
```
### mmap 优化
mmap 通过内存映射，将文件映射到内核缓冲区，用户空间可以共享内核空间的数据。在进行网络传输时，就可以减少内核空间到用户空间的拷贝次数

```
                             -------------
                            | user buffer |
                             ------------- 
                                   ^
                                   | shard
                                   |
    ------------  DMA copy  ---------------  CPU copy  ---------------  DMA copy（3）  -----------------
   | hard drive | -------> | kernel buffer | -------> | socket buffer | ------------> | protocol buffer |
    ------------            ---------------            ---------------                 -----------------
```
### sendFile 优化
Linux 2.1 版本 提供了 sendFile 函数，其基本原理如下：数据根本不经过用户态，直接从内核缓冲区进入到Socket Buffer，同时，由于和用户态完全无关，就减少了一次上下文切换
```
    ------------  DMA copy  ---------------  CPU copy  ---------------  DMA copy（3）  -----------------
   | hard drive | -------> | kernel buffer | -------> | socket buffer | ------------> | protocol buffer |
    ------------            ---------------            ---------------                 -----------------
```

Linux 在 2.4 版本中，做了一些修改，避免了从内核缓冲区拷贝到 Socket buffer 的操作，直接拷贝到协议栈，从而再一次减少了数据拷贝
```
                                   -------------------------------------------------------
                                  |                      DMA copy                         |
                                  |                                                       V
    ------------  DMA copy  ---------------  CPU copy  ---------------  DMA copy  -----------------
   | hard drive | -------> | kernel buffer | -------> | socket buffer | -------> | protocol buffer |
    ------------            ---------------            ---------------            -----------------
```
这里其实有 一次 cpu 拷贝kernel buffer -> socket buffer，但是拷贝的信息很少，比如 lenght , offset , 消耗低，可以忽略
### 零拷贝优势
- 是从操作系统的角度来说的。因为内核缓冲区之间，没有数据是重复的（只有 kernel buffer 有一份数据）
- 零拷贝不仅仅带来更少的数据复制，还能带来其他的性能优势，例如更少的上下文切换，更少的 CPU 缓存伪共享以及无 CPU 校验和计算
### mmap 和 sendFile 的区别
- mmap 适合小数据量读写，sendFile 适合大文件传输。
- mmap 需要 4 次上下文切换，3 次数据拷贝；sendFile 需要 3 次上下文切换，最少 2 次数据拷贝。
- sendFile 可以利用 DMA 方式，减少 CPU 拷贝，mmap 则不能（必须从内核拷贝到 Socket 缓冲区）。
### 零拷贝案例
```java
/**
 * @Description NIO—零拷贝：文件发送方
 */
public class CopyClient {
    public static void request(String path) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 7001));
        //得到一个文件 channel
        FileChannel fileChannel = new FileInputStream(path).getChannel();
        //准备发送
        long startTime = System.currentTimeMillis();
        //在 linux 下一个 transferTo 方法就可以完成传输
        //在 windows 下 一次调用 transferTo 只能发送 8m , 就需要分段传输文件, 而且要主要
        //传输时的位置 =》 课后思考...
        //transferTo 底层使用到零拷贝
        long transferCount = fileChannel.transferTo(0, fileChannel.size(), socketChannel);
        System.out.println("发送的总的字节数：" + transferCount + "，耗时：" + (System.currentTimeMillis() - startTime));
        //关闭
        fileChannel.close();
    }
}

/**
 * @Description NIO—零拷贝：文件接收方
 */
public class CopyServer {
    public static void response() throws IOException {
        InetSocketAddress address = new InetSocketAddress(7001);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(address);
        // 创建 buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            int readcount = 0;
            while (-1 != readcount) {
                try {
                    readcount = socketChannel.read(byteBuffer);
                }catch (Exception ex) {
                    break;
                }
                // 倒带 position = 0 mark 作废
                byteBuffer.rewind();
            }
        }
    }
}
```
