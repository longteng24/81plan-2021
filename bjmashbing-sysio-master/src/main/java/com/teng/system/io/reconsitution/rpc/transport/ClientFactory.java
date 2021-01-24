package com.teng.system.io.reconsitution.rpc.transport;

import com.sun.org.apache.xpath.internal.objects.XNull;
import com.teng.system.io.netty.SerDerUtil;
import com.teng.system.io.reconsitution.rpc.protocol.MyContent;
import com.teng.system.io.reconsitution.rpc.protocol.MyHeader;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.net.*;
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

        //content 就是货物 现在可以用自定义的rpc传输协议（有状态） ，也可以用http协议作为载体传输
        //先手工用http作为协议载体，那这样是不是可以让provider 是一个tomcat jetty 基于http协议
        // 有无状态来自于你使用的什么协议， 那么http 协议肯定是无状态， 每个请求对应一个连接
        //dubbo 是一个rpc 框架，  netty是一个io框架
        //dubbo 中传输协议上，可以是自定义的rpc传输协议，http协议


//        String type="rpc"
        String type = "http";

        CompletableFuture<Object>  res = new CompletableFuture<>();
        if ("rpc".equals(type)) {
            byte[] msgBody = SerDerUtil.ser(content);
            //2.requestId+message ,本地要缓存    协议：【header< >】 【msgBody】
            MyHeader header = MyHeader.createHeader(msgBody);
            byte[] msgHeader = SerDerUtil.ser(header);
            System.out.println("msgHeader length :" + msgHeader.length);


            NioSocketChannel clientChannel = factory.getClient(new InetSocketAddress("localhost", 9090));

            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(msgHeader.length + msgBody.length);

            long id = header.getRequestId();

            ResponseMappingCallback.addCallBack(id, res);


            byteBuf.writeBytes(msgHeader);
            byteBuf.writeBytes(msgBody);
            ChannelFuture channelFuture = clientChannel.writeAndFlush(byteBuf);

        } else {
            //使用 http协议为载体
            // 1.使用 URL  现成工具（包含http 的编解码，发送 ，socket, 连接）

        //    urlTs(content, res);

            //2.自己操心 ： on netty (io 框架)+ 已经提供的 http 相关编解码
            nettyTs(content, res);
        }



        return res;

    }

    private static void nettyTs(MyContent content, CompletableFuture<Object> res) {
        //在这个执行之前 provider 端已经开发完了， 已经 on netty 的http server 了
        //现在做的事consumer 端的代码修改，改为 on netty的http client

        //1.通过netty 建立io连接
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        Bootstrap client = bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {

                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(1024 * 512))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {       //3.接收  预埋回调，根据netty socket io 事件响应
                                        //3.接收  预埋回调，根据netty socket io 事件响应
                                         //客户端的msg是啥： 完整的http-response

                                        FullHttpResponse response=   (FullHttpResponse)msg;
                                        System.out.println(response.toString());

                                        ByteBuf resContent = response.content();
                                        byte[] data = new byte[resContent.readableBytes()];
                                        resContent.readBytes(data);

                                        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(data));
                                        MyContent o = (MyContent)oin.readObject();


                                        res.complete(o.getRes());
                                    }

                                });


                    }
                });

        try {
          ChannelFuture syncFuture=  client.connect("localhost", 9090).sync();

            //2.发送
            Channel clientChannel = syncFuture.channel();

            byte[] data = SerDerUtil.ser(content);

            DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_0,
                    HttpMethod.POST, "/",
                    Unpooled.copiedBuffer(data));

            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.length);

            clientChannel.writeAndFlush(request).sync(); //作为client ,想server端发送 ：http request

        } catch (InterruptedException e) {
            e.printStackTrace();
        }








    }

    private static void urlTs(MyContent content, CompletableFuture<Object> res) {
        //这种方式是每请求占用一个连接的方式，因为使用的http协议
        Object obj =null;
        try {
            URL url = new URL("http://localhost:9090/");
            HttpURLConnection hc = (HttpURLConnection)url.openConnection();

            hc.setRequestMethod("POST");
            //设置有请求体
            hc.setDoOutput(true);
            hc.setDoInput(true);

            OutputStream out = hc.getOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeObject(content);

            //等待服务端返回结果
            if (hc.getResponseCode() == 200) {
                InputStream in = hc.getInputStream();
                ObjectInputStream oin = new ObjectInputStream(in);
                MyContent myContent = (MyContent)oin.readObject();
                obj = myContent.getRes();
            }




        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        res.complete(obj);

    }

}