package com.teng.system.io.testreactor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @program: 81plan
 * @description: 客户端
 * @author: Mr.Teng
 * @create: 2021-01-21 22:38
 **/
public class SocketClient extends Thread{
    public static Socket client=null;
    public static BufferedReader input =null;
    public static BufferedReader br =null;
    static String str="";

    public static void main(String[] args) throws Exception{

        //目标服务器的 IP地址 和 端口号
        client = new Socket(InetAddress.getByName("localhost"),6666);

        //读取返回 数据 指定  其输入缓冲的空间地址 中数据   读取缓存  通过 控制台 输入（由客户端 人工输入）
        br = new BufferedReader(new InputStreamReader(System.in));

        //读取返回 数据 指定  其输入缓冲的空间地址 中数据   读取缓存  通过 用户接收信息的缓存 输入（由服务器输入）
        input = new BufferedReader(new InputStreamReader(client.getInputStream()));

        new SocketClient().start();
        //不知道为什么取消此段代码  程序就运行的不正常？
        // 答： 该进程  负责  发送数据
        //     该进程未对 str 赋值 则会通过 A  处判断（ if(str.equals(""))）；来确认进程  负责  的类型

        Thread.sleep(1);
        //     以下进程  负责 接收数据
        str="ss";

        new SocketClient().start();

    }

    public void run() {
        try {
            if(str.equals("")){//  ：A
                String line="";
                PrintWriter out = new PrintWriter(client.getOutputStream());

                while((line=br.readLine())!=null){//不停的检测 控制台输入的信息 将信息 发送
                    out.println(line);
                    out.flush(); //不清空缓存也会出现运行不正常，不知道为什么？？？？？？？？？？？？？？？？？？？
                    if(line.equals("bye"))
                    {
                        break;
                    }
                    if(line.equals("exit"))
                    {
                        System.out.println("out line  close");
                        if (client != null) {
                            client.close();
                            System.out.println(Thread.currentThread().getName()+"  client in exit");
                        }
                        break;
                    }
                }
            }   else {
                String inputMessage = "";
                while ((inputMessage = input.readLine()) != null)//不停读取  服务器返回的消息 然后将数据打印在控制台
                {
                    System.out.println(inputMessage);
                    if(inputMessage.equals("exit"))
                    {
                        System.out.println("in line  close");
                        if (client != null) {
                            client.close();
                            System.out.println(Thread.currentThread().getName()+"  client out exit");
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
