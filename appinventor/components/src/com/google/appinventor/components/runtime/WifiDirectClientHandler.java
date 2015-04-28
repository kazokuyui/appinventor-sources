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
    private WifiDirectControlClient client;

    public WifiDirectClientHandler(WifiDirectControlClient client) {
        super();
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        PeerMessage response = this.parseResponse((String) msg);
        if(response.getType() == PeerMessage.CONTROL_DATA) {
            if(response.getData().equals(PeerMessage.CTRL_CONNECTED)) {
                this.client.peerConnected(response.getHeader());
            }
            else if(response.getData().equals(PeerMessage.CTRL_REGISTERED)) {
                this.client.peerRegistered(new WifiDirectPeer("wow", "wow", "wow", 4545));
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
        ctx.close();
    }

    public PeerMessage parseResponse(String response) {
        String[] separated = response.split("/");
        return new PeerMessage(Integer.parseInt(separated[0]), separated[1], separated[2]);
    }
}
