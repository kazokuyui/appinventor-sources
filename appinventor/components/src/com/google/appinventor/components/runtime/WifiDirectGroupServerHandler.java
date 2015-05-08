package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.PeerMessage;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.SocketAddress;
import java.util.HashMap;

/**
 * Java NIO Server Handler for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectGroupServerHandler extends ChannelInboundHandlerAdapter {
    private WifiDirectGroupServer server;
    private ChannelGroup channels;
    private HashMap<String, WifiDirectPeer> peerChannels;

    public WifiDirectGroupServerHandler(WifiDirectGroupServer server) {
        super();
        this.server = server;
        this.server.setServerHandler(this);
        this.channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.peerChannels = new HashMap<String, WifiDirectPeer>();
    }

    @Override
    public void channelActive(final ChannelHandlerContext context) {
        final String clientIP = this.parseIp(context.channel().remoteAddress());
        PeerMessage msg = new PeerMessage(PeerMessage.CONTROL_DATA, PeerMessage.CTRL_CONNECTED, clientIP);
        final ChannelFuture f = context.channel().writeAndFlush(msg.toString());
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                WifiDirectGroupServerHandler.this.server.peerConnected(clientIP);
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, Object msg) {
        PeerMessage response = this.parseResponse((String) msg);
        if(response.getType() == PeerMessage.CONTROL_DATA) {
            if(response.getHeader().equals(PeerMessage.CTRL_REGISTER)) {
                final WifiDirectPeer peer = new WifiDirectPeer(response.getData());
                this.server.registerPeer(peer);
                PeerMessage reply = new PeerMessage(PeerMessage.CONTROL_DATA,
                                                    PeerMessage.CTRL_REGISTERED,
                                                    Integer.toString(peer.getId()));
                ChannelFuture f = context.channel().writeAndFlush(reply.toString());
                f.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        WifiDirectGroupServerHandler.this.channels.add(context.channel());
                        WifiDirectGroupServerHandler.this.peerChannels.put(context.channel().id().asLongText(), peer);
                        WifiDirectGroupServerHandler.this.server.peersChanged();
                    }
                });
            }
            else if(response.getHeader().equals(PeerMessage.CTRL_REQUEST_PEER)) {
                PeerMessage reply = new PeerMessage(PeerMessage.CONTROL_DATA,
                                                    PeerMessage.CTRL_PEERS_LIST,
                                                    this.server.getPeersList());
                context.channel().writeAndFlush(reply.toString());
            }
            else if(response.getHeader().equals(PeerMessage.CTRL_QUIT)) {
                this.server.removePeerById(Integer.valueOf(response.getData()));
                context.close();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        this.server.trigger(cause.toString());
        this.closeChannel(context.channel());
    }

    public void broadcastMessage(PeerMessage msg) {
        this.channels.writeAndFlush(msg.toString());
    }

    public void closeChannel(Channel channel) {
        String channelId = channel.id().asLongText();
        WifiDirectPeer peer = this.peerChannels.get(channelId);
        this.server.removePeerById(peer.getId());
        ChannelFuture f = channel.close();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                WifiDirectGroupServerHandler.this.server.peersChanged();
            }
        });
    }

    public String parseIp(SocketAddress socketAddress) {
        return socketAddress.toString().substring(1).split(":")[0].trim();
    }

    public PeerMessage parseResponse(String response) {
        String[] separated = response.split("/");
        return new PeerMessage(Integer.parseInt(separated[0]), separated[1], separated[2]);
    }

    public ChannelGroup getChannels() {
        return this.channels;
    }
}
