package com.teng.system.io.reconsitution;


import com.teng.system.io.netty.SerDerUtil;
import com.teng.system.io.reconsitution.proxy.MyProxy;
import com.teng.system.io.reconsitution.rpc.Dispatcher;
import com.teng.system.io.reconsitution.rpc.protocol.MyContent;
import com.teng.system.io.reconsitution.rpc.transport.MyHttpRpcHandler;
import com.teng.system.io.reconsitution.service.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: 81plan
 * @description: 1.先假设一个需求，写一个PPC
 *               2.来回通信，连接数量，拆包？
 *               3.动态代理， 序列化，协议封装
 *               4.连接池
 *               5.就像调用本地方法一样，调用远程方法，
 *            面向java中就是所谓的面向接口编程
 * @author: Mr.Teng
 * @create: 2021-01-23 14:44
 **/

/**
 *  能发送，
 *  问题：并发时通过一个连接发送后，服务端解析byteBuf 转 对象过程出错
 */
public class MyRPCTest {

    @Test
    public void startServer() {

        MyCar car = new MyCar();
        MyFly fly = new MyFly();
        Dispatcher dis =  Dispatcher.getDis();

        dis.register(Car.class.getName(),car);
        dis.register(Fly.class.getName(),fly);

        NioEventLoopGroup boss = new NioEventLoopGroup(50);
        NioEventLoopGroup worker =boss;

        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        System.out.println("server accept client port "+ ch.remoteAddress().getPort());
                        ChannelPipeline p = ch.pipeline();
                        //自定义 rpc
//                        p.addLast(new ServerDecode());
//                        p.addLast(new ServerRequestHandler(dis));

                        //1.自定义RPC,  粘包，拆包过程  2.小火车传输协议用地就是http,

                        p.addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024 * 512))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        //http协议 ，这个msg是一个 ： 完整的http-request
                                        FullHttpRequest request = (FullHttpRequest) msg;
                                        System.out.println(request.toString());

                                        //这个是consumer 序列化的 MyContent
                                        ByteBuf content = request.content();
                                        byte[] data = new byte[content.readableBytes()];
                                        content.readBytes(data);
                                        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(data));
                                        MyContent myContent = (MyContent) oin.readObject();

                                        String name = myContent.getName();
                                        String method = myContent.getMethodName();
                                        Object c = dis.get(name);
                                        Class<?> clazz = c.getClass();
                                        Object res = null ;
                                        Method m = null;
                                        try {
                                            m = clazz.getMethod(method,myContent.getParameterTypes());
                                            res = m.invoke(c, myContent.getArgs());
                                        } catch (NoSuchMethodException e) {
                                            e.printStackTrace();
                                        } catch (IllegalAccessException e) {
                                            e.printStackTrace();
                                        } catch (InvocationTargetException e) {
                                            e.printStackTrace();
                                        }

                                        String execThreadName = Thread.currentThread().getName();
//                String s = "io thread : " + ioThreadName + ",exec thread :"
//                        + execThreadName + " from args :" + requestPkg.content.getArgs()[0];
                                        //     System.out.println("s :"+s);
                                        MyContent resContent = new MyContent();

                                        resContent.setRes(res);

                                        byte[] contentByte = SerDerUtil.ser(resContent);

                                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                                                HttpVersion.HTTP_1_0,
                                                HttpResponseStatus.OK,
                                                Unpooled.copiedBuffer(contentByte));
                                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentByte.length);
                                        //http协议， header+body
                                        ctx.writeAndFlush(response);
                                    }


                                });
                    }
                }).bind(new InetSocketAddress("localhost", 9090));

        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void startHttpServer() {

        MyCar car = new MyCar();
        MyFly fly = new MyFly();
        Dispatcher dis =  Dispatcher.getDis();

        dis.register(Car.class.getName(),car);
        dis.register(Fly.class.getName(),fly);
        //tomcat jetty
        Server server = new Server(new InetSocketAddress("localhost", 9090));
        ServletContextHandler handler = new ServletContextHandler(server, "/");
        server.setHandler(handler);
        handler.addServlet(MyHttpRpcHandler.class,"/*"); //web.xml

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //模拟comsumer端
    @Test
    public void get() {

        AtomicInteger num = new AtomicInteger(0);
        int size=100;
        Thread[] threads = new Thread[size];

        for (int i = 0; i < size; i++) {
            threads[i] = new Thread(() -> {
                Car car = MyProxy.proxyGet(Car.class);//动态代理实现  是真的要实现RPC调用吗
                String arg = "hello" + num.incrementAndGet();
             String res=   car.ooxx(arg);
                System.out.println("client over msg: "+res+" ,src arg: "+arg);
            });

        }
        for (Thread thread : threads) {
            thread.start();
        }
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testRPC() {
        Car car = MyProxy.proxyGet(Car.class);
        Persion zhangsan = car.oxox("zhangsan", 16);
        System.out.println(zhangsan);
    }

}























