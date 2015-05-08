package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.PeerMessage;
import io.netty.channel.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectControlClientHandler extends ChannelInboundHandlerAdapter {
    private WifiDirectControlClient client;

    public WifiDirectControlClientHandler(WifiDirectControlClient client) {
        super();
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        PeerMessage response = this.parseResponse((String) msg);
        if(response.getType() == PeerMessage.CONTROL_DATA) {
            if(response.getHeader().equals(PeerMessage.CTRL_CONNECTED)) {
                this.client.peerConnected(response.getData());
                PeerMessage reply = new PeerMessage(PeerMessage.CONTROL_DATA,
                                                    PeerMessage.CTRL_REGISTER,
                                                    this.client.getmPeer().toString());
                ctx.channel().writeAndFlush(reply.toString());
            }
            else if(response.getHeader().equals(PeerMessage.CTRL_REGISTERED)) {
                this.client.peerRegistered(Integer.parseInt(response.getData()));
            }
            else if(response.getHeader().equals(PeerMessage.CTRL_PEERS_LIST)) {
                this.client.peersAvailable(this.getPeersList(response.getData()));
            }
            else if(response.getHeader().equals(PeerMessage.CTRL_PEERS_CHANGE)) {
                this.client.peersChanged();
            }
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

    public void requestPeers(Channel serverChannel) {
        PeerMessage msg = new PeerMessage(PeerMessage.CONTROL_DATA,
                                          PeerMessage.CTRL_REQUEST_PEER,
                                          Integer.toString(this.client.getmPeer().getId()));
        serverChannel.writeAndFlush(msg.toString());
    }

    public void quit(Channel serverChannel) {
        PeerMessage msg = new PeerMessage(PeerMessage.CONTROL_DATA,
                                          PeerMessage.CTRL_QUIT,
                                          Integer.toString(this.client.getmPeer().getId()));
        serverChannel.writeAndFlush(msg.toString());
    }

    public PeerMessage parseResponse(String response) {
        String[] separated = response.split("/");
        return new PeerMessage(Integer.parseInt(separated[0]), separated[1], separated[2]);
    }

    public Collection<WifiDirectPeer> getPeersList(String rawList) {
        Collection<WifiDirectPeer> peersList = new ArrayList<WifiDirectPeer>();
        String[] rawPeers = rawList.split(",");

        for(String peer : rawPeers) {
            WifiDirectPeer newPeer = new WifiDirectPeer(peer);
            if(newPeer.getId() != this.client.getmPeer().getId()) {
                peersList.add(newPeer);
            }
        }

        return peersList;
    }
}
