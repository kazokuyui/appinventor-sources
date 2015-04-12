package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.WifiDirectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectClientHandler extends SimpleChannelInboundHandler<String> {
    private WifiDirectClient client;

    /**
     * Creates a client-side handler.
     */
    public WifiDirectClientHandler(WifiDirectClient client) {
        super();
        this.client = client;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        this.client.peerConnected();
        this.client.trigger("MESSAGE HELLO");
        this.client.accept("FUCK YOU CLIENT");
        ByteBuf msg = Unpooled.copiedBuffer("Welcome Client", CharsetUtil.UTF_8);
        ctx.writeAndFlush(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf m = (ByteBuf) msg;
        this.client.peerConnected();
        this.client.trigger("MESSAGE HELLO");
        this.client.accept("FUCK YOU CLIENT");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        this.client.peerConnected();
        this.client.trigger("MESSAGE HELLO");
        this.client.accept("FUCK YOU CLIENT");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        this.client.peerConnected();
        this.client.trigger("MESSAGE HELLO");
        this.client.accept("FUCK YOU CLIENT");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.client.peerConnected();
        this.client.trigger("MESSAGE HELLO");
        this.client.accept("FUCK YOU CLIENT");

        this.client.trigger(cause.toString());
        ctx.close();
    }

}
