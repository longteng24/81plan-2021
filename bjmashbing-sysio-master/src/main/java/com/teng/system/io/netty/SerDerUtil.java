package com.teng.system.io.netty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @program: 81plan
 * @description: 公共序列化
 * @author: Mr.Teng
 * @create: 2021-01-23 20:02
 **/
public class SerDerUtil {
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
