package pf.bluemoon.com.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @Author chaoyou
 * @Date Create in 2022-12-16 11:41
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class NettyServerThreadHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerThreadHandler.class);

    /**
     * 读取数据实际(这里我们可以读取客户端发送的消息)
     *
     * @param ctx 上下文对象, 含有 管道 pipeline , 通道 channel, 地址
     * @param msg 就是客户端发送的数据 默认 Object
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        /**
         * 比如这里我们有一个非常耗时长的业务-> 异步执行 -> 提交该 channel 对应的 NIOEventLoop 的 taskQueue 中
         *
         * 解决方案 1 用户程序自定义的普通任务
         */
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
        // 一般讲，我们对这个发送的数据进行编码
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

}
