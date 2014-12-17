package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.*;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import sun.misc.IOUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

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

    protected final String logTag;
    protected String encoding;

    protected WifiP2pManager manager;
    protected WifiP2pManager.Channel channel;
    protected BroadcastReceiver receiver;
    protected final IntentFilter intentFilter = new IntentFilter();

    private WifiP2pDeviceList devices = new WifiP2pDeviceList();
    private Collection<WifiP2pDevice> peers;
    private WifiP2pDevice groupOwner;
    private WifiP2pInfo connectionInfo;

    private Socket socket;

    public WifiDirectClient(ComponentContainer container) {
        this(container.$form(), "WifiDirectClient");
        form.registerForOnDestroy(this);
    }

    private WifiDirectClient(Form form, String logTag) {
        super(form);
        this.logTag = logTag;

        this.manager = (WifiP2pManager) form.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(form, form.getMainLooper(), null);
        this.receiver = new WifiDirectBroadcastReceiver(this.manager, this.channel, this);

        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        form.registerReceiver(this.receiver, this.intentFilter);

        socket = new Socket();
        CharacterEncoding("UTF-8");
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

    @SimpleEvent
    public void Trigger(String msg){
        /*sample event for testing purposes*/
        EventDispatcher.dispatchEvent(this, "Trigger", msg);
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
            defaultValue = "UTF-8")
    @SimpleProperty
    public void CharacterEncoding(String encoding) {
        try {
            "check".getBytes(encoding);
            this.encoding = encoding;
        }catch (UnsupportedEncodingException ignored) {

        }
    }

    @SimpleProperty(description = "Whether WiFi Direct is available on the device",
            category = PropertyCategory.BEHAVIOR)
    public boolean Available() {
        return true;
    }

    @SimpleProperty(description = "Whether WiFi Direct is enabled on the device",
            category = PropertyCategory.BEHAVIOR)
    public boolean Enabled() {
        return true;
    }

    @SimpleProperty(description = "Whether the device is connected to another device",
            category = PropertyCategory.BEHAVIOR)
    public boolean Connected() {
        return true;
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

    @SimpleProperty(description = "All the available peers in the network", category = PropertyCategory.BEHAVIOR)
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
    public void ReceiveMessage() {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(4545);
                    Socket client = serverSocket.accept();

                    InputStream inputStream = client.getInputStream();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int length;
                    byte[] data = new byte[1024];

                    while((length = inputStream.read(data, 0, data.length)) != -1){
                        buffer.write(data, 0, length);
                    }

                    String msg = buffer.toString();

                    Trigger(msg+"wow");
                } catch (Exception e) {
                    Trigger(e.toString());
                }
            }
        });
    }

    @SimpleFunction(description = "Send a message to a peer")
    public void SendMessage(String address, String message){
        byte buffer[] = new byte[1024];
        int length;

        try{
            this.socket.bind(null);
            this.socket.connect(new InetSocketAddress(address, 4545), 5000);

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));

            while((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
            Trigger(e.toString());
        } finally {
            if(this.socket != null) {
                if(this.socket.isConnected()) {
                    try {
                        this.socket.close();
                    }catch (IOException e){
                        e.printStackTrace();
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

    public String deviceToString(WifiP2pDevice device) {
        return "[" + device.deviceName + "] " + device.deviceAddress;
    }

    @Override
    public void onDelete() {

    }

    @Override
    public void onDestroy() {

    }
}