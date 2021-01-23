package com.teng.system.io.netty;

import java.util.concurrent.*;

/**
 * @program: 81plan
 * @description: 异步执行测试
 * @author: Mr.Teng
 * @create: 2021-01-23 21:04
 **/
public class CompletableFutureTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                System.out.println("executorService 是否为守护线程 :" + Thread.currentThread().isDaemon());
                return null;
            }
        });
        //默认调用的是守护线程，主线程退出，守护线程自动退出，入下方不用get 阻塞，当前方法执行中退出
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("this is lambda supplyAsync");
            System.out.println("supplyAsync 是否为守护线程 " + Thread.currentThread().isDaemon());
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("this lambda is executed by forkJoinPool");
            return "result1";
        });
        //用指定的线程池中 线程来执行
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("this is task with executor");
            System.out.println("supplyAsync 使用executorService 时是否为守护线程 : " + Thread.currentThread().isDaemon());
            return "result2";
        }, executorService);
        System.out.println(completableFuture.get());
        System.out.println(future.get());
        executorService.shutdown();


        System.out.println("======================");
        otherStaticMethod();
    }

    public static void otherStaticMethod() throws ExecutionException, InterruptedException {
        final CompletableFuture<String> futureOne = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("futureOne InterruptedException");
            }
            return "futureOneResult";
        });
        final CompletableFuture<String> futureTwo = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("futureTwo InterruptedException");
            }
            return "futureTwoResult";
        });
        //这两个方法的入参是一个completableFuture组、allOf就是所有任务都完成时返回。但是是个Void的返回值。
        CompletableFuture future = CompletableFuture.allOf(futureOne, futureTwo);
        System.out.println(future.get());
        //anyOf是当入参的completableFuture组中有一个任务执行完毕就返回。返回结果是第一个完成的任务的结果。
        CompletableFuture completableFuture = CompletableFuture.anyOf(futureOne, futureTwo);
        System.out.println(completableFuture.get());
    }
}
