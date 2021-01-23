package com.teng.system.io.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

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
public class MyRPCTest {


    @Test
    public void startServer() {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker =boss;

        ServerBootstrap sbs = new ServerBootstrap();
        ChannelFuture bind = sbs.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        System.out.println("server accept client port "+ ch.remoteAddress().getPort());
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ServerRequestHandler());
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

        new Thread(() ->{
            startServer();
        }).start();

        System.out.println("server started.....");

        int size=50;
        Thread[] threads = new Thread[size];

        for (int i = 0; i < size; i++) {
            threads[i] = new Thread(() -> {
                Car car = proxyGet(Car.class);//动态代理实现
                car.ooxx("hello");
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

//        Fly fly = proxyGet(Fly.class);//动态代理实现
//        fly.xxoo("hello");
    }

    public static   <T> T proxyGet(Class<T> interfaceInfo) {
      //实现各自版本的动态代理

        ClassLoader loader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};



        return (T) Proxy.newProxyInstance(loader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 如何设计我们的consumer对于 provider的调用过程

                //1.调用  服务，方法，参数  =》封装成message  [content]
                String name = interfaceInfo.getName();

                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();

                MyContent content = new MyContent();

                content.setArgs(args);
                content.setName(name);
                content.setMethodName(methodName);
                content.setParameterTypes(parameterTypes);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.writeObject(content);
                byte[] msgBody = out.toByteArray();



                //2.requestId+message ,本地要缓存
                // 协议：【header< >】 【msgBody】

                MyHeader header = createHeader(msgBody);

                out.reset();
                 oout = new ObjectOutputStream(out);
                oout.writeObject(header);
                // TODO: 解决数据decode问题
                byte[] msgHeader = out.toByteArray();

         //       System.out.println("msgHeader length :"+msgHeader.length);



                //3.连接池：：取得连接
                ClientFactory factory = ClientFactory.getFactory();
                NioSocketChannel clientChannel = factory.getClient(new InetSocketAddress("localhost", 9090));
                //获取连接过程中： 开始-创建  连接-直接取


                // 4.发送 -->走IO out   走netty (event驱动)
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);

                CountDownLatch countDownLatch = new CountDownLatch(1);

                long id = header.getRequestId();
                ResponseHandler.addCallBack(id, new Runnable() {
                    @Override
                    public void run() {
                     countDownLatch.countDown();
                    }
                });



                byteBuf.writeBytes(msgHeader);
                byteBuf.writeBytes(msgBody);
                ChannelFuture channelFuture = clientChannel.writeAndFlush(byteBuf);
                channelFuture.sync();  // io双向，你看似sync ,她仅代表out



                countDownLatch.await();


                // 5. ？如果从IO， 未来回来了，怎么将代码执行到这里
                //(睡眠/回调，如果让线程停下来？ 你还能让他继续。。。)

                return null;
            }
        });
    }










    public static MyHeader createHeader(byte[] msg) {
        MyHeader header = new MyHeader();

        int size = msg.length;
        int f=0x14141414; // 32位规则  14八进制
        long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        //0x14 0001 0100
        header.setFlag(f);
        header.setDataLen(size);
        header.setRequestId(requestId);
        return header;
    }


}


class MyContent implements Serializable {
    String name;
    String methodName;
    Class<?>[] parameterTypes;
    Object[] args;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}


interface Car {
     void ooxx(String msg);
}

interface Fly {
    void xxoo(String msg);
}

//原语spark源码
class ClientFactory {

    int poolSize = 1;

    Random rend = new Random();

    NioEventLoopGroup clientWorker;

    private static  final ClientFactory factory;

    static {
        factory = new ClientFactory();
    }

    public static ClientFactory getFactory() {
        return factory;
    }

    private ClientFactory() {
    }

    //一个consumenr ,可以连接多个provider ,每一个provider都有自己的poll  k,v
    ConcurrentHashMap<InetSocketAddress, ClientPool> outBox = new ConcurrentHashMap<>();

    public NioSocketChannel getClient(InetSocketAddress address) {

        ClientPool clientPool = outBox.get(address);
        if (clientPool == null) {
            outBox.putIfAbsent(address, new ClientPool(poolSize));
            clientPool = outBox.get(address);
        }

        int i = rend.nextInt(poolSize);
        if (clientPool.clients[i] != null && clientPool.clients[i].isActive()) {
            return clientPool.clients[i];
        }

        synchronized (clientPool.lock[i]) {
            return clientPool.clients[i] = create(address);
        }
    }

    private NioSocketChannel create(InetSocketAddress address) {
        //基于netty 的客户端 创建方式
         clientWorker = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelFuture connect = bs.group(clientWorker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ClientResponses());


                    }
                }).connect(address);
        try {
            NioSocketChannel client = (NioSocketChannel)connect.sync().channel();
            return client;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

}




class ResponseHandler {
  static   ConcurrentHashMap<Long, Runnable> mapping = new ConcurrentHashMap<>();

    public static void  addCallBack(long requestId, Runnable cb) {
        mapping.putIfAbsent(requestId, cb);
    }

    public static void runCallBack(long requestId) {
        Runnable runnable = mapping.get(requestId);
        runnable.run();
        removeCB(requestId);

    }

    private static void removeCB(long requestId) {
        mapping.remove(requestId);
    }

}

class ServerRequestHandler extends ChannelInboundHandlerAdapter {


    //consumer...
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        ByteBuf sendBuf = buf.copy();

        if (buf.readableBytes() >= 103) {
            byte[] bytes = new byte[103];
            buf.readBytes(bytes);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader   header= (MyHeader)oin.readObject();
            System.out.println("server   dataLen"+header.dataLen);
            System.out.println(" server    getRequestId"+header.getRequestId());


            if (buf.readableBytes() >= header.getDataLen()) {
                byte[] data = new byte[(int)header.getDataLen()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream doin = new ObjectInputStream(din);
                MyContent   content= (MyContent)doin.readObject();
                System.out.println(" server  content.getName()"+content.getName());
            }

        }


        ChannelFuture channelFuture = ctx.writeAndFlush( sendBuf);
        channelFuture.sync();


    }
}

class ClientResponses extends ChannelInboundHandlerAdapter {


    //consumer...
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        if (buf.readableBytes() >= 103) {
            byte[] bytes = new byte[103];
            buf.readBytes(bytes);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader   header= (MyHeader)oin.readObject();
            System.out.println("client  dataLen"+header.dataLen);
            System.out.println(" client getRequestId"+header.getRequestId());

            //TODO:
                ResponseHandler.runCallBack(header.requestId);

//            if (buf.readableBytes() >= header.getDataLen()) {
//                byte[] data = new byte[(int)header.getDataLen()];
//                buf.readBytes(data);
//                ByteArrayInputStream din = new ByteArrayInputStream(data);
//                ObjectInputStream doin = new ObjectInputStream(din);
//                MyContent   content= (MyContent)doin.readObject();
//                System.out.println("content.getName()"+content.getName());
//            }

        }


        super.channelRead(ctx, msg);

    }
}

class MyHeader implements Serializable{
    //通信上的协议
    /**
     * 1.ooxx值
     * 2.UUID
     * 3.DATA_LIN
     */
    int flag; // 32bit可以设置很多信息
    long requestId;
    long dataLen;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getDataLen() {
        return dataLen;
    }

    public void setDataLen(long dataLen) {
        this.dataLen = dataLen;
    }
}

class ClientPool{

    NioSocketChannel [] clients;
    Object[] lock;

    ClientPool(int size) {
        clients = new NioSocketChannel[size];  // init 连接时空的
        lock = new Object[size];               //锁是可以初始化的
        for (int i = 0; i < size; i++) {
            lock[i] = new Object();
        }
    }

}