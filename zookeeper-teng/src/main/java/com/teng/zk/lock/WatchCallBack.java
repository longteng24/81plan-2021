package com.teng.zk.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @program: bigdata-redis
 * @description:
 * @author: Mr.Teng
 * @create: 2021-03-06 16:03
 **/
public class WatchCallBack implements Watcher,AsyncCallback.StringCallback, AsyncCallback.ChildrenCallback, AsyncCallback.StatCallback {


    private ZooKeeper zk;

    private String threadName;

    private CountDownLatch cdl = new CountDownLatch(1);

    private String pathName;

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public ZooKeeper getZk() {
        return zk;
    }

    public void setZk(ZooKeeper zk) {
        this.zk = zk;
    }

    public void tryLock() {

        try {

            System.out.println(threadName+"create........ing ");
            // 创建有序的零时节点
            zk.create("/lock", threadName.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL, this, "teng");



            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void unLock() {
        try {


            // -1 表示忽略版本
            zk.delete(pathName,-1);
            System.out.println("delete..."+ pathName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void process(WatchedEvent watchedEvent) {

        //如果第一个人锁释放了，只有第二个人才能收到事件
        //如果中间某个线程挂了，后面线程会监听到这个事件，并watch这个挂掉的线程  前面的线程
        switch (watchedEvent.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                //节点删除, 继续处理所有子节点
                zk.getChildren("/", this, this, "teng");

                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
        }


    }

    /**
     * create 产生事件的回调
     * @param i
     * @param s
     * @param o
     * @param s1
     */
    @Override
    public void processResult(int i, String s, Object o, String s1) {
        if (s1 != null) {
            System.out.println(threadName+"create node : "+s1);
            this.pathName = s1;
            //获取当前父目录下所有元素   , 不需要监控， 设置回调
            zk.getChildren("/", false, this, "getChildren..");
        }
    }

    /**
     * getChildren 的异步回调
     * @param i
     * @param s
     * @param o
     * @param list
     */
    @Override
    public void processResult(int i, String s, Object o, List<String> list) {
        // 一定能看到自己前面的

//        System.out.println(threadName+"look cur children...");
//
//        for (String s1 : list) {
//            System.out.println(s1);
//        }
        Collections.sort(list);
        int index = list.indexOf(pathName.substring(1));


        //是不是第一个，第一个就执行业务逻辑， 其他的则监控前一个
        if (index == 0) {

            try {
                // todo 实现重入锁
                zk.setData("/",threadName.getBytes(),-1);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cdl.countDown();
            System.out.println(threadName + "，i am first...");
        } else {

            //监控删除事件 ，同时监控节点状态
            zk.exists("/" + list.get(index - 1), this, this, "teng");
        }


    }

    /**
     * 监控节点状态
     *
     * @param i
     * @param s
     * @param o
     * @param stat
     */
    @Override
    public void processResult(int i, String s, Object o, Stat stat) {
       //判断
    }
}
