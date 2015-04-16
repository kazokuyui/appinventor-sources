package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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
    private int state;
    private WifiDirectPeer representation;

    public WifiDirectClient(WifiDirectP2P p2p, InetAddress hostAddress, int port) throws IOException {
        this.p2p = p2p;
        this.hostAddress = hostAddress;
        this.port = port;
        this.group = new NioEventLoopGroup();
    }

    public void run() {
        SslContext sslCtx = this.initiateSsl();
        try {
            Bootstrap b = new Bootstrap();
            final SslContext finalSslCtx = sslCtx;
            b.group(this.group);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.channel(NioSocketChannel.class);
            b.handler(new LoggingHandler(LogLevel.INFO));
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();

                    if (finalSslCtx != null) {
                        p.addLast(finalSslCtx.newHandler(ch.alloc(),
                                                         WifiDirectClient.this.hostAddress.getHostAddress(),
                                                         WifiDirectClient.this.port));
                    }

                    p.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                    p.addLast(new StringEncoder());
                    p.addLast(new StringDecoder());
                    p.addLast(new WifiDirectClientHandler(WifiDirectClient.this));
                }
            });

            // Start the client.
            final ChannelFuture f = b.connect(this.hostAddress.getHostAddress(), this.port).sync();
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    WifiDirectClient.this.setmSocket(f.channel().remoteAddress());
                }
            });

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
        this.p2p.ConnectedToGroupOwner();
    }

    public void peerRegistered() {
        SocketAddress socketAddress = this.mSocket;
        if(socketAddress != null) {
            this.p2p.DeviceRegistered(socketAddress.toString());
        }
        else {
            this.p2p.DeviceRegistered(WifiDirectUtil.defaultDeviceIPAddress);
        }
    }

    public void setmSocket(SocketAddress socketAddress) {
        this.mSocket = socketAddress;
    }

    public void trigger(String msg) {
        this.p2p.Trigger(msg);
    }
}
