package com.google.appinventor.components.runtime;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.net.Socket;

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
    private Socket socket;
    private int port;
    private int timeOut;
    private boolean isGroupOwner;

    public WifiDirectPeer(ComponentContainer container) {
        super(container, "WifiDirectClient");
        this.isGroupOwner = false;
    }

    @SimpleEvent(description = "Data is received")
    public void DataReceived(String msg) {
        EventDispatcher.dispatchEvent(this, "DataReceived", msg);
    }

    @SimpleEvent(description = "Data sent")
    public void DataSent(String msg) {
        EventDispatcher.dispatchEvent(this, "DataSent", msg);
    }

    @SimpleProperty(description = "Returns the Group owner of the P2P group",
            category = PropertyCategory.BEHAVIOR)
    public String GroupOwner() {
        return WifiDirectUtil.deviceToString(this.groupOwner);
    }

    @SimpleProperty(description = "Returns the Group owner's host address")
    public String GroupOwnerHostAddress() {
        return this.mConnectionInfo.groupOwnerAddress.getHostAddress();
    }

    @SimpleFunction(description = "Requests the list of peers addresses from the group owner")
    public void RequestPeers() {}

    @SimpleFunction(description = "Request connection info")
    public void RequestConnectionInfo(){
        this.manager.requestConnectionInfo(this.channel, (WifiP2pManager.ConnectionInfoListener) this.receiver);
    }

    @SimpleFunction(description = "Connect to a certain device")
    public void Connect(String MACAddress) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = MACAddress;

        this.manager.connect(this.channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });
    }

    @SimpleFunction(description = "Send data to a particular device")
    public void SendData(String address, int port) {}

    @Override
    public void onDelete() {}

    @Override
    public void onDestroy() {}
}