package com.teng.system.io.reconsitution.rpc.transport;


import com.teng.system.io.reconsitution.rpc.util.PackageMsg;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


class ResponseMappingCallback {
  static   ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<>();

    public static void  addCallBack(long requestId, CompletableFuture cb) {
        mapping.putIfAbsent(requestId, cb);
    }

    public static void runCallBack(PackageMsg msg) {
        CompletableFuture cf = mapping.get(msg.getHeader().getRequestId());
        cf.complete(msg.getContent().getRes());
        removeCB(msg.getHeader().getRequestId());

    }

    private static void removeCB(long requestId) {
        mapping.remove(requestId);
    }

}





