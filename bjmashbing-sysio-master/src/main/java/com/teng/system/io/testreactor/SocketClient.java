package com.teng.system.io.testreactor;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * @program: 81plan
 * @description: 客户端
 * @author: Mr.Teng
 * @create: 2021-01-21 22:38
 **/
public class SocketClient {
    public static void main(String[] args) {
        try {
            Socket socket=new Socket("129.0.0.1", 9999);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
