package com.teng.system.io.reconsitution.rpc.transport;


import com.teng.system.io.reconsitution.rpc.util.PackageMsg;
import com.teng.system.io.reconsitution.rpc.protocol.MyContent;
import com.teng.system.io.reconsitution.rpc.protocol.MyHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;


public class ServerDecode extends ByteToMessageDecoder{

    //父类里一定要 channelread { 前老的拼buf }->byteBuf  decode():剩余留存 ；对out遍历
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

   //     System.out.println("channel start :"+buf.readableBytes());
        while (buf.readableBytes() >= 124) {
            byte[] bytes = new byte[124];
            buf.getBytes(buf.readerIndex(),bytes); //从哪读，不会移动指针
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            MyHeader header= (MyHeader)oin.readObject();
         //   System.out.println("server   dataLen"+header.dataLen);
         //   System.out.println(" server    getRequestId"+header.getRequestId());

      //Decode在两个方向都使用
            //通信协议
            if (buf.readableBytes()-124 >= header.getDataLen()) {
                //处理指针  移动指针到body开始位位置   保证buf读 整个头和体
                buf.readBytes(124);

                byte[] data = new byte[(int)header.getDataLen()];
                buf.readBytes(data);
                ByteArrayInputStream din = new ByteArrayInputStream(data);
                ObjectInputStream doin = new ObjectInputStream(din);


                if(header.getFlag()==0x14141414){
                    MyContent content= (MyContent)doin.readObject();

                    out.add(new PackageMsg(header, content));
                }else if(header.getFlag()==0x14141424){
                    MyContent   content= (MyContent)doin.readObject();

                    out.add(new PackageMsg(header, content));
                }


            }else{
               break;
            }

        }


    }
}





