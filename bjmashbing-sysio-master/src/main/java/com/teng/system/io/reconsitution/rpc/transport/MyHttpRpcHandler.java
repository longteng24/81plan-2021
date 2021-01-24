package com.teng.system.io.reconsitution.rpc.transport;

import com.teng.system.io.reconsitution.rpc.Dispatcher;
import com.teng.system.io.reconsitution.rpc.protocol.MyContent;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public  class MyHttpRpcHandler extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            ServletInputStream in = req.getInputStream();
            ObjectInputStream oin = new ObjectInputStream(in);
            try {
                MyContent myContent = (MyContent)oin.readObject();

                String name = myContent.getName();
                String method = myContent.getMethodName();
                Object c = Dispatcher.getDis().get(name);
                Class<?> clazz = c.getClass();
                Object res = null ;
                Method m = null;
                try {
                    m = clazz.getMethod(method,myContent.getParameterTypes());
                    res = m.invoke(c, myContent.getArgs());
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                String execThreadName = Thread.currentThread().getName();
//                String s = "io thread : " + ioThreadName + ",exec thread :"
//                        + execThreadName + " from args :" + requestPkg.content.getArgs()[0];
                //     System.out.println("s :"+s);
                MyContent resContent = new MyContent();

                resContent.setRes(res);

//                byte[] contentByte = SerDerUtil.ser(resContent);
//
                ServletOutputStream out = resp.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.writeObject(resContent);
//                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentByte.length);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


        }
    }
