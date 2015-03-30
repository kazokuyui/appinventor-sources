package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.*;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.net.wifi.p2p.WifiP2pManager.Channel;

/**
 * An abstract base class for WifiDirectGroupOwner and WifiDirectClient
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.ACCESS_WIFI_STATE, " +
        "android.permission.CHANGE_WIFI_STATE, " +
        "android.permission.CHANGE_NETWORK_STATE, " +
        "android.permission.ACCESS_NETWORK_STATE, " +
        "android.permission.CHANGE_WIFI_MULTICAST_STATE, " +
        "android.permission.INTERNET, " +
        "android.permission.READ_PHONE_STATE")

public abstract class WifiDirectBase extends AndroidNonvisibleComponent
        implements Component, OnDestroyListener, Deleteable {

    protected String TAG;

    protected WifiP2pManager manager;
    protected Channel channel;
    protected BroadcastReceiver receiver;

    protected IntentFilter intentFilter;
    protected Collection<WifiP2pDevice> availableDevices;
    protected Collection<WifiP2pDevice> mPeers;
    protected WifiP2pDevice mDevice;
    protected WifiP2pGroup mGroup;
    protected WifiP2pInfo mConnectionInfo;

    protected String IPAddress;
    protected boolean isAvailable;
    protected boolean isConnected;

    protected WifiDirectBase(ComponentContainer container, String TAG) {
        this(container.$form(), TAG);
        form.registerForOnDestroy(this);
    }

    private WifiDirectBase(Form form, String TAG) {
        super(form);
        this.TAG = TAG;
        this.manager = (WifiP2pManager) form.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(form, form.getMainLooper(), null);
        this.receiver = new WifiDirectBroadcastReceiver(this.manager, this.channel, this);

        this.intentFilter = new IntentFilter();
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        form.registerReceiver(this.receiver, this.intentFilter);

        this.isAvailable = false;
        this.isConnected = false;
        this.IPAddress = WifiDirectUtil.defaultDeviceIPAddress;
    }

    @SimpleEvent(description = "List of nearby devices is available")
    public void DevicesAvailable() {
        EventDispatcher.dispatchEvent(this, "DevicesAvailable");
    }

    @SimpleEvent(description = "This device information is available")
    public void DeviceInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "DeviceInfoAvailable");
    }

    @SimpleEvent(description = "This device is connected")
    public void DeviceConnected() {
        EventDispatcher.dispatchEvent(this, "DeviceConnected");
    }

    @SimpleEvent(description = "Peers information is available")
    public void PeersAvailable()  {
        EventDispatcher.dispatchEvent(this, "PeersAvailable");
    }

    @SimpleEvent(description = "Connection information is available")
    public void ConnectionInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "ConnectionInfoAvailable");
    }

    @SimpleEvent(description = "Connection information is available")
    public void GroupInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "GroupInfoAvailable");
    }

    @SimpleEvent(description = "Data is received")
    public void DataReceived(String msg) {
        EventDispatcher.dispatchEvent(this, "DataReceived", msg);
    }

    @SimpleEvent(description = "Data sent")
    public void DataSent(String msg) {
        EventDispatcher.dispatchEvent(this, "DataSent", msg);
    }

    @SimpleEvent(description = "Channel is disconnected")
    public void ChannelDisconnected() {
        EventDispatcher.dispatchEvent(this, "ChannelDisconnected");
    }

    @SimpleEvent(description = "For testing purposes only")
    public void Trigger(String msg){
        /*sample event for testing purposes*/
        EventDispatcher.dispatchEvent(this, "Trigger", msg);
    }

    @SimpleProperty(description = "Returns true if WiFi Direct connection is available",
                    category = PropertyCategory.BEHAVIOR)
    public boolean Available() {
        return this.isAvailable;
    }

    @SimpleProperty(description = "Returns true if this device is connected to any other device",
                    category = PropertyCategory.BEHAVIOR)
    public boolean Connected() {
        return this.isConnected;
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
        if(this.isConnected) {
            return this.IPAddress;
        }
        return WifiDirectUtil.defaultDeviceIPAddress;
    }

    @SimpleProperty(description = "Returns status of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceStatus() {
        return WifiDirectUtil.getDeviceStatus(this.mDevice);
    }

    @SimpleProperty(description = "All the available devices near you",
                    category = PropertyCategory.BEHAVIOR)
    public List<String> AvailableDevices() {
        List<String> availableDevices = new ArrayList<String>();
        if(this.availableDevices != null) {
            for (WifiP2pDevice device : this.availableDevices) {
                availableDevices.add(WifiDirectUtil.deviceToString(device)); //temp
            }
        }
        return availableDevices;
    }

    @SimpleProperty(description = "All the available peers in the network",
                    category = PropertyCategory.BEHAVIOR)
    public List<String> AvailablePeers() {
        List<String> peers = new ArrayList<String>();
        for(WifiP2pDevice peer: this.mPeers) {
            peers.add(WifiDirectUtil.deviceToString(peer)); //temp
        }
        return peers;
    }

    @SimpleProperty(description = "Returns true if this device is a Group Owner")
    public boolean IsGroupOwner() {
        if(this.mConnectionInfo != null) {
            return this.mConnectionInfo.isGroupOwner;
        }
        return false;
    }

    @SimpleProperty(description = "Returns true if this device is WiFi-enabled")
    public boolean IsWifiEnabled() {
        WifiManager wifiManager = (WifiManager) this.form.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
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
    public void SendData(String address, int port) {

    }

    @SimpleFunction(description = "Receive data from a particular device")
    public void ReceiveData(String address, int port) {

    }

    public void setAvailableDevices(WifiP2pDeviceList wifiP2pDeviceList) {
        this.availableDevices = wifiP2pDeviceList.getDeviceList();
    }

    public void setPeers(Collection<WifiP2pDevice> peers) {
        this.mPeers = peers;
    }

    public void setDevice(WifiP2pDevice device) {
        this.mDevice = device;
    }

    public void setConnectionInfo(WifiP2pInfo connectionInfo) {
        this.mConnectionInfo = connectionInfo;
    }

    public void setP2PGroup(WifiP2pGroup mGroup) {
        this.mGroup = mGroup;
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    protected void wifiDirectError(String functionName, int errorCode, Object... args) {
        this.form.dispatchErrorOccurredEvent(this, functionName, errorCode, args);
    }
}