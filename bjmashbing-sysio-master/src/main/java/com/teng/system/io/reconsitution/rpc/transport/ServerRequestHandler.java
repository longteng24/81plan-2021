package com.teng.system.io.reconsitution.rpc.transport;


import com.teng.system.io.netty.SerDerUtil;
import com.teng.system.io.reconsitution.rpc.Dispatcher;
import com.teng.system.io.reconsitution.rpc.protocol.MyContent;
import com.teng.system.io.reconsitution.rpc.protocol.MyHeader;
import com.teng.system.io.reconsitution.rpc.util.PackageMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class ServerRequestHandler extends ChannelInboundHandlerAdapter {

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


                String name = requestPkg.getContent().getName();
                String method = requestPkg.getContent().getMethodName();
                Object c = dis.get(name);
                Class<?> clazz = c.getClass();
                Object res = null ;
                Method m = null;
                try {
                    m = clazz.getMethod(method, requestPkg.getContent().getParameterTypes());
                     res = m.invoke(c, requestPkg.getContent().getArgs());
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
                resHeader.setRequestId(requestPkg.getHeader().getRequestId());
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





