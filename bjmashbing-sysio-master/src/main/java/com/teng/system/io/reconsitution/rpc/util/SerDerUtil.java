package com.teng.system.io.reconsitution.rpc.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @program: 81plan
 * @description: 1.先假设一个需求，写一个PPC
 *               2.来回通信，连接数量，拆包？
 *               3.动态代理， 序列化，协议封装
 *               4.连接池
 *               5.就像调用本地方法一样，调用远程方法，
 *            面向java中就是所谓的面向接口编程
 * @author: Mr.Teng
 * @create: 2021-01-23 14:44
 **/


class SerDerUtil {
    static ByteArrayOutputStream out = new ByteArrayOutputStream();

    public synchronized static byte[] ser(Object msg) {
        out.reset();
        ObjectOutputStream oout = null;
        byte[] msgBody=null;
        try {
            oout = new ObjectOutputStream(out);
            oout.writeObject(msg);
            msgBody = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
//
        return msgBody;
    }
}