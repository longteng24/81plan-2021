package com.teng.zk.test01;

import org.apache.zookeeper.*;

import java.io.IOException;

public class Test {
    // �Ự��ʱʱ�䣬����Ϊ��ϵͳĬ��ʱ��һ��
    private static final int SESSION_TIMEOUT = 30 * 1000;

    // ���� ZooKeeper ʵ��
    private ZooKeeper zk;

    // ���� Watcher ʵ��
    private Watcher wh = new Watcher() {
        /**
         * Watched�¼�
         */
        public void process(WatchedEvent event) {
            System.out.println("WatchedEvent >>> " + event.toString());
        }
    };

    // ��ʼ�� ZooKeeper ʵ��
    private void createZKInstance() throws IOException {
        // ���ӵ�ZK���񣬶�������ö��ŷָ�д
        zk = new ZooKeeper("localhost:2181,localhost:2182,localhost:2183,localhost:2184", Test.SESSION_TIMEOUT, this.wh);

    }

    private void ZKOperations() throws IOException, InterruptedException, KeeperException {
        //Ȩ�ޣ� OPEN_ACL_UNSAFE ���ڵ����ͣ� Persistent
        zk.create("/test", "lhc0512".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        //�鿴
        System.out.println(new String(zk.getData("/test", this.wh, null)));//lhc0512

        // �޸�
        zk.setData("/test", "llhhcc".getBytes(), -1);//WatchedEvent >>> WatchedEvent state:SyncConnected type:NodeDataChanged path:/test

        // �����ٴν����޸ģ��򲻻ᴥ��Watch�¼��������������֤ZK��һ�����ԡ�һ���Դ�����
        zk.setData("/test", "llhhcc0512".getBytes(), -1);

        //�鿴
        System.out.println(new String(zk.getData("/test", false, null)));//llhhcc0512

        //ɾ��
        zk.delete("/test", -1);

        //�鿴�Ƿ�ɾ��
        System.out.println(" �ڵ�״̬�� [" + zk.exists("/test", false) + "]");// �ڵ�״̬�� [null]
    }

    //�ر�
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