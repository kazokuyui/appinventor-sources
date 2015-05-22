package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.PeerMessage;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.SocketAddress;
import java.util.HashMap;

/**
 * Java NIO Gateway Handler for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class WifiDirectGatewayHandler extends ChannelInboundHandlerAdapter {
    static final ChannelGroup newChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private WifiDirectGateway gateway;
    private HashMap<String, WifiDirectPeer> peerChannels;

    public WifiDirectGatewayHandler(WifiDirectGateway gateway) {
        super();
        this.gateway = gateway;
        this.gateway.setGatewayHandler(this);
        this.peerChannels = new HashMap<String, WifiDirectPeer>();
    }

    @Override
    public void channelActive(final ChannelHandlerContext context) {
        final String clientIP = this.parseIp(context.channel().remoteAddress());
        PeerMessage msg = new PeerMessage(PeerMessage.CONTROL_DATA,
                                          PeerMessage.CTRL_GATEWAY_CONNECTED,
                                          clientIP);
        context.channel().writeAndFlush(msg.toString());
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, Object msg) {
        PeerMessage response = this.parseResponse((String) msg);
        if(response.getType() == PeerMessage.CONTROL_DATA) {
            if(response.getHeader().equals(PeerMessage.CTRL_REGISTER)) {
                final WifiDirectPeer peer = new WifiDirectPeer(response.getData());
                peer.setChannelId(context.channel().id());
                this.gateway.registerPeer(peer);

                PeerMessage reply = new PeerMessage(PeerMessage.CONTROL_DATA,
                                                    PeerMessage.CTRL_REGISTERED,
                                                    Integer.toString(peer.getId()));

                ChannelFuture f = context.channel().writeAndFlush(reply.toString());
                f.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        newChannels.add(context.channel());
                        WifiDirectGatewayHandler.this.peerChannels.put(context.channel().id().asLongText(), peer);
                        WifiDirectGatewayHandler.this.gateway.peersChanged();
                    }
                });
            }
            else if(response.getHeader().equals(PeerMessage.CTRL_REQUEST_PEER)) {
                PeerMessage reply = new PeerMessage(PeerMessage.CONTROL_DATA,
                                                    PeerMessage.CTRL_PEERS_LIST,
                                                    this.gateway.getPeersList());
                context.channel().writeAndFlush(reply.toString());
            }
        }
        else if(response.getType() == PeerMessage.USER_DATA) {
            if(response.getHeader().equals(PeerMessage.USR_MESSAGE)) {
                WifiDirectPeer sender = peerChannels.get(context.channel().id().asLongText());
                this.gateway.addPeerMessage(parseUserMessage(sender.getName(), response.getData()));
                this.gateway.trigger("GATEWAY USR: " + this.gateway.getQueuedMessages());
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        this.gateway.errorOccurred("Gateway Handler", ErrorMessages.ERROR_WIFIDIRECT_SERVER_FAILED, cause.getMessage());
        context.channel().close();
    }

    public void broadcastMessage(PeerMessage msg) {
        newChannels.writeAndFlush(msg.toString());
    }

    public PeerMessage parseUserMessage(String sender, String data) {
        return new PeerMessage(PeerMessage.USER_DATA, sender, data);
    }

    public String parseIp(SocketAddress socketAddress) {
        return socketAddress.toString().substring(1).split(":")[0].trim();
    }

    public PeerMessage parseResponse(String response) {
        String[] separated = response.split("/");
        return new PeerMessage(Integer.parseInt(separated[0]), separated[1], separated[2]);
    }
}
