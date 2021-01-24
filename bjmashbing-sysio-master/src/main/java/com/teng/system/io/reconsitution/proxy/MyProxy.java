package com.teng.system.io.reconsitution.proxy;

import com.teng.system.io.netty.SerDerUtil;
import com.teng.system.io.reconsitution.rpc.Dispatcher;

import com.teng.system.io.reconsitution.rpc.protocol.MyContent;
import com.teng.system.io.reconsitution.rpc.protocol.MyHeader;
import com.teng.system.io.reconsitution.rpc.transport.ClientFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * @program: 81plan
 * @description: 代理类
 * @author: Mr.Teng
 * @create: 2021-01-24 10:31
 **/
public class MyProxy {

    public static   <T> T proxyGet(Class<T> interfaceInfo) {
        //实现各自版本的动态代理

        ClassLoader loader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};

        //todo LOCAL 实现 用到 dispatcher  直接返回，还是本地调用时也代理下    代理扩展性好，可插入监控，统计等

        Dispatcher dispatcher = Dispatcher.getDis();

        return (T) Proxy.newProxyInstance(loader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {


                Object res = null;

                Object o = dispatcher.get(interfaceInfo.getName());

                if (o == null) {
                    //走远程调用

                    //1.调用  服务，方法，参数  =》封装成message  [content]
                    String name = interfaceInfo.getName();

                    String methodName = method.getName();
                    Class<?>[] parameterTypes = method.getParameterTypes();

                    //todo rpc 就像小火车拉货  content是service的具体数据，但还需要header层完成IO传输控制
                    MyContent content = new MyContent();

                    content.setArgs(args);
                    content.setName(name);
                    content.setMethodName(methodName);
                    content.setParameterTypes(parameterTypes);

                    /**
                     * todo 未来小伙车可能会变
                     * 1.缺失注册发现 zk
                     * 2.第一层负载，面向provider
                     * 3.consumer 线程池 面向service  ;并发就有木桶，倾斜
                     *  serviceA
                     *    ipA:port
                     *      socket 1
                     *      socket 2
                     *    ipB:Port
                     */
                    CompletableFuture<Object> resF = ClientFactory.transport(content);
                    res= resF.get(); //阻塞的
                }else{
                    System.out.println("local FC....");
                    Class<?> clazz = o.getClass();

                    Method m = null;
                    try {
                        m = clazz.getMethod(method.getName(), method.getParameterTypes());
                        res = m.invoke(o, args);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }

                return  res;
            }
        });
    }
}
