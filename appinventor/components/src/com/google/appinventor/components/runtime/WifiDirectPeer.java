package com.google.appinventor.components.runtime;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author nmcalabroso@up.edu.ph (neil)
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

    @SimpleEvent(description = "This device is now recognized by the Group Owner")
    public void DeviceRegistered(String groupOwner) {
        EventDispatcher.dispatchEvent(this, "DeviceRegistered", groupOwner);
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

    @SimpleProperty(description = "Returns true if this device is a Group Owner")
    public boolean IsGroupOwner() {
        return this.isGroupOwner;
    }

    @SimpleFunction(description = "Register this device to the group owner")
    public void RegisterToNetwork() {
        if (this.mConnectionInfo != null) {
            try {
                this.socket = new Socket(this.mConnectionInfo.groupOwnerAddress, this.timeOut);
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outputStream);
                out.writeUTF(WifiDirectUtil.REGISTER_TO_NETWORK);
                InputStream inputStream =  this.socket.getInputStream();
                DataInputStream in = new DataInputStream(inputStream);
                String serverResponse = in.readUTF();
                if (serverResponse.equals(WifiDirectUtil.REGISTRATION_ACCEPTED)) {
                    DeviceRegistered(this.socket.getRemoteSocketAddress().toString());
                }
                else {
                    wifiDirectError("RegisterToNetwork",
                            ErrorMessages.ERROR_WIFIDIRECT_REGISTRATION_FAILED,
                            this.socket.getRemoteSocketAddress().toString());
                }
            } catch (IOException e) {
                wifiDirectError("RegisterToNetwork",
                                ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                                e.getMessage());
            }
        }
    }

    @SimpleFunction(description = "Requests the list of peers addresses from the group owner")
    public void RequestPeers() {

    }

    @Override
    public void setP2PGroup(WifiP2pGroup mGroup) {
        Collection<WifiP2pDevice> list = new ArrayList<WifiP2pDevice>();
        list.addAll(mGroup.getClientList());

        if(!mGroup.isGroupOwner()){
            this.setGroupOwner(mGroup.getOwner());
        }
        else{
            WifiP2pDevice me = new WifiP2pDevice(mGroup.getOwner());
            if(me.deviceName.isEmpty()){
                me.deviceName = "Me";
            }
            this.setGroupOwner(me);
        }

        this.setPeers(list);
        this.PeersAvailable();
    }

    public void setGroupOwner(WifiP2pDevice groupOwner) {
        this.groupOwner = groupOwner;
    }

    @Override
    public void onDelete() {

    }

    @Override
    public void onDestroy() {

    }
}