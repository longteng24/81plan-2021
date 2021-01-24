package com.teng.system.io.reconsitution;


import com.sean.io.test.service.Car;
import com.sean.io.test.service.Fly;
import com.sean.io.test.service.MyCar;
import com.sean.io.test.service.MyFly;
import com.teng.system.io.reconsitution.proxy.MyProxy;
import com.teng.system.io.reconsitution.rpc.Dispatcher;
import com.teng.system.io.reconsitution.rpc.transport.ServerDecode;
import com.teng.system.io.reconsitution.rpc.transport.ServerRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.io.IOException;
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
                        p.addLast(new ServerDecode());
                        p.addLast(new ServerRequestHandler(dis));
                    }
                }).bind(new InetSocketAddress("localhost", 9090));

        try {
            bind.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
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

}























