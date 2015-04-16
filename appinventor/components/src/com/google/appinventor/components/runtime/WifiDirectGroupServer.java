package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.ErrorMessages;
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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

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
    private ArrayList<WifiDirectPeer> activePeers;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

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

                    p.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                    p.addLast(new StringEncoder());
                    p.addLast(new StringDecoder());
                    p.addLast(new WifiDirectGroupServerHandler(WifiDirectGroupServer.this));
                }
            });

            // Start the server.
            ChannelFuture f = b.bind(this.hostAddress, this.port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shut down all event loops to terminate all threads.
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
        }
    }

    public void peerConnected(String peer) {
        this.p2p.ConnectionAccepted(peer);
    }

    public void acceptPeer(WifiDirectPeer peer) {
        this.p2p.ConnectionRegistered(peer.toString());
    }

    public void stop() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

    public ArrayList<WifiDirectPeer> getActivePeers() {
        return this.activePeers;
    }

    public String getActivePeersInJSON() {
        return new Gson().toJson(this.activePeers);
    }

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

    private String getAddress(String remoteAddress) {
        String[] parts = remoteAddress.split(":");
        return parts[0];
    }

    public void trigger(String msg) {
        this.p2p.Trigger(msg);
    }
}
