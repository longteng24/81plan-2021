package com.teng.system.io.reconsitution.rpc.transport;

import com.teng.system.io.netty.SerDerUtil;
import com.teng.system.io.reconsitution.rpc.protocol.MyContent;
import com.teng.system.io.reconsitution.rpc.protocol.MyHeader;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

//原语spark源码
public class ClientFactory {

    int poolSize = 5;

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
            synchronized(outBox) {
                if (clientPool == null) {
                    outBox.putIfAbsent(address, new ClientPool(poolSize));
                    clientPool = outBox.get(address);
                }

            }

        }

        int i = rend.nextInt(poolSize);
        if (clientPool.clients[i] != null && clientPool.clients[i].isActive()) {
            return clientPool.clients[i];
        }else {
            synchronized (clientPool.lock[i]) {
                if(clientPool.clients[i] ==null ||! clientPool.clients[i].isActive()){

                    clientPool.clients[i] = create(address);
                }
            }
        }
        return clientPool.clients[i];

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

    public static CompletableFuture<Object> transport(MyContent content) {
        byte[] msgBody = SerDerUtil.ser(content);
        //2.requestId+message ,本地要缓存    协议：【header< >】 【msgBody】
        MyHeader header = MyHeader.createHeader(msgBody);
        byte[] msgHeader =SerDerUtil.ser(header);
        System.out.println("msgHeader length :"+msgHeader.length);


        NioSocketChannel clientChannel = factory.getClient(new InetSocketAddress("localhost", 9090));

        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);

        long id = header.getRequestId();

        //用于等待异步方法处理完，接收返回值
        CompletableFuture<Object>  res = new CompletableFuture<>();

        ResponseMappingCallback.addCallBack(id, res);



        byteBuf.writeBytes(msgHeader);
        byteBuf.writeBytes(msgBody);
        ChannelFuture channelFuture = clientChannel.writeAndFlush(byteBuf);

        return res;

    }

}