package com.google.appinventor.components.runtime;

import java.nio.channels.SocketChannel;

/**
 * Data Event for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class WifiDirectDataEvent {

    public WifiDirectServer server;
    public SocketChannel socket;
    public byte[] data;

    public WifiDirectDataEvent(WifiDirectServer server, SocketChannel socket, byte[] data) {
        this.server = server;
        this.socket = socket;
        this.data = data;
    }
}
