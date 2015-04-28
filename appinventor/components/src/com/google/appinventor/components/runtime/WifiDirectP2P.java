package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.*;
import android.os.Handler;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A peer-to-peer(p2p) component via WiFi Direct
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

@DesignerComponent(version = YaVersion.WIFIDIRECTPEER_COMPONENT_VERSION,
                   description = "Non-visible component that provides network functions in a P2P network via WiFi Direct",
                   designerHelpDescription = "Wifi Direct Peer-to-Peer Component",
                   category = ComponentCategory.CONNECTIVITY,
                   nonVisible = true,
                   iconName = "images/wifiDirect.png")
@UsesLibraries(libraries = "netty.jar,json.jar,gson-2.1.jar")
@UsesPermissions(permissionNames = "android.permission.ACCESS_WIFI_STATE, " +
                 "android.permission.CHANGE_WIFI_STATE, " +
                 "android.permission.CHANGE_NETWORK_STATE, " +
                 "android.permission.ACCESS_NETWORK_STATE, " +
                 "android.permission.CHANGE_WIFI_MULTICAST_STATE, " +
                 "android.permission.INTERNET, " +
                 "android.permission.READ_PHONE_STATE")
@SimpleObject
public class WifiDirectP2P extends AndroidNonvisibleComponent implements Component, OnDestroyListener, Deleteable {
    public String TAG;

    /* Network layer for WifiDirect access */
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;

    /* WifiDirect Info for this Device */
    private WifiP2pDevice mDevice;
    private WifiP2pDevice mGroupOwner;
    private WifiP2pGroup mGroup;
    private WifiP2pInfo mConnectionInfo;

    /* Application Layer Control Planes */
    private WifiDirectGroupServer groupServer;
    private WifiDirectControlClient controlClient;
    private Collection<WifiP2pDevice> availableDevices;
    private Collection<WifiDirectPeer> availablePeers;

    private Handler handler;

    public WifiDirectP2P(ComponentContainer container) {
        this(container.$form(), "WifiDirectP2P");
        this.form.registerForOnDestroy(this);
    }

    private WifiDirectP2P(Form form, String TAG) {
        super(form);
        this.TAG = TAG;
        this.manager = (WifiP2pManager) form.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(form, form.getMainLooper(), null);
        this.receiver = new WifiDirectBroadcastReceiver(this.manager, this.channel, this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        form.registerReceiver(this.receiver, intentFilter);

        this.handler = new Handler();
    }

    /* Network Layer Events */
    @SimpleEvent(description = "List of nearby devices is available; Triggered by DiscoverDevices")
    public void DevicesAvailable() {
        EventDispatcher.dispatchEvent(this, "DevicesAvailable");
    }

    @SimpleEvent(description = "This device information is available; Triggered by RequestConnectionInfo")
    public void DeviceInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "DeviceInfoAvailable");
    }

    @SimpleEvent(description = "Connection information is available; Triggered by RequestConnectionInfo")
    public void ConnectionInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "ConnectionInfoAvailable");
    }

    @SimpleEvent(description = "Group information is available; Triggered by RequestGroupInfo")
    public void GroupInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "GroupInfoAvailable");
    }

    @SimpleEvent(description = "This device is connected; Triggered by Connect")
    public void ConnectedToNetwork() {
        EventDispatcher.dispatchEvent(this, "ConnectedToNetwork");
    }

    @SimpleEvent(description = "Channel is disconnected to the network; Triggered by Disconnect")
    public void DisconnectedToNetwork() {
        EventDispatcher.dispatchEvent(this, "DisconnectedToNetwork");
    }

    /* Client Application Layer Events */
    @SimpleEvent(description = "Device connected to the Group Owner Server; Triggered by ControlClient.peerConnected")
    public void DeviceConnected(String ipAddress) {
        EventDispatcher.dispatchEvent(this, "DeviceConnected", ipAddress);
    }

    @SimpleEvent(description = "Device is now registered to the Group Owner Server; Triggered by ControlClient.peerRegistered")
    public void DeviceRegistered(String client) {
        EventDispatcher.dispatchEvent(this, "DeviceRegistered", client);
    }

    @SimpleEvent(description = "Peers information is available from the Group Owner Server")
    public void PeersAvailable()  {
        EventDispatcher.dispatchEvent(this, "PeersAvailable");
    }

    /* Server Application Layer Events */
    @SimpleEvent(description = "Connection of a peer is accepted by the Group Owner Server; Triggered by GroupServer.peerConnected")
    public void ConnectionAccepted(String ipAddress) {
        EventDispatcher.dispatchEvent(this, "ConnectionAccepted", ipAddress);
    }

    @SimpleEvent(description = "Connection of a peer is registered by the Group Owner Server; Triggered by GroupServer.peerRegistered")
    public void ConnectionRegistered(String deviceName) {
        EventDispatcher.dispatchEvent(this, "ConnectionRegistered", deviceName);
    }

    /* Core Events of the framework for Peer-to-Peer communication */
    @SimpleEvent(description = "Data is received")
    public void DataReceived(final String msg) {
        EventDispatcher.dispatchEvent(this, "DataReceived", msg);
    }

    @SimpleEvent(description = "Data sent")
    public void DataSent(String msg) {
        EventDispatcher.dispatchEvent(this, "DataSent", msg);
    }

    @SimpleEvent(description = "For testing purposes only")
    public void Trigger(final String msg){
        EventDispatcher.dispatchEvent(this, "Trigger", msg);
    }

    @SimpleProperty(description = "Returns true if this device is WiFi-enabled",
                    category = PropertyCategory.BEHAVIOR)
    public boolean IsWifiEnabled() {
        WifiManager wifiManager = (WifiManager) this.form.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    @SimpleProperty(description = "Returns true if WiFi Direct connection is available",
                    category = PropertyCategory.BEHAVIOR)
    public boolean Available() {
        return false;
    }

    @SimpleProperty(description = "Returns true if this device is connected to any other device",
                    category = PropertyCategory.BEHAVIOR)
    public boolean Connected() {
        return false;
    }

    @SimpleProperty(description = "Returns the representation of this device with the format" +
                    "[deviceName] deviceMACAddress",
                    category = PropertyCategory.BEHAVIOR)
    public String Device() {
        return WifiDirectUtil.deviceToString(this.mDevice);
    }

    @SimpleProperty(description = "Returns name of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceName() {
        if(this.mDevice != null) {
            return this.mDevice.deviceName;
        }
        return WifiDirectUtil.defaultDeviceName;
    }

    @SimpleProperty(description = "Returns status of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceStatus() {
        return WifiDirectUtil.getDeviceStatus(this.mDevice);
    }

    @SimpleProperty(description = "Returns MAC address of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceMACAddress() {
        if(this.mDevice != null) {
            return this.mDevice.deviceAddress;
        }
        return WifiDirectUtil.defaultDeviceMACAddress;
    }

    @SimpleProperty(description = "Returns the IP address of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceIPAddress() {
        return WifiDirectUtil.defaultDeviceIPAddress;
    }

    @SimpleProperty(description = "All the available devices near you",
                    category = PropertyCategory.BEHAVIOR)
    public List<String> AvailableDevices() {
        List<String> availableDevices = new ArrayList<String>();
        if(this.availableDevices != null) {
            for (WifiP2pDevice device : this.availableDevices) {
                availableDevices.add(WifiDirectUtil.deviceToString(device));
            }
        }
        return availableDevices;
    }

    @SimpleProperty(description = "All the available peers in the network",
                    category = PropertyCategory.BEHAVIOR)
    public List<String> AvailablePeers() {
        List<String> peers = new ArrayList<String>();
        return peers;
    }

    @SimpleProperty(description = "Returns the name of the group",
                    category = PropertyCategory.BEHAVIOR)
    public String GroupName() {
        if(this.mGroup != null) {
            return this.mGroup.getNetworkName();
        }
        return WifiDirectUtil.defaultGroupName;
    }

    @SimpleProperty(description = "Returns the Group owner's device representation",
                    category = PropertyCategory.BEHAVIOR)
    public String GroupOwner() {
        if(this.mGroupOwner != null) {
            return WifiDirectUtil.deviceToString(this.mGroupOwner);
        }
        return WifiDirectUtil.defaultDeviceName;
    }

    @SimpleProperty(description = "Returns the Group owner's host address",
                    category = PropertyCategory.BEHAVIOR)
    public String GroupOwnerAddress() {
        if(this.mConnectionInfo != null) {
            return this.mConnectionInfo.groupOwnerAddress.getHostAddress();
        }
        return WifiDirectUtil.defaultDeviceIPAddress;
    }

    @SimpleProperty(description = "Returns the passphrase for the group",
                    category = PropertyCategory.BEHAVIOR)
    public String GroupPassphrase() {
        if(this.mGroup != null) {
            return this.mGroup.getPassphrase();
        }
        return "Unknown";
    }

    @SimpleProperty(description = "Returns true if this device is a Group Owner",
                    category = PropertyCategory.BEHAVIOR)
    public boolean IsGroupOwner() {
        return this.mConnectionInfo != null && this.mConnectionInfo.isGroupOwner;
    }


    @SimpleFunction(description = "Scan all devices nearby")
    public void DiscoverDevices() {
        this.manager.discoverPeers(this.channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    @SimpleFunction(description = "Request connection info")
    public void RequestConnectionInfo(){
        this.manager.requestConnectionInfo(this.channel, (WifiP2pManager.ConnectionInfoListener) this.receiver);
    }

    @SimpleFunction(description = "Request group info")
    public void RequestGroupInfo(){
        this.manager.requestGroupInfo(this.channel, (WifiP2pManager.GroupInfoListener) this.receiver);
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

    @SimpleFunction(description = "Receive data from a particular device")
    public void ReceiveData(String address, int port) {

    }

    @SimpleFunction(description = "Requests the list of peers addresses from the group owner")
    public void RequestPeers() {
    }

    @SimpleFunction(description = "Start Group Owner Server")
    public void StartGOServer() {
        if(this.IsGroupOwner()) {
            try {
                this.groupServer = new WifiDirectGroupServer(this, null, WifiDirectUtil.defaultGroupServerPort);
                AsynchUtil.runAsynchronously(this.groupServer);
            } catch (IOException e) {
                wifiDirectError("StartGOServer",
                                ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                                e.getMessage());
            }
        }
    }

    @SimpleFunction(description = "Stop Group Owner Server")
    public void StopGOServer(){
        this.groupServer.stop();
    }

    @SimpleFunction(description = "Start the client and connect to the Group Owner in the specified port")
    public void StartClient(int port) {
        try {
            this.controlClient = new WifiDirectControlClient(this, this.mConnectionInfo.groupOwnerAddress, port);
            this.controlClient.setHandler(this.handler);
            AsynchUtil.runAsynchronously(this.controlClient);
        } catch (Exception e) {
            wifiDirectError("StartClient",
                            ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                            e.getMessage());
        }
    }

    @SimpleFunction(description = "Stop the client")
    public void StopClient() {
        this.controlClient.stop();
    }

    @Override
    public void onDelete() {

    }

    @Override
    public void onDestroy() {

    }

    public void setAvailableDevices(WifiP2pDeviceList wifiP2pDeviceList) {
        this.availableDevices = wifiP2pDeviceList.getDeviceList();
    }

    public void setPeers(Collection<WifiDirectPeer> peers) {
        this.availablePeers = peers;
    }

    public void setDevice(WifiP2pDevice device) {
        this.mDevice = device;
    }

    public void setConnectionInfo(WifiP2pInfo connectionInfo) {
        this.mConnectionInfo = connectionInfo;
    }

    public void setP2PGroup(WifiP2pGroup mGroup) {
        this.mGroup = mGroup;
        this.setP2PGroupOwner(this.mGroup.getOwner());
    }

    public void setP2PGroupOwner(WifiP2pDevice go) {
        this.mGroupOwner = go;
    }

    public void setIsAvailable(boolean isAvailable) {
        return;
    }

    public void setIsConnected(boolean isConnected) {
        return;
    }

    public void setIsRegistered(boolean isRegistered) {
        return;
    }

    public void wifiDirectError(String functionName, int errorCode, Object... args) {
        this.form.dispatchErrorOccurredEvent(this, functionName, errorCode, args);
    }
}
