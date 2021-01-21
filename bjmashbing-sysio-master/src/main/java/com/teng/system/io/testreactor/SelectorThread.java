package com.teng.system.io.testreactor;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @program: 81plan
 * @description: 每个线程对应一个selector
 * @author: Mr.Teng
 * @create: 2021-01-21 21:23
 **/
public class SelectorThread implements  Runnable {
    //每个线程对应一个Selector
    //多线程情况下，该主机。该程序的并发客户端被分配到多个selectors上
    //注意每个客户端，只绑定到其中一个selector
    //其实不会有交互问题

    Selector selector=null;

    LinkedBlockingDeque<Channel> lbq = new LinkedBlockingDeque<>();

    SelectorThread() {

        try {
            //epoll_create
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //loop
        while (true) {
            try {
                //1.select()
                System.out.println(Thread.currentThread().getName() + ":before select..." + selector.keys().size());
                int nums = selector.select(); //阻塞 wakeup()叫醒

//                Thread.sleep(1000);  这绝对不会解决方案
                 System.out.println(Thread.currentThread().getName() + ":after select..." + selector.keys().size());

                //2.处里selectkeys
                if (nums > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isAcceptable()) {  //复杂，接收客户端过程（接收之后，要注册。多线程下，新客户端注册到哪里）
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        } else if (key.isWritable()) {

                        }

                    }

                }

                //3.处理一些task ， 注册 server client
                if (!lbq.isEmpty()) {
                    Channel c = lbq.take();
                    if (c instanceof ServerSocketChannel) {
                        ServerSocketChannel server = (ServerSocketChannel) c;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                    } else if (c instanceof SocketChannel) {
                        SocketChannel client = (SocketChannel) c;
                        ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                    }
                }



            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }

    private void readHandler(SelectionKey key) {
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel client =(SocketChannel) key.channel();
        buffer.clear();
        while (true) {
            int num = 0;
            try {
                num = client.read(buffer);
                if (num > 0) {
                    buffer.flip(); //将读到的内容翻转，然后直接写出
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (num == 0) {
                    break;
                } else if (num < 0) {
                    //客户端断开
                    System.out.println("client:"+client.getRemoteAddress()+"closed...");
                    key.cancel();
                    break;
                }



            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private void acceptHandler(SelectionKey key) {

        ServerSocketChannel server =(ServerSocketChannel) key.channel();

        try {
            System.out.println("acceptHandler....");
            SocketChannel client = server.accept();
            client.configureBlocking(false);

            //choose a selector and register!!!


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
