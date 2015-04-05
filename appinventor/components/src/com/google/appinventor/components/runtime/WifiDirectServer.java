package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * Java NIO Server for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectServer implements Runnable {
    private WifiDirectP2P go;
    private InetAddress hostAddress;
    private int port;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(WifiDirectUtil.defaultBufferSize);
    private WifiDirectWorker worker;
    private final List<WifiDirectChangeRequest> pendingChanges = new LinkedList<WifiDirectChangeRequest>();
    private final Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

    public WifiDirectServer(WifiDirectP2P go, InetAddress hostAddress, int port, WifiDirectWorker worker) throws IOException {
        this.go = go;
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
        this.worker = worker;
    }

    public void send(SocketChannel socket, byte[] data) {
        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new WifiDirectChangeRequest(socket, WifiDirectChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            synchronized (this.pendingData) {
                List<ByteBuffer> queue = this.pendingData.get(socket);
                if (queue == null) {
                    queue = new ArrayList<ByteBuffer>();
                    this.pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }

        this.selector.wakeup();
    }

    public void run() {
        while (this.go.IsAccepting()) {
            try {
                synchronized (this.pendingChanges) {
                    for (WifiDirectChangeRequest pendingChange : this.pendingChanges) {
                        switch (pendingChange.type) {
                            case WifiDirectChangeRequest.CHANGEOPS:
                                SelectionKey key = pendingChange.socket.keyFor(this.selector);
                                key.interestOps(pendingChange.ops);
                                break;
                        }
                    }
                    this.pendingChanges.clear();
                }

                this.selector.select();

                Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        this.accept(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        socketChannel.register(this.selector, SelectionKey.OP_READ);
        this.go.ConnectionAccepted(socket.getInetAddress().getHostAddress());
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        this.readBuffer.clear();

        int numRead;
        try {
            numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            key.cancel();
            socketChannel.close();
            return;
        }

        if (numRead == -1) {
            key.channel().close();
            key.cancel();
            return;
        }

        this.worker.processData(this, socketChannel, this.readBuffer.array(), numRead);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List<ByteBuffer> queue = this.pendingData.get(socketChannel);

            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    break;
                }
                queue.remove(0);
            }

            if (queue.isEmpty()) {

                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(isa);

        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }
}
