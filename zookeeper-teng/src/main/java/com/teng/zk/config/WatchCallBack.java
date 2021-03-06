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
 * existe�õ� statcallback , �������� dataCallback
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
            //��������ʱ���� ��ǰ�߳�
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
                // �ڵ㴴��
                zk.getData("/AppConf", this, this,"teng");

                break;
            case NodeDeleted:
                // �ڵ�ɾ��   ������
                conf.setConf("");
                cdl = new CountDownLatch(1);
                break;
            case NodeDataChanged:
                //���ݱ�� ��ȡ����
                zk.getData("/AppConf", this, this,"teng");

                break;
            case NodeChildrenChanged:
                break;
        }
    }
}
