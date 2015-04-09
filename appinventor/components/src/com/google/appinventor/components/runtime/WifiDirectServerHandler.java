package com.google.appinventor.components.runtime;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Java NIO Server Handler for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

@ChannelHandler.Sharable
public class WifiDirectServerHandler extends ChannelInboundHandlerAdapter {

    private WifiDirectServer server;

    public WifiDirectServerHandler(WifiDirectServer server) {
        super();
        this.server = server;
    }

    @Override
    public void channelActive(final ChannelHandlerContext context) {
        this.server.accept(context.channel().remoteAddress().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
