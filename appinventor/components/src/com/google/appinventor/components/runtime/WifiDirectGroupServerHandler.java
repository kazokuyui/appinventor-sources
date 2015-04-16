package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.PeerMessage;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

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
        PeerMessage msg = new PeerMessage(PeerMessage.CONTROL_DATA, PeerMessage.CTRL_CONNECTED);
        final ChannelFuture f = context.channel().writeAndFlush(msg.toString());
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                WifiDirectGroupServerHandler.this.server.peerConnected(context.channel().remoteAddress().toString());
            }
        });
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
        cause.printStackTrace();
        ctx.close();
    }
}
