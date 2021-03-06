package com.teng.zk.lock;


import com.teng.zk.config.ZKUtils;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @program: bigdata-redis
 * @description: 测试用zk   配置中心
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
                    //干活

                    String threadName = Thread.currentThread().getName();
                    wcb.setThreadName(threadName);
                    //强锁
                    wcb.tryLock();


                    //干活
                    System.out.println(threadName+" :do....");




                    //释放锁
                    wcb.unLock();
                }
            }.start();



        }
        while (true) {

        }
    }
}
