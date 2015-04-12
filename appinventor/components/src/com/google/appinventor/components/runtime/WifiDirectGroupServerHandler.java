package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.WifiDirectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

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
        this.server.accept(context.channel().remoteAddress().toString());
        ByteBuf msg = Unpooled.copiedBuffer("Welcome Client", CharsetUtil.UTF_8);

        final ChannelFuture f = context.writeAndFlush(msg);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                WifiDirectGroupServerHandler.this.server.accept("WRITING: DONE!");
            }
        });
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
        this.server.trigger(cause.toString());
        ctx.close();
    }
}
