package com.teng.zk.config;

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
public class TestConfig {

    ZooKeeper zk;

    @Before
    public void conn() {
        zk = ZKUtils.getZK();
    }

    @Test
    public void getConf() {

        WatchCallBack wcb = new WatchCallBack();
        // 注入当前zk实例
        wcb.setZk(zk);
        // 获取的数据对象
        MyConf myConf = new MyConf();
        wcb.setConf(myConf);


         wcb.aWait();
         //1.节点不存在
        // 2.节点存在


        // 业务处理
        while (true) {

            //当配置被清空时，阻塞等待获取数据
            if ("".equals(myConf.getConf())) {
                wcb.aWait();
            } else {

                System.out.println(myConf.getConf());
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @After
    public void close() {
        try{
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
