package com.teng.zk.config;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * @program: bigdata-redis
 * @description:
 * @author: Mr.Teng
 * @create: 2021-03-06 15:14
 *
 * existe用的 statcallback , 读数据用 dataCallback
 **/
public class WatchCallBack implements Watcher, AsyncCallback.StatCallback,AsyncCallback.DataCallback {

   private ZooKeeper zk;

   private MyConf conf;

    CountDownLatch cdl = new CountDownLatch(1);

    public MyConf getConf() {
        return conf;
    }

    public void setConf(MyConf conf) {
        this.conf = conf;
    }

    public ZooKeeper getZk() {
        return zk;
    }

    public void setZk(ZooKeeper zk) {
        this.zk = zk;
    }

    public void aWait() {

        zk.exists("/AppConf",this,this , "TENG");
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void processResult(int i, String s, Object o, byte[] data, Stat stat) {
        if (data != null) {
            String s1 = new String(data);
            conf.setConf(s1);
            //读到数据时唤醒 当前线程
            cdl.countDown();
        }
    }

    @Override
    public void processResult(int i, String s, Object o, Stat stat) {
        if (stat != null) {
            zk.getData("/AppConf", this, this,"teng");
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {

        switch (watchedEvent.getType()) {
            case None:
                break;
            case NodeCreated:
                // 节点创建
                zk.getData("/AppConf", this, this,"teng");

                break;
            case NodeDeleted:
                // 节点删除   容忍性
                conf.setConf("");
                cdl = new CountDownLatch(1);
                break;
            case NodeDataChanged:
                //数据变更 ，取数据
                zk.getData("/AppConf", this, this,"teng");

                break;
            case NodeChildrenChanged:
                break;
        }
    }
}
