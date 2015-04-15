package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.WifiDirectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;

/**
 * Java NIO Server Handler for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectGroupServerHandler extends ChannelInboundHandlerAdapter {

    private WifiDirectGroupServer server;

    public WifiDirectGroupServerHandler(WifiDirectGroupServer server) {
        super();
        this.server = server;
    }

    @Override
    public void channelActive(final ChannelHandlerContext context) {
        //ByteBuf msg = context.alloc().buffer(4);
        //msg.writeInt(WifiDirectUtil.PEER_CONNECTED);
        String msg = "PEER_CONNECTED" + "\r\n";
        final ChannelFuture f = context.channel().writeAndFlush(msg);
        //context.channel().flush();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                WifiDirectGroupServerHandler.this.server.peerConnected(context.channel().remoteAddress().toString() + "01");
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        // for disconnection
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) {
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
