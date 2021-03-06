package com.teng.zk.demo01;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @program: bigdata-redis
 * @description:
 * @author: Mr.Teng
 * @create: 2021-03-06 13:49
 **/
public class zk01 {


    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

        //zk有session概念，无连接池概念
        //watch分两类   第一类：new zk时，传入watch  session级别，跟path ,node无关   异步连接
        //watch只发生在 读类型调用 exeits get
        CountDownLatch cd = new CountDownLatch(1);
        ZooKeeper zk = new ZooKeeper("localhost:2181,localhost:2182,localhost:2183,localhost:2184",
                3000, new Watcher() {
            //watch的回调方法
            @Override
            public void process(WatchedEvent event) {
                Event.KeeperState state = event.getState();
                Event.EventType type = event.getType();
                String path = event.getPath();
                System.out.println("new zk:"+event.toString());

                switch (state) {
                    case Unknown:
                        break;
                    case Disconnected:
                        break;
                    case NoSyncConnected:
                        break;
                    case SyncConnected:
                        //连接成功弄后结束阻塞
                        System.out.println("connected");
                        cd.countDown();
                        break;
                    case AuthFailed:
                        break;
                    case ConnectedReadOnly:
                        break;
                    case SaslAuthenticated:
                        break;
                    case Expired:
                        break;
                }

                switch (type) {
                    case None:
                        break;
                    case NodeCreated:
                        break;
                    case NodeDeleted:
                        break;
                    case NodeDataChanged:
                        break;
                    case NodeChildrenChanged:
                        break;
                }

            }
        });

        cd.await();

        ZooKeeper.States state = zk.getState();

        switch (state) {
            case CONNECTING:
                System.out.println("ing.............");
                break;
            case ASSOCIATING:
                break;
            case CONNECTED:
                System.out.println("ed.............");
                break;
            case CONNECTEDREADONLY:
                break;
            case CLOSED:
                break;
            case AUTH_FAILED:
                break;
            case NOT_CONNECTED:
                break;
        }

        // 1.目录  2.数据  3.权限控制  4.节点类型 EPHEMERAL随着session的
        String pathName = zk.create("/oxx", "olddata".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

        //元数据
        Stat stat = new Stat();
        // 1.目录 2.watcher
        byte[] node = zk.getData("/oxx", new Watcher() {
            // 这个路径下一次有事件时触发
            @Override
            public void process(WatchedEvent watchedEvent) {
                System.out.print("watchedEvent:");
                System.out.println(watchedEvent.toString());

                try {
                    // true表示 new zk的watch
//                    zk.getData("/oxx", true, stat);
                    zk.getData("/oxx", this, stat);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, stat);

        System.out.print("node:");
        System.out.println(new String(node));

        //触发get是设置的回调
        Stat stat1 = zk.setData("/oxx", "newData".getBytes(), 0);


        //是否还会触发  一次性
        Stat stat2 = zk.setData("/oxx", "newData01".getBytes(), stat1.getVersion());


        System.out.println("----async start------");
        // 异步回调  reactor
        zk.getData("/oxx", false, new AsyncCallback.DataCallback() {
            @Override
            public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {

                System.out.println("----async back------");
                System.out.println(o);
                System.out.println(new String(bytes));

            }
        }, "teng");
        System.out.println("---async over----");

         //  Thread.sleep(222222222);
    }

}
