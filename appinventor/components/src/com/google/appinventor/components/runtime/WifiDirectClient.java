package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.WifiDirectUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;

public class WifiDirectClient implements Runnable {
    private WifiDirectP2P p2p;
    private InetAddress hostAddress;
    private int port;
    private EventLoopGroup group;
    private SocketAddress mSocket;

    public WifiDirectClient(WifiDirectP2P p2p, InetAddress hostAddress, int port) throws IOException {
        this.p2p = p2p;
        this.hostAddress = hostAddress;
        this.port = port;
        this.group = new NioEventLoopGroup();
    }

    public void run() {
        // Configure SSL.git
        SslContext sslCtx = this.initiateSsl();

        try {
            Bootstrap b = new Bootstrap();
            final SslContext finalSslCtx = sslCtx;
            b.group(this.group);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.TCP_NODELAY, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    if (finalSslCtx != null) {
                        p.addLast(finalSslCtx.newHandler(ch.alloc(),
                                                         WifiDirectClient.this.hostAddress.getHostAddress(),
                                                         WifiDirectClient.this.port));
                    }

                    p.addLast(new WifiDirectClientHandler(WifiDirectClient.this));
                    WifiDirectClient.this.mSocket = ch.remoteAddress();
                }
            });

            // Start the client.
            ChannelFuture f = b.connect(this.hostAddress.getHostAddress(),
                                        this.port).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shut down the event loop to terminate all threads.
            this.group.shutdownGracefully();
        }
    }

    public void stop() {
        this.group.shutdownGracefully();
    }

    public SslContext initiateSsl() {
        if (WifiDirectUtil.SSL) {
            try {
                return SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public void peerConnected() {
        this.p2p.DeviceRegistered(this.mSocket.toString());
    }
}
