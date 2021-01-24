package com.teng.system.io.reconsitution.rpc.transport;


import com.teng.system.io.reconsitution.rpc.util.PackageMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


public class ClientResponses extends ChannelInboundHandlerAdapter {


    //consumer...
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PackageMsg responseMsg = (PackageMsg) msg;

        ResponseMappingCallback.runCallBack(responseMsg);
    }
}





