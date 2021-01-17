package com.teng.fiber;


import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;

/**
 * 纤程测试对比 -用 fiber实现
 * \u200b （(Zero width space) characters）
 *
 */
public class HelloFiber2 {

    public static void main(String[] args) throws  Exception {
        long start = System.currentTimeMillis();


        int size = 10000;

        Fiber<Void>[] fibers = new Fiber[size];

        for (int i = 0; i < fibers.length; i++) {
            fibers[i] = new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    calc();
                }
            });
        }

        for (int i = 0; i < fibers.length; i++) {
            fibers[i].start();
        }

        for (int i = 0; i < fibers.length; i++) {
            fibers[i].join();
        }

        long end = System.currentTimeMillis();
        System.out.println(end - start);


    }

    static void calc() {
        int result = 0;
        for (int m = 0; m < 10000; m++) {
            for (int i = 0; i < 200; i++) {
                result += i;
            }

        }
    }
}