package com.teng.cacheline;

/**
 * @program: 81plan
 * @description: 缓存行测试-01  反例
 * @author: Mr.Teng
 * @create: 2021-01-14 08:50
 **/
public class T01_CacheLinePaddind {

    public static volatile long[] arr = new long[2];

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            // 数字加下换线，方便阅读
            for (long i = 0; i < 1000_000_000L; i++) {
                arr[0] = i;
            }
        });

        Thread t2 = new Thread(() -> {
            for (long i = 0; i < 1000_000_000L; i++) {
                arr[1] = i;
            }
        });

        final long start = System.nanoTime();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("time:"+(System.nanoTime() -start)/100_0000);
    }


}
