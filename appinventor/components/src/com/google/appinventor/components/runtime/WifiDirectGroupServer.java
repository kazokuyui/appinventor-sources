package com.google.appinventor.components.runtime;

import android.os.Handler;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;
import com.google.gson.Gson;
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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
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
    private InetAddress hostAddress;
    private int port;

    private Collection<WifiDirectPeer> activePeers;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Handler handler;

    public WifiDirectGroupServer(WifiDirectP2P p2p, InetAddress hostAddress, int port) throws IOException {
        this.p2p = p2p;
        this.hostAddress = hostAddress;
        this.port = port;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
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
                    p.addLast(new WifiDirectGroupServerHandler(WifiDirectGroupServer.this));
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

    public void peerConnected(String peer) {
        this.p2p.ConnectionAccepted(peer);
    }

    public void peerRegistered(WifiDirectPeer peer) {
        this.p2p.ConnectionRegistered(peer.toString());
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

    /* For testing purposes */
    public void trigger(String msg) {
        this.p2p.Trigger(msg);
    }
}
