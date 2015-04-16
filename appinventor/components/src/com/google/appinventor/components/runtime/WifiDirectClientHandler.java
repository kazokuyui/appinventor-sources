package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.PeerMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectClientHandler extends ChannelInboundHandlerAdapter {
    private WifiDirectClient client;

    /**
     * Creates a client-side handler.
     */
    public WifiDirectClientHandler(WifiDirectClient client) {
        super();
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        PeerMessage response = this.parseResponse((String) msg);
        if(response.getType() == PeerMessage.CONTROL_DATA) {
            if(response.getData().equals(PeerMessage.CTRL_CONNECTED)) {
                this.client.peerConnected();
            }
            else if(response.getData().equals(PeerMessage.CTRL_REGISTERED)) {
                this.client.peerRegistered();
            }
        }
        else if(response.getType() == PeerMessage.PEER_DATA) {
            //handle peer data
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.client.trigger(cause.toString());
        cause.printStackTrace();
        ctx.close();
    }

    public PeerMessage parseResponse(String response) {
        return new PeerMessage(PeerMessage.CONTROL_DATA, PeerMessage.CTRL_REGISTERED);
    }
}
