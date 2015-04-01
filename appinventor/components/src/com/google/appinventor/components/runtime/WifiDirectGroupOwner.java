package com.google.appinventor.components.runtime;

import android.net.wifi.p2p.WifiP2pDevice;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
@DesignerComponent(version = YaVersion.WIFIDIRECTGROUPOWNER_COMPONENT_VERSION,
        description = "Non-visible component that provides the Group Owner functions in a P2P network via WiFi Direct",
        designerHelpDescription = "WiFi Direct Group Owner Component",
        category = ComponentCategory.CONNECTIVITY,
        nonVisible = true,
        iconName = "images/wifiDirect.png")

@SimpleObject
public class WifiDirectGroupOwner extends WifiDirectBase{

    private int serverPort;
    private boolean isAccepting;

    protected WifiDirectGroupOwner(ComponentContainer container) {
        super(container, "WifiDirectGroupOwner");
        this.serverPort = WifiDirectUtil.defaultServerPort;
        this.isAccepting = false;
    }

    @SimpleEvent(description = "Connection of a peer is accepted")
    public void ConnectionAccepted(String deviceName) {
        EventDispatcher.dispatchEvent(this, "ConnectionAccepted", deviceName);
    }

    @SimpleProperty(description = "Server port", category = PropertyCategory.BEHAVIOR)
    public int ServerPort() {
        return this.serverPort;
    }

    @SimpleProperty
    public void ServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    @SimpleProperty
    public boolean IsAccepting() {
        return this.isAccepting;
    }

    @SimpleFunction(description = "Start accepting new connections")
    public void AcceptConnection() {
        this.isAccepting = true;
        WifiDirectWorker worker = new WifiDirectWorker();

        try {
            AsynchUtil.runAsynchronously(worker);
            AsynchUtil.runAsynchronously(new WifiDirectServer(this, null, this.serverPort, worker));
        } catch (IOException e) {
            wifiDirectError("AcceptConnection",
                            ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                            e.getMessage());
        }
    }

    @SimpleFunction(description = "Stop accepting new connection")
    public void StopAcceptingConnection(){
        this.isAccepting = false;
        // close here
    }

    @SimpleFunction(description = "Broadcasts the latest list of peers")
    public void BroadcastPeers() {

    }

    @Override
    public void setPeers(Collection<WifiP2pDevice> peers) {
        this.mPeers = peers;
        PeersAvailable();
    }

    @Override
    public void onDelete() {
    }

    @Override
    public void onDestroy() {

    }
}
