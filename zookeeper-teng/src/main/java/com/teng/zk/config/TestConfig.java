package com.teng.zk.config;

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
public class TestConfig {

    ZooKeeper zk;

    @Before
    public void conn() {
        zk = ZKUtils.getZK();
    }

    @Test
    public void getConf() {

        WatchCallBack wcb = new WatchCallBack();
        // ע�뵱ǰzkʵ��
        wcb.setZk(zk);
        // ��ȡ�����ݶ���
        MyConf myConf = new MyConf();
        wcb.setConf(myConf);


         wcb.aWait();
         //1.�ڵ㲻����
        // 2.�ڵ����


        // ҵ����
        while (true) {

            //�����ñ����ʱ�������ȴ���ȡ����
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
