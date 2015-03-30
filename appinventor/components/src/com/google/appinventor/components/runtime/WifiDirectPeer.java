package com.google.appinventor.components.runtime;

import android.net.wifi.p2p.WifiP2pDevice;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
@DesignerComponent(version = YaVersion.WIFIDIRECTPEER_COMPONENT_VERSION,
        description = "Non-visible component that provides the Peer functions in a P2P network via WiFi Direct",
        designerHelpDescription = "Wifi Direct Peer Component",
        category = ComponentCategory.CONNECTIVITY,
        nonVisible = true,
        iconName = "images/wifiDirect.png")
@SimpleObject

public final class WifiDirectPeer extends WifiDirectBase {

    private WifiP2pDevice groupOwner;
    private Socket controlSocket;
    private Socket clientSocket;

    public WifiDirectPeer(ComponentContainer container) {
        super(container, "WifiDirectClient");
    }

    @SimpleProperty(description = "Returns the Group owner of the P2P group",
            category = PropertyCategory.BEHAVIOR)
    public String GroupOwner() {
        return WifiDirectUtil.deviceToString(this.groupOwner);
    }

    @SimpleEvent(description = "Device is now registered to the group owner")
    public void DeviceRegistered(String IPAddress) {
        EventDispatcher.dispatchEvent(this, "DeviceRegistered", IPAddress);
    }

    @SimpleProperty(description = "Returns the Group owner's host address")
    public String GroupOwnerHostAddress() {
        if(this.mConnectionInfo != null) {
            return this.mConnectionInfo.groupOwnerAddress.getHostAddress();
        }
        return WifiDirectUtil.defaultDeviceIPAddress;
    }

    @SimpleFunction(description = "Registers device to the group owner")
    public boolean RegisterDevice(int port) {
        if(this.isConnected) {
            try {
                this.initiateConnection(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @SimpleFunction(description = "Requests the list of peers addresses from the group owner")
    public void RequestPeers() {}

    @SimpleFunction(description = "Send data to a particular device")
    public void SendData(String address, int port) {}

    @Override
    public void onDelete() {}

    @Override
    public void onDestroy() {}

    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    private SocketChannel initiateConnection(int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        socketChannel.connect(new InetSocketAddress(this.GroupOwnerHostAddress(), port));

        return socketChannel;
    }
}