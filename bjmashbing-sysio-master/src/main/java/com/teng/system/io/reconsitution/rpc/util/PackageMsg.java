package com.teng.system.io.reconsitution.rpc.util;


import com.teng.system.io.reconsitution.rpc.protocol.MyContent;
import com.teng.system.io.reconsitution.rpc.protocol.MyHeader;

public  class PackageMsg {

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

