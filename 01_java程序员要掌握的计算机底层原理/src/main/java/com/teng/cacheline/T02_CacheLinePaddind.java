package com.teng.cacheline;

/**
 * @program: 81plan
 * @description: 缓存行测试-02
 * @author: Mr.Teng
 * @create: 2021-01-14 08:50
 **/
public class T02_CacheLinePaddind {

    public static volatile long[] arr = new long[16];

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (long i = 0; i < 1000_000_000L; i++) {
                arr[0] = i;
            }
        });
        /**
         * 保证两个变量不公用一个缓存行，发生伪共享
         */
        Thread t2 = new Thread(() -> {
            for (long i = 0; i < 1000_000_000L; i++) {
                arr[8] = i;
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
