package com.google.appinventor.components.runtime;

import android.net.wifi.p2p.WifiP2pDevice;
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
 * Java NIO Server for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectGroupServer implements Runnable {
    private WifiDirectP2P p2p;
    private Handler handler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    WifiDirectGroupServerHandler serverHandler;

    private InetAddress hostAddress;
    private int port;

    private Collection<WifiDirectPeer> activePeers;

    public WifiDirectGroupServer(WifiDirectP2P p2p, InetAddress hostAddress, int port) throws IOException {
        this.p2p = p2p;
        this.hostAddress = hostAddress;
        this.port = port;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.serverHandler = new WifiDirectGroupServerHandler(this);
        this.activePeers = new ArrayList<WifiDirectPeer>();
    }

    public void run() {
        SslContext sslCtx = this.initiateSsl();

        try {
            ServerBootstrap b = new ServerBootstrap();
            final SslContext finalSslCtx = sslCtx;
            b.group(bossGroup, workerGroup);
            b.option(ChannelOption.SO_KEEPALIVE, true);
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
                    p.addLast(WifiDirectGroupServer.this.serverHandler);
                }
            });

            ChannelFuture f = b.bind(this.hostAddress, this.port).sync();
            this.serverStarted();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
        }
    }

    public void stop() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

    public void registerPeer(WifiDirectPeer peer) {
        peer.setId(this.activePeers.size() + 1);
        this.activePeers.add(peer);
        this.peerRegistered(peer);
//        if(peer.getId() > 1) {
//            this.peersChanged();
//        }
    }

    /* Server Events */
    public void serverStarted() {
        final String ipAddress = this.hostAddress.getHostAddress();
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectGroupServer.this.p2p.GOServerStarted(ipAddress);
            }
        });
    }

    public void peerConnected(final String peer) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectGroupServer.this.p2p.ConnectionAccepted(peer);
            }
        });
    }

    public void peerRegistered(final WifiDirectPeer peer) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectGroupServer.this.p2p.ConnectionRegistered(peer.getName());
            }
        });
    }

    public void peersChanged() {
        PeerMessage msg = new PeerMessage(PeerMessage.CONTROL_DATA, PeerMessage.CTRL_PEERS_CHANGE, " ");
        this.serverHandler.broadcastMessage(msg);
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectGroupServer.this.p2p.PeersChanged();
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

    public String getPeersList() {
        String peerList = "";
        if(!this.activePeers.isEmpty()) {
            for (WifiDirectPeer peer : this.activePeers) {
                peerList += peer+",";
            }
        }
        else {
            peerList = "NIL";
        }
        return peerList;
    }

    /* For testing purposes */
    public void trigger(String msg) {
        this.p2p.Trigger(msg);
    }
}
