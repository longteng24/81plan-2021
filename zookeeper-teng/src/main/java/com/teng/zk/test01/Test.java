package com.teng.zk.test01;

import org.apache.zookeeper.*;

import java.io.IOException;

public class Test {
    // 会话超时时间，设置为与系统默认时间一致
    private static final int SESSION_TIMEOUT = 30 * 1000;

    // 创建 ZooKeeper 实例
    private ZooKeeper zk;

    // 创建 Watcher 实例
    private Watcher wh = new Watcher() {
        /**
         * Watched事件
         */
        public void process(WatchedEvent event) {
            System.out.println("WatchedEvent >>> " + event.toString());
        }
    };

    // 初始化 ZooKeeper 实例
    private void createZKInstance() throws IOException {
        // 连接到ZK服务，多个可以用逗号分割写
        zk = new ZooKeeper("localhost:2181,localhost:2182,localhost:2183,localhost:2184", Test.SESSION_TIMEOUT, this.wh);

    }

    private void ZKOperations() throws IOException, InterruptedException, KeeperException {
        //权限： OPEN_ACL_UNSAFE ，节点类型： Persistent
        zk.create("/test", "lhc0512".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        //查看
        System.out.println(new String(zk.getData("/test", this.wh, null)));//lhc0512

        // 修改
        zk.setData("/test", "llhhcc".getBytes(), -1);//WatchedEvent >>> WatchedEvent state:SyncConnected type:NodeDataChanged path:/test

        // 这里再次进行修改，则不会触发Watch事件，这就是我们验证ZK的一个特性“一次性触发”
        zk.setData("/test", "llhhcc0512".getBytes(), -1);

        //查看
        System.out.println(new String(zk.getData("/test", false, null)));//llhhcc0512

        //删除
        zk.delete("/test", -1);

        //查看是否删除
        System.out.println(" 节点状态： [" + zk.exists("/test", false) + "]");// 节点状态： [null]
    }

    //关闭
    private void ZKClose() throws InterruptedException {
        zk.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        Test dm = new Test();
        dm.createZKInstance();
        dm.ZKOperations();
        dm.ZKClose();
    }
}