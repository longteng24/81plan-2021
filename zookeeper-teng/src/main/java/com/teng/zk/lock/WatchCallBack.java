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
            // �����������ʱ�ڵ�
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


            // -1 ��ʾ���԰汾
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

        //�����һ�������ͷ��ˣ�ֻ�еڶ����˲����յ��¼�
        //����м�ĳ���̹߳��ˣ������̻߳����������¼�����watch����ҵ����߳�  ǰ����߳�
        switch (watchedEvent.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                //�ڵ�ɾ��, �������������ӽڵ�
                zk.getChildren("/", this, this, "teng");

                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
        }


    }

    /**
     * create �����¼��Ļص�
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
            //��ȡ��ǰ��Ŀ¼������Ԫ��   , ����Ҫ��أ� ���ûص�
            zk.getChildren("/", false, this, "getChildren..");
        }
    }

    /**
     * getChildren ���첽�ص�
     * @param i
     * @param s
     * @param o
     * @param list
     */
    @Override
    public void processResult(int i, String s, Object o, List<String> list) {
        // һ���ܿ����Լ�ǰ���

//        System.out.println(threadName+"look cur children...");
//
//        for (String s1 : list) {
//            System.out.println(s1);
//        }
        Collections.sort(list);
        int index = list.indexOf(pathName.substring(1));


        //�ǲ��ǵ�һ������һ����ִ��ҵ���߼��� ����������ǰһ��
        if (index == 0) {

            try {
                // todo ʵ��������
                zk.setData("/",threadName.getBytes(),-1);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cdl.countDown();
            System.out.println(threadName + "��i am first...");
        } else {

            //���ɾ���¼� ��ͬʱ��ؽڵ�״̬
            zk.exists("/" + list.get(index - 1), this, this, "teng");
        }


    }

    /**
     * ��ؽڵ�״̬
     *
     * @param i
     * @param s
     * @param o
     * @param stat
     */
    @Override
    public void processResult(int i, String s, Object o, Stat stat) {
       //�ж�
    }
}
