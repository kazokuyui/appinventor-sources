package com.google.appinventor.components.runtime;

import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class WifiDirectClient implements Runnable {
    private InetAddress hostAddress;
    private int port;
    private Selector selector;

    private ByteBuffer readBuffer = ByteBuffer.allocate(WifiDirectUtil.defaultBufferSize);
    private final List pendingChanges = new LinkedList();
    private final Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
    private Map<SocketChannel, WifiDirectRSPHandler> rspHandlers = Collections.synchronizedMap(new HashMap<SocketChannel, WifiDirectRSPHandler>());

    public WifiDirectClient(InetAddress hostAddress, int port) throws IOException {
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
    }

    public void send(byte[] data, WifiDirectRSPHandler handler) throws IOException {
        SocketChannel socket = this.initiateConnection();

        this.rspHandlers.put(socket, handler);

        synchronized (this.pendingData) {
            List<ByteBuffer> queue = this.pendingData.get(socket);
            if (queue == null) {
                queue = new ArrayList<ByteBuffer>();
                this.pendingData.put(socket, queue);
            }
            queue.add(ByteBuffer.wrap(data));
        }

        this.selector.wakeup();
    }

    public void run() {
        while (true) {
            try {
                synchronized (this.pendingChanges) {
                    for (Object pendingChange : this.pendingChanges) {
                        WifiDirectChangeRequest change = (WifiDirectChangeRequest) pendingChange;
                        switch (change.type) {
                            case WifiDirectChangeRequest.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(this.selector);
                                key.interestOps(change.ops);
                                break;
                            case WifiDirectChangeRequest.REGISTER:
                                change.socket.register(this.selector, change.ops);
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

                    if (key.isConnectable()) {
                        this.finishConnection(key);
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

        this.handleResponse(socketChannel, this.readBuffer.array(), numRead);
    }

    private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
        byte[] rspData = new byte[numRead];
        System.arraycopy(data, 0, rspData, 0, numRead);

        WifiDirectRSPHandler handler = this.rspHandlers.get(socketChannel);

        if (handler.handleResponse(rspData)) {
            socketChannel.close();
            socketChannel.keyFor(this.selector).cancel();
        }
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

    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            System.out.println(e);
            key.cancel();
            return;
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private SocketChannel initiateConnection() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        socketChannel.connect(new InetSocketAddress(this.hostAddress, this.port));

        synchronized(this.pendingChanges) {
            this.pendingChanges.add(new WifiDirectChangeRequest(socketChannel, WifiDirectChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }

        return socketChannel;
    }

    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }
}
