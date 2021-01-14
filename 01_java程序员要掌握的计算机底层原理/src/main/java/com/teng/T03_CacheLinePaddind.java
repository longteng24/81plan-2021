package com.teng;

import sun.misc.Contended;

/**
 * @program: 81plan
 * @description: 缓存行测试-03  jdk8 @Contended 注解  jvm参数： -XX:-RestrictContended
 * @author: Mr.Teng
 * @create: 2021-01-14 08:50
 **/
public class T03_CacheLinePaddind {
    @Contended
     volatile long a ;
    @Contended
     volatile long b ;

    public static void main(String[] args) throws InterruptedException {
        T03_CacheLinePaddind t03 = new T03_CacheLinePaddind();
        Thread t1 = new Thread(() -> {
            for (long i = 0; i < 1000_000_000L; i++) {
                t03.a = i;
            }
        });
        /**
         * 保证两个变量不公用一个缓存行，发生伪共享
         */
        Thread t2 = new Thread(() -> {
            for (long i = 0; i < 1000_000_000L; i++) {
                t03.b  = i;
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
