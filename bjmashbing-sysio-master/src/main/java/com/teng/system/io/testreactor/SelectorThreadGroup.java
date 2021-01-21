package com.teng.system.io.testreactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: 81plan
 * @description:
 * @author: Mr.Teng
 * @create: 2021-01-21 21:44
 **/
public class SelectorThreadGroup {

    SelectorThread[] sts;
    ServerSocketChannel server = null;
    AtomicInteger xid = new AtomicInteger(0);

    SelectorThreadGroup(int num) {
        //num 线程数
        sts = new SelectorThread[num];
        for (int i = 0; i < num; i++) {
            sts[i] = new SelectorThread();

            new Thread(sts[i]).start();
        }
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            //注册到那个selector上呢
            nextSelector(server);



        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //无论serverSocket socket 都复用这个方法
    private void nextSelector(Channel c) {
        SelectorThread st = next();  // 在 main 线程中 ，取到堆里的selectorThread对象

        //1.通过队列传递数据 消息
        st.lbq.add(c);
        //2.通过打断阻塞，让对应的线程 自己在打断后完成注册 selector
        st.selector.wakeup();

        // 重点 ： c可能是server  或 client
//        ServerSocketChannel s = (ServerSocketChannel) c;
        //呼应上    int nums = selector.select(); //阻塞 wakeup()叫醒




/*        try {
            s.register(st.selector, SelectionKey.OP_ACCEPT);  // 会被阻塞  wakeup()

            st.selector.wakeup(); //功能是让 selector的select()方法,立刻返回不阻塞
//            System.out.println("aaa");

        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }*/


    }
    //无论serverSocket socket 都复用这个方法
    private SelectorThread next() {
        int index=xid.incrementAndGet() % sts.length;
        return sts[index];
    }
}
