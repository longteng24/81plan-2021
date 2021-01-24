package com.teng.system.io.rpc;


import com.teng.system.io.reconsitution.proxy.MyProxy;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
        Dispatcher dis = new Dispatcher();

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
//                        System.out.println("server accept client port "+ ch.remoteAddress().getPort());
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

        new Thread(() ->{
            startServer();
        }).start();

        System.out.println("server started.....");

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

//        Fly fly = proxyGet(Fly.class);//动态代理实现
//        fly.xxoo("hello");
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


class Dispatcher{


    public static ConcurrentHashMap<String, Object> invokeMap = new ConcurrentHashMap<>();

    public void register(String k, Object obj) {
        invokeMap.put(k, obj);
    }

    public Object get(String k) {
        return invokeMap.get(k);

    }
}

class MyCar implements  Car{
    @Override
    public String ooxx(String msg) {
        System.out.println("server ,get client arg :"+ msg);
        return "server res :" + msg;
    }
}

class MyFly implements  Fly{
    @Override
    public void xxoo(String msg) {
        System.out.println("server  xxoo  ,get client arg :"+ msg);
//        return "server res :" + msg;
    }
}

interface Car {
     String ooxx(String msg);
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
                        p.addLast(new ServerDecode());
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


class ResponseMappingCallback {
  static   ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<>();

    public static void  addCallBack(long requestId, CompletableFuture cb) {
        mapping.putIfAbsent(requestId, cb);
    }

    public static void runCallBack(PackageMsg msg) {
        CompletableFuture cf = mapping.get(msg.header.getRequestId());
//        runnable.run();

        //complete方法意思就是这个任务完成了需要返回的结果  get()获取结果
        cf.complete(msg.content.getRes());
        removeCB(msg.header.getRequestId());

    }

    private static void removeCB(long requestId) {
        mapping.remove(requestId);
    }

}


class ServerDecode extends ByteToMessageDecoder{

    //父类里一定要 channelread { 前老的拼buf }->byteBuf  decode():剩余留存 ；对out遍历
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

   //     System.out.println("channel start :"+buf.readableBytes());
        while (buf.readableBytes() >= 124) {
            byte[] bytes = new byte[124];
            buf.getBytes(buf.readerIndex(),bytes); //从哪读，不会移动指针
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader   header= (MyHeader)oin.readObject();
         //   System.out.println("server   dataLen"+header.dataLen);
         //   System.out.println(" server    getRequestId"+header.getRequestId());

      //Decode在两个方向都使用
            //通信协议
            if (buf.readableBytes() >= header.getDataLen()) {
                //处理指针  移动指针到body开始位位置   保证buf读 整个头和体
                buf.readBytes(124);

                byte[] data = new byte[(int)header.getDataLen()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream doin = new ObjectInputStream(din);


                if(header.getFlag()==0x14141414){
                    MyContent   content= (MyContent)doin.readObject();

                    out.add(new PackageMsg(header, content));
                }else if(header.getFlag()==0x14141424){
                    MyContent   content= (MyContent)doin.readObject();

                    out.add(new PackageMsg(header, content));
                }


            }else{
               break;
            }

        }


    }
}

class ServerRequestHandler extends ChannelInboundHandlerAdapter {

    Dispatcher dis;

    public ServerRequestHandler(Dispatcher dis ) {
        this.dis = dis;
    }

    //consumer...
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PackageMsg requestPkg = (PackageMsg) msg;
//        System.out.println(" PackageMsg.getName()"+ requestPkg.content.getArgs()[0]);

        //假如处理完了，要给客户端返回了   要注意哪些环节   byteBuf  requestId
        // 解决解码问题  关注通信协议  来的时候 flag 0x14141414


        // 回写，有新 header+content
        String ioThreadName = Thread.currentThread().getName();

        // 1.直接在当前方法，处理IO 和业务返回
        // 3.自己创建线程池
        //2.使用netty自己的eventLoop来处理业务

//        ctx.executor().execute(new Runnable() {      //放入当前线程自己的阻塞对列中执行
        ctx.executor().parent().next().execute(new Runnable() {   //交给线程组中其他线程执行 ，打散到其他线程处理
            @Override
            public void run() {


                String name = requestPkg.content.getName();
                String method = requestPkg.content.getMethodName();
                Object c = dis.get(name);
                Class<?> clazz = c.getClass();
                Object res = null ;
                Method m = null;
                try {
                    m = clazz.getMethod(method, requestPkg.content.parameterTypes);
                     res = m.invoke(c, requestPkg.content.getArgs());
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
                MyContent content = new MyContent();

                content.setRes((String)res);

                byte[] contentByte = SerDerUtil.ser(content);


                MyHeader resHeader = new MyHeader();
                resHeader.setRequestId(requestPkg.header.getRequestId());
                resHeader.setFlag(0x14141424);
                resHeader.setDataLen(contentByte.length);

                byte[] headerByte = SerDerUtil.ser(resHeader);
                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(headerByte.length + contentByte.length);
                byteBuf.writeBytes(headerByte);
                byteBuf.writeBytes(contentByte);

                ctx.writeAndFlush(byteBuf);
            }
        });



    /*    ChannelFuture channelFuture = ctx.writeAndFlush( sendBuf);
        channelFuture.sync();
*/

//
    }
}


class ClientResponses extends ChannelInboundHandlerAdapter {


    //consumer...
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PackageMsg responseMsg = (PackageMsg) msg;

        ResponseMappingCallback.runCallBack(responseMsg);
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

class PackageMsg {

    MyHeader header;
    MyContent content;

    public PackageMsg(MyHeader header, MyContent content) {
        this.content = content;
        this.header = header;
    }

    public MyHeader getHeader() {
        return header;
    }

    public void setHeader(MyHeader header) {
        this.header = header;
    }

    public MyContent getContent() {
        return content;
    }

    public void setContent(MyContent content) {
        this.content = content;
    }
}

class MyContent implements Serializable {
    String name;
    String methodName;
    Class<?>[] parameterTypes;
    Object[] args;
    String res;

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

    public void setRes(String res) {
        this.res = res;
    }

    public String getRes() {
        return res;
    }
}

class SerDerUtil {
    static ByteArrayOutputStream out = new ByteArrayOutputStream();

    public synchronized static byte[] ser(Object msg) {
        out.reset();
        ObjectOutputStream oout = null;
        byte[] msgBody=null;
        try {
            oout = new ObjectOutputStream(out);
            oout.writeObject(msg);
            msgBody = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
//
        return msgBody;
    }
}