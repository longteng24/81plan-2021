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
      //  SelectorThreadGroup stg = new SelectorThreadGroup(1);

        //混杂模式，只有一个线程负责accept,每个都会被分配client,进行r/w
        SelectorThreadGroup stg = new SelectorThreadGroup(3);

        //2.把监听9999 的server 注册到某一个selector 上
        stg.bind(9999);
    }
}
