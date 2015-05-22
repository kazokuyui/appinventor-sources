package com.google.appinventor.components.runtime;

import android.os.Handler;
import com.google.appinventor.components.runtime.util.PeerMessage;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Java NIO Gateway for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class WifiDirectGateway implements Runnable {
    private WifiDirectP2P p2p;
    private Handler handler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private WifiDirectGatewayHandler gatewayHandler;

    private InetAddress hostAddress;
    private int port;

    private Collection<WifiDirectPeer> peers;
    private Collection<PeerMessage> queuedMessages;
    public boolean isAccepting;

    public WifiDirectGateway(WifiDirectP2P p2p, InetAddress hostAddress, int port) throws IOException {
        this.p2p = p2p;
        this.hostAddress = hostAddress;
        this.port = port;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.peers = new ArrayList<WifiDirectPeer>();
        this.queuedMessages = new ArrayList<PeerMessage>();
        this.isAccepting = false;
    }

    public void run() {
        SslContext sslCtx = this.initiateSsl();

        try {
            ServerBootstrap b = new ServerBootstrap();
            final SslContext finalSslCtx = sslCtx;
            b.group(bossGroup, workerGroup);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.SO_REUSEADDR, true);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();

                    if (finalSslCtx != null) {
                        p.addLast(finalSslCtx.newHandler(ch.alloc()));
                    }

                    p.addLast(new DelimiterBasedFrameDecoder(WifiDirectUtil.controlBufferSize,
                                                             Delimiters.lineDelimiter()));
                    p.addLast(new StringEncoder());
                    p.addLast(new StringDecoder());
                    p.addLast(new WifiDirectGatewayHandler(WifiDirectGateway.this));
                }
            });
            ChannelFuture f = b.bind(this.hostAddress, this.port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
        }
    }

    public void registerPeer(WifiDirectPeer peer) {
        peer.setId(this.peers.size() + 1);
        peer.setStatus(WifiDirectPeer.PEER_STATUS_ACTIVE);
        this.peers.add(peer);
    }

    public void addPeerMessage(PeerMessage message) {
        this.queuedMessages.add(message);
    }

    /* Gateway Events */
    public void peersChanged() {
        PeerMessage msg = new PeerMessage(PeerMessage.CONTROL_DATA, PeerMessage.CTRL_PEERS_CHANGE, " ");
        this.gatewayHandler.broadcastMessage(msg);
    }

    public void errorOccurred(final String functionName, final int errorCode, final String cause) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectGateway.this.p2p.wifiDirectError(functionName, errorCode, cause);
            }
        });
    }

    /* Setters and Getters */
    public SslContext initiateSsl() {
        if (WifiDirectUtil.SSL) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                return SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setGatewayHandler(WifiDirectGatewayHandler gatewayHandler) {
        this.gatewayHandler = gatewayHandler;
    }

    public void setPeers(Collection<WifiDirectPeer> peers) {
        this.peers = peers;
    }

    public Collection<WifiDirectPeer> getPeers() {
        return this.peers;
    }

    public String getPeersList() {
        String peerList = "";
        if(!this.peers.isEmpty()) {
            for (WifiDirectPeer peer : this.peers) {
                peerList += peer+",";
            }
        }
        else {
            peerList = "NIL";
        }
        return peerList;
    }

    public Collection<PeerMessage> getQueuedMessages() {
        return this.queuedMessages;
    }

    public String getQueuedMessagesList() {
        String messages = "";
        if(!this.queuedMessages.isEmpty()) {
            for (PeerMessage msg : this.queuedMessages) {
                messages += msg+",";
            }
        }
        else {
            messages = "NIL";
        }
        return messages;
    }

    /* For testing purposes */
    public void trigger(String msg) {
        this.p2p.Trigger(msg);
    }
}
