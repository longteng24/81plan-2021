package com.teng.system.io.reconsitution.service;

public class MyFly implements  Fly{
    @Override
    public void xxoo(String msg) {
        System.out.println("server  xxoo  ,get client arg :"+ msg);
//        return "server res :" + msg;
    }
}