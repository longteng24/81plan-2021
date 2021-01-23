package com.teng.system.io.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @program: 81plan
 * @description: netty初级使用
 * @author: Mr.Teng
 * @create: 2021-01-23 09:51
 **/
public class MyNetty {

    /**
     * 目的：前边 noi 逻辑
     * 恶心版本 --- 依托着前面的思维逻辑
     * <p>
     * channel  bytebuffer selector
     * <p>
     * bytebuffer bytebuf [poll]
     *    读指针  写指针
     */

@Test
    public void myByteBuf() {
     // 第一个参数 为 初始分配空间， 第二个参数表示最大分配空间     初始空间分配 为8的倍数 ，不够则*2 等，直到最大
//    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20);
    // pool  池化的分配    控制 池化非池化 ，堆内或堆外
//    ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
     ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
    print(buf);


    buf.writeBytes(new byte[]{1, 2, 2, 3});
    print(buf);
    buf.writeBytes(new byte[]{1, 2, 2, 3});
    print(buf);
    buf.writeBytes(new byte[]{1, 2, 2, 3});
    print(buf);
    buf.writeBytes(new byte[]{1, 2, 2, 3});
    print(buf);
    buf.writeBytes(new byte[]{1, 2, 2, 3});
    print(buf);
    //超上限 IndexOutOfBoundsException  writerIndex(20) + minWritableBytes(4) exceeds maxCapacity(20
    buf.writeBytes(new byte[]{1, 2, 2, 3});
    print(buf);
}


    public static void print(ByteBuf buf) {
        System.out.println("isReadable    是否可读    "+buf.isReadable());
        System.out.println("readerIndex   可读头指针   "+buf.readerIndex());
        System.out.println("readableBytes 可读头字节数 "+buf.readableBytes());
        System.out.println("isWritable    是否可写     "+buf.isWritable());
        System.out.println("writerIndex   可写头指针   "+buf.writerIndex());
        System.out.println("writableBytes 可写多少个   "+buf.writableBytes());
        System.out.println("capacity      空间上限     "+buf.capacity());  //
        System.out.println("maxCapacity   最大空间上限  "+buf.maxCapacity());
        System.out.println("isDirect      是否堆外分配  "+buf.isDirect());
        System.out.println("--------------------------------");

    }


    /**
     * 客户端   telnet 客户端测试    nc -l 当做服务端
     * 连接别人
     * 1. 主动发送数据
     * 2.别人什么时候给我发？  event selector
     */
    @Test
    public void loopExecutor() throws IOException {
        // group 线程池
        NioEventLoopGroup selector = new NioEventLoopGroup(2);
        selector.execute(() ->{
            try {
                for (; ; ) {
                    System.out.println("hello world001");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        selector.execute(() ->{
            try {
                for (; ; ) {
                    System.out.println("hello world002");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.in.read();
    }


    /**
     * 手写客户端
     * @throws InterruptedException
     */
    @Test
    public void clientMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);

        // channel not registered to an event loop
        NioSocketChannel client = new NioSocketChannel();

        thread.register(client); // epoll_ctl( 5,add,3)

        //读取时流处理   响应式
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler());

        // 连接有了，未发送数据
        // reactor 异步特征  建立连接 和发送数据都是异步的  都需要用sync 阻塞
        ChannelFuture connect = client.connect(new InetSocketAddress("121.36.28.218", 9999));
        ChannelFuture sync = connect.sync();

        ByteBuf buf = Unpooled.copiedBuffer("hello server.".getBytes());
        // 写事件
        ChannelFuture send = client.writeAndFlush(buf);
        send.sync();

        //  等待关闭事件  阻塞客户端关闭
        sync.channel().closeFuture().sync();

        System.out.println("client over....");
    }




    @Test
    public void nettyClient() {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();

        ChannelFuture connect = bs.group(group)
                .channel(NioSocketChannel.class)
//                .handler(new ChannelInit())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new MyInHandler());

                    }
                })
                .connect(new InetSocketAddress("121.36.28.218", 9999));
        try {
            //同步 连接完成
            Channel client = connect.sync().channel();

            ByteBuf buf = Unpooled.copiedBuffer("hello  server.".getBytes());
            // 写事件
            ChannelFuture send = client.writeAndFlush(buf);
            send.sync();

            //  等待关闭事件  阻塞客户端关闭
            client.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void serverMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioServerSocketChannel server = new NioServerSocketChannel();

        thread.register(server);

        ChannelPipeline p = server.pipeline();
        p.addLast(new MyAcceptHandler(thread,new ChannelInit()));

        // 会将myInHandler 共享到所有客户端，只初始化了一次  。 会出现数据共享或错乱问题， 当两个客户端连接时， 报错
//        p.addLast(new MyAcceptHandler(thread,new MyInHandler()));
        //accept接收客户端，注册到指定 loopGroup , 并指定客户端处理器


        //异步的
        ChannelFuture bind = server.bind(new InetSocketAddress("192.168.1.6", 9999));
        // 等bind结束 ，并处理后续客户端关闭时退出
        bind.sync().channel().closeFuture().sync();
        System.out.println("server close....");


    }

    /**
     * 不涉及业务，只为了客户端处理的handler能注入
     * 帮助完成客户端 handler，在客户端注册是加入到客户端 pipeline里
     */
    @ChannelHandler.Sharable
    class ChannelInit extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            Channel client = ctx.channel();
            ChannelPipeline p = client.pipeline();
            p.addLast(new MyInHandler());   // 2. client :: pipeline [MyInHandler]

            //注册业务handler处理完成，移除自己
            ctx.pipeline().remove(this);
        }
    }



    /**
     * 就是用户自己实现， 您呢让用户放弃属性的操作吗
     *   @ChannelHandler.Sharable 不应该被压迫给coder
     */
  //  @ChannelHandler.Sharable   //被客户端共享， MyInHandler 在server中只new了一次
   // 默认识独享的  last handler in the pipeline did not handle the exception
    class MyInHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            System.out.println("client registered......");
        }


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("client active...");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
          ByteBuf buf=  (ByteBuf)msg;
//  会移动指针          CharSequence str=buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);
             CharSequence str=buf.getCharSequence(0,buf.readableBytes(), CharsetUtil.UTF_8);
            System.out.println(str );

            ctx.writeAndFlush(buf);

        }
    }

    class MyAcceptHandler extends ChannelInboundHandlerAdapter {


        private final EventLoopGroup selector;

        private final ChannelHandler handler;

        public MyAcceptHandler(EventLoopGroup thread , ChannelHandler myInHandler) {
            this.selector = thread;
            this.handler = myInHandler;

        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            System.out.println("server registered......");
        }


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("server active...");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // listen socket accept    Object msg ->client
            //socket R/W        Object msg ->byteBuf
            SocketChannel client=  (SocketChannel)msg;

             //响应式的 handler
            ChannelPipeline p = client.pipeline();
            p.addLast(handler);  // 1. client :: pipeline [ChannelInit]

            //注册
            selector.register(client); //当注册时，触发 ChannelInit 中 register事件
        }
    }

    @Test
    public void nettyServer() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ServerBootstrap bs = new ServerBootstrap();
        ChannelFuture bind = bs.group(group, group)
                .channel(NioServerSocketChannel.class)
//                .childHandler(new ChannelInit())
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new MyInHandler());
                    }
                })
                .bind(new InetSocketAddress("192.168.1.6", 8888));

        bind.sync().channel().closeFuture().sync();


    }
}


