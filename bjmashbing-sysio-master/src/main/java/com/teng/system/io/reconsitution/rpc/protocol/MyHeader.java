package com.teng.system.io.reconsitution.rpc.protocol;

import java.io.Serializable;
import java.util.UUID;

public class MyHeader implements Serializable {
    //通信上的协议
    /**
     * 1.ooxx值
     * 2.UUID
     * 3.DATA_LIN
     */
    int flag; // 32bit可以设置很多信息
    long requestId;
    long dataLen;


    public static MyHeader createHeader(byte[] msg) {
        MyHeader header = new MyHeader();

        int size = msg.length;
        int f=0x14141414; // 32位规则  14八进制
        long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        //0x14 0001 0100
        header.setFlag(f);
        header.setDataLen(size);
        header.setRequestId(requestId);
        return header;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getDataLen() {
        return dataLen;
    }

    public void setDataLen(long dataLen) {
        this.dataLen = dataLen;
    }
}