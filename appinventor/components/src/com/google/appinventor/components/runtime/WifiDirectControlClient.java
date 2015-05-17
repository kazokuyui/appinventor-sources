package com.google.appinventor.components.runtime;

import android.os.Handler;
import com.google.appinventor.components.runtime.util.PeerMessage;
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
import java.util.Collection;

public class WifiDirectControlClient implements Runnable {
    private WifiDirectP2P p2p;
    private Handler handler;
    private String status;
    private WifiDirectPeer mPeer;
    private WifiDirectControlClientHandler clientHandler;
    private Channel serverChannel;

    private InetAddress hostAddress;
    private int port;
    private Collection<WifiDirectPeer> peers;

    private EventLoopGroup group;

    public boolean isRunning;

    public WifiDirectControlClient(WifiDirectP2P p2p, InetAddress hostAddress, int port) throws IOException {
        this.p2p = p2p;
        this.hostAddress = hostAddress;
        this.port = port;
        this.group = new NioEventLoopGroup();
        this.clientHandler = new WifiDirectControlClientHandler(this);
        this.isRunning = false;
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
                                                         WifiDirectControlClient.this.hostAddress.getHostAddress(),
                                                         WifiDirectControlClient.this.port));
                    }

                    p.addLast(new DelimiterBasedFrameDecoder(WifiDirectUtil.controlBufferSize,
                                                             Delimiters.lineDelimiter()));
                    p.addLast(new StringEncoder());
                    p.addLast(new StringDecoder());
                    p.addLast(WifiDirectControlClient.this.clientHandler);
                }
            });

            final ChannelFuture f = b.connect(this.hostAddress.getHostAddress(), this.port).sync();
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    WifiDirectControlClient.this.serverChannel = f.channel();
                }
            });
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.group.shutdownGracefully();
        }
    }

    public void requestPeers() {
        this.clientHandler.requestPeers(this.serverChannel);
    }

    public void requestCall(int peerId) {
        this.clientHandler.requestCall(this.serverChannel, peerId);
    }

    public void acceptCall(int peerId) {
        this.clientHandler.acceptCall(this.serverChannel, peerId);
    }

    public void rejectCall(int peerId) {
        this.clientHandler.rejectCall(this.serverChannel, peerId);
    }

    public void sendMessage(String msg) {
        this.clientHandler.sendMessage(this.serverChannel, msg);
    }

    public void stop() {
        this.clientHandler.quit(this.serverChannel);
        this.group.shutdownGracefully();
        this.isRunning = false;
        this.deviceDisconnected();
    }

    /* Client events */
    public void peerConnected(final String ipAddress) {
        this.status = PeerMessage.CTRL_CONNECTED;
        this.mPeer.setIpAddress(ipAddress);
        this.isRunning = true;
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.DeviceConnected(ipAddress);
            }
        });
    }

    public void peerRegistered(final int id) {
        this.status = PeerMessage.CTRL_REGISTERED;
        this.mPeer.setId(id);
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.DeviceRegistered(id);
            }
        });
    }

    public void peersAvailable(Collection<WifiDirectPeer> newPeers) {
        this.peers = newPeers;
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.PeersAvailable();
            }
        });
    }

    public void peersChanged() {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.PeersChanged();
            }
        });
    }

    public void callRequested(final WifiDirectPeer peer) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.CallReceived(peer.toString());
            }
        });
    }

    public void callAccepted(final WifiDirectPeer peer) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.CallAccepted(peer.toString());
            }
        });
    }

    public void callRejected(final WifiDirectPeer peer) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.CallRejected(peer.toString());
            }
        });
    }

    public void deviceDisconnected() {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.DeviceDisconnected();
            }
        });
    }

    public void messageReceived(final String fromPeer, final String msg) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.DataReceived(fromPeer, msg);
            }
        });
    }

    public void errorOccurred(final String functionName, final int errorCode, final String cause) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                WifiDirectControlClient.this.p2p.wifiDirectError(functionName, errorCode, cause);
            }
        });
    }

    /* Setters and Getters */
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

    public void setHandler(Handler uiHandler) {
        this.handler = uiHandler;
    }

    public InetAddress getHostAddress() {
        return this.hostAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setmPeer(WifiDirectPeer wifiDirectPeer) {
        this.mPeer = wifiDirectPeer;
    }

    public WifiDirectPeer getmPeer() {
        return mPeer;
    }

    public Collection<WifiDirectPeer> getPeers() {
        return this.peers;
    }

    public WifiDirectPeer getPeerById(int peerId) {
        if(this.peers.size() > 0) {
            for (WifiDirectPeer peer : this.peers) {
                if(peer.getId() == peerId) {
                    return peer;
                }
            }
        }

        return null;
    }

    /* For testing purposes */
    public void trigger(String msg) {
        this.p2p.Trigger(msg);
    }
}
