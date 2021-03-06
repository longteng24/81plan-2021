package com.teng.zk.lock;


import com.teng.zk.config.ZKUtils;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @program: bigdata-redis
 * @description: ������zk   ��������
 * @author: Mr.Teng
 * @create: 2021-03-06 15:02
 **/
public class TestLock {

    ZooKeeper zk;

    @Before
    public void conn() {
        zk = ZKUtils.getZK();
    }



    @After
    public void close() {
        try{
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void getLock() {

        for (int i = 0; i <10; i++) {
            WatchCallBack wcb = new WatchCallBack();

            new Thread() {
                @Override
                public void run() {
                    wcb.setZk(zk);
                    //�ɻ�

                    String threadName = Thread.currentThread().getName();
                    wcb.setThreadName(threadName);
                    //ǿ��
                    wcb.tryLock();


                    //�ɻ�
                    System.out.println(threadName+" :do....");




                    //�ͷ���
                    wcb.unLock();
                }
            }.start();



        }
        while (true) {

        }
    }
}
