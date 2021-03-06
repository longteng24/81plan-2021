package com.teng.system.io.netty;

/**
 * @program: 81plan
 * @description:
 * @author: Mr.Teng
 * @create: 2021-01-23 19:28
 **/
public class PackageMsg {

    MyHeader header;
    MyContent content;

    public PackageMsg(MyHeader header, MyContent content) {
        this.content = content;
        this.header = header;
    }

    public MyHeader getHeader() {
        return header;
    }

    public void setHeader(MyHeader header) {
        this.header = header;
    }

    public MyContent getContent() {
        return content;
    }

    public void setContent(MyContent content) {
        this.content = content;
    }
}
