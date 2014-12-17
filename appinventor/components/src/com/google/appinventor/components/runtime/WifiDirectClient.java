package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.*;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 */
@DesignerComponent(version = YaVersion.WIFIDIRECTCLIENT_COMPONENT_VERSION,
        designerHelpDescription = "Wifi Direct Client Component",
        category = ComponentCategory.CONNECTIVITY,
        nonVisible = true,
        iconName = "images/wifiDirect.png")
@SimpleObject
@UsesPermissions(permissionNames =
        "android.permission.ACCESS_WIFI_STATE, " +
                "android.permission.CHANGE_WIFI_STATE, " +
                "android.permission.CHANGE_NETWORK_STATE, " +
                "android.permission.ACCESS_NETWORK_STATE, " +
                "android.permission.CHANGE_WIFI_MULTICAST_STATE, " +
                "android.permission.INTERNET, " +
                "android.permission.READ_PHONE_STATE")

public final class WifiDirectClient extends AndroidNonvisibleComponent implements
        Component, Deleteable, OnDestroyListener {

    protected final String TAG;
    protected boolean isAvailable;
    protected boolean isConnected;

    protected WifiP2pManager manager;
    protected WifiP2pManager.Channel channel;
    protected BroadcastReceiver receiver;
    protected final IntentFilter intentFilter = new IntentFilter();

    private WifiP2pDeviceList devices = new WifiP2pDeviceList();
    private Collection<WifiP2pDevice> peers;
    private WifiP2pDevice groupOwner;
    private WifiP2pInfo connectionInfo;

    private Socket socket;
    private int port;
    private int bufferSize;
    private int timeOut;

    public WifiDirectClient(ComponentContainer container) {
        this(container.$form(), "WifiDirectClient");
        form.registerForOnDestroy(this);
    }

    private WifiDirectClient(Form form, String TAG) {
        super(form);
        this.TAG = TAG;

        this.manager = (WifiP2pManager) form.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(form, form.getMainLooper(), null);
        this.receiver = new WifiDirectBroadcastReceiver(this.manager, this.channel, this);

        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        form.registerReceiver(this.receiver, this.intentFilter);

        socket = new Socket();
        this.isAvailable = false;
        this.isConnected = false;
        this.bufferSize = 1024;
        this.timeOut = 5000;
    }

    @SimpleEvent(description = "List of nearby devices is available")
    public void DevicesAvailable(){
        EventDispatcher.dispatchEvent(this, "DevicesAvailable");
    }

    @SimpleEvent(description = "Device is connected")
    public void DeviceConnected(){
        EventDispatcher.dispatchEvent(this, "DeviceConnected");
    }

    @SimpleEvent(description = "Peers Info is available")
    public void PeersAvailable() {
        EventDispatcher.dispatchEvent(this, "PeersAvailable");
    }

    @SimpleEvent(description = "Connection Info is available")
    public void ConnectionInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "ConnectionInfoAvailable");
    }

    @SimpleEvent(description = "Text is received")
    public void TextReceived(String text) {
        EventDispatcher.dispatchEvent(this, "TextReceived", text);
    }

    @SimpleEvent
    public void Trigger(String msg){
        /*sample event for testing purposes*/
        EventDispatcher.dispatchEvent(this, "Trigger", msg);
    }

    @SimpleProperty(description = "Whether WiFi Direct connection is available on the device",
            category = PropertyCategory.BEHAVIOR)
    public boolean Available() {
      return this.isAvailable;
    }

    @SimpleProperty(description = "Whether the device is connected to another device",
            category = PropertyCategory.BEHAVIOR)
    public boolean Connected() {
        return this.isConnected;
    }

    @SimpleProperty(description = "Returns the Group owner of the P2P group",
            category = PropertyCategory.BEHAVIOR)
    public String GroupOwner() {
        return deviceToString(this.groupOwner);
    }

    @SimpleProperty(description = "Returns the Group owner's host address")
    public String GroupOwnerHostAddress() {
        return this.connectionInfo.groupOwnerAddress.getHostAddress();
    }

    @SimpleProperty(description = "All the available devices near you",
            category = PropertyCategory.BEHAVIOR)
    public List<String> AvailableDevices() {
        List<String> availableDevices = new ArrayList<String>();
        if(this.devices != null) {
            for (WifiP2pDevice device : this.devices.getDeviceList()) {
                availableDevices.add(deviceToString(device)); //temp
            }
        }
        return availableDevices;
    }

    @SimpleProperty(description = "All the available peers in the network",
            category = PropertyCategory.BEHAVIOR)
    public List<String> AvailablePeers() {
        List<String> peers = new ArrayList<String>();
        for(WifiP2pDevice peer: this.peers) {
            peers.add(deviceToString(peer)); //temp
        }
        return peers;
    }

    @SimpleFunction(description = "Scan all devices nearby")
    public void DiscoverDevices() {
        this.manager.discoverPeers(this.channel, new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess(){

            }

            @Override
            public void onFailure(int reason){

            }
        });
    }

    @SimpleFunction(description = "Request list of peers")
    public void RequestPeers(){
        this.manager.requestGroupInfo(this.channel, (WifiP2pManager.GroupInfoListener) this.receiver);
    }

    @SimpleFunction(description = "Request connection info")
    public void RequestConnectionInfo(){
        this.manager.requestConnectionInfo(this.channel, (WifiP2pManager.ConnectionInfoListener) this.receiver);
    }

    @SimpleFunction(description = "Connect to a certain device")
    public void Connect(String address) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;

        this.manager.connect(this.channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });
    }

    @SimpleFunction(description = "Receive a message from a peer")
    public void ReceiveText(int port) {
        this.port = port;

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(WifiDirectClient.this.port);
                    Socket client = serverSocket.accept();

                    InputStream inputStream = client.getInputStream();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int length;
                    byte[] data = new byte[WifiDirectClient.this.bufferSize];

                    while((length = inputStream.read(data, 0, data.length)) != -1){
                        buffer.write(data, 0, length);
                    }

                    String msg = buffer.toString();

                    TextReceived(msg);
                } catch (IOException e) {
                    wifiDirectError("ReceiveText",
                            ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                            e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Send a message to a peer")
    public void SendText(String address, int port, String text){
        byte[] buffer = new byte[this.bufferSize];
        int length;
        this.port = port;
        try{
            this.socket.bind(null);
            this.socket.connect(new InetSocketAddress(address, this.port), this.timeOut);

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

            while((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            wifiDirectError("SendText",
                    ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                    e.getMessage());
        } finally {
            if(this.socket != null) {
                if(this.socket.isConnected()) {
                    try {
                        this.socket.close();
                    }catch (IOException e){
                        wifiDirectError("SendText",
                                ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                                e.getMessage());
                    }
                }
            }
        }
    }

    public void setAvailableDevices(WifiP2pDeviceList wifiP2pDeviceList) {
        this.devices = wifiP2pDeviceList;
    }

    public void setPeers(Collection<WifiP2pDevice> peers) {
        this.peers = peers;
    }

    public void setGroupOwner(WifiP2pDevice groupOwner) {
        this.groupOwner = groupOwner;
    }

    public void setConnectionInfo(WifiP2pInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public String deviceToString(WifiP2pDevice device) {
        return "[" + device.deviceName + "] " + device.deviceAddress;
    }

    protected void wifiDirectError(String functionName, int errorCode, Object... args) {
        this.form.dispatchErrorOccurredEvent(this, functionName, errorCode, args);
    }

    @Override
    public void onDelete() {

    }

    @Override
    public void onDestroy() {

    }
}