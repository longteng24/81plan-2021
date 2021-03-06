package com.teng.zk.config;

import jdk.nashorn.internal.ir.CallNode;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @program: bigdata-redis
 * @description: zkπ§æﬂ¿‡
 * @author: Mr.Teng
 * @create: 2021-03-06 15:03
 **/
public class ZKUtils {


    private static ZooKeeper zk;

   // private static String address = "localhost:2181,localhost:2182,localhost:2183,localhost:2184/tengConf";
    private static String address = "localhost:2181,localhost:2182,localhost:2183,localhost:2184/tengLock";

    private static DefaultWatch watch = new DefaultWatch();

    private static CountDownLatch init = new CountDownLatch(1);

    public static ZooKeeper getZK() {

        try {
            zk = new ZooKeeper(address, 1000, watch);
            watch.setCd(init);

            init.await();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return zk;
    }
}
