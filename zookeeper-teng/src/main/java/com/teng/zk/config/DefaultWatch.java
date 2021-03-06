package com.teng.zk.config;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.CountDownLatch;

/**
 * @program: bigdata-redis
 * @description:
 * @author: Mr.Teng
 * @create: 2021-03-06 15:05
 **/
public class DefaultWatch  implements Watcher {

    private   CountDownLatch  cd;


    public void setCd(CountDownLatch cd) {
        this.cd = cd;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {

        System.out.println(watchedEvent.toString());

        switch (watchedEvent.getState()) {
            case Unknown:
                break;
            case Disconnected:
                break;
            case NoSyncConnected:
                break;
            case SyncConnected:
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

    }
}
