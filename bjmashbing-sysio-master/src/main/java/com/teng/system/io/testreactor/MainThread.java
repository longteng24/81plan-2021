package com.teng.system.io.testreactor;

/**
 * @program: 81plan
 * @description: 主程序
 * @author: Mr.Teng
 * @create: 2021-01-21 21:22
 **/
public class MainThread {

    public static void main(String[] args) {
        //这里不做关于IO 和 业务的事情


        //1.创建IO thread 一个或多个
        SelectorThreadGroup boss = new SelectorThreadGroup(3);   //boss线程组
        SelectorThreadGroup worker = new SelectorThreadGroup(3);  //worker线程组

        //混杂模式，只有一个线程负责accept,每个都会被分配client,进行r/w
//        SelectorThreadGroup stg = new SelectorThreadGroup(3);

        //2.把监听9999 的server 注册到某一个selector 上

        boss.setWorker(worker);

        /**
         * boss 里面选了一个线程注册 listen, 触发bind
         * 从而这个选中的线程得持有 workerGroup 的引用
         *
         * 因为未来listen 一旦accept 得到client 后得去worker中 next出下一个线程分配
         */
        boss.bind(9999);
        boss.bind(8888);
        boss.bind(7777);
        boss.bind(6666);
    }
}
