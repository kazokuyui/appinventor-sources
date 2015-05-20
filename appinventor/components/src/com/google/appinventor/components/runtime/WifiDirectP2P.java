package com.google.appinventor.components.runtime;

import android.content.Context;
import android.content.IntentFilter;
import android.media.*;
import android.net.wifi.p2p.*;
import android.os.Handler;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.appinventor.components.runtime.WifiDirectP2P.Status.*;
import static com.google.appinventor.components.runtime.util.WifiDirectUtil.*;

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
                 "android.permission.READ_PHONE_STATE, " +
                 "android.permission.RECORD_AUDIO")
@SimpleObject
public class WifiDirectP2P extends AndroidNonvisibleComponent implements Component, OnDestroyListener, Deleteable {
    public String TAG;

    public enum Status {
        Idle, Available, Unavailable,
        Invited, NetworkConnected, Connected,
        Registered, Inactive, Failed
    }

    /* Network layer for WifiDirect access */
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    /* WifiDirect Info for this Device */
    private WifiP2pDevice mDevice;
    private WifiP2pDevice mGroupOwner;
    private String originalGoMac;
    private WifiP2pGroup mGroup;
    private WifiP2pInfo mConnectionInfo;

    /* Application Layer Control Planes */
    private WifiDirectBroadcastReceiver receiver;
    private WifiDirectGroupServer groupServer;
    private WifiDirectControlClient controlClient;

    private Handler handler;
    private Status status;
    private boolean isCalling;
    private boolean isReleased;
    private String newPeerMacAddress;

    public WifiDirectP2P(ComponentContainer container) {
        this(container.$form(), "WifiDirectP2P");
        this.form.registerForOnDestroy(this);
    }

    private WifiDirectP2P(Form form, String TAG) {
        super(form);
        this.TAG = TAG;

        this.initializeChannel();

        this.handler = new Handler();
        this.status = Idle;
        this.isCalling = false;
        this.isReleased = false;
    }

    /* Network Layer Events */
    @SimpleEvent(description = "List of nearby devices is available; Triggered by DiscoverDevices")
    public void DevicesAvailable() {
        this.setStatus(Available);
        EventDispatcher.dispatchEvent(this, "DevicesAvailable");
    }

    @SimpleEvent(description = "This device information is available; Triggered by RequestConnectionInfo")
    public void DeviceInfoAvailable() {
        EventDispatcher.dispatchEvent(this, "DeviceInfoAvailable");
    }

    @SimpleEvent(description = "This device is available; Triggered by DiscoverDevices")
    public void AvailableToNetwork() {
        this.setStatus(Available);
        EventDispatcher.dispatchEvent(this, "AvailableNetwork");
    }

    @SimpleEvent(description = "This device is connected; Triggered by Connect")
    public void ConnectedToNetwork() {
        this.setStatus(NetworkConnected);
        this.requestConnectionInfo();
        EventDispatcher.dispatchEvent(this, "ConnectedToNetwork");
    }

    @SimpleEvent(description = "All network information is now available; Triggered next to ConnectedToNetwork")
    public void NetworkInfoAvailable() {
        this.setStatus(Registered);
        EventDispatcher.dispatchEvent(this, "NetworkInfoAvailable");
    }

    @SimpleEvent(description = "Channel is disconnected to the network; Triggered by Receiver.disconnect")
    public void DisconnectedToNetwork() {
        this.setStatus(Idle);
        if(this.IsGroupOwner()) {
            this.groupServer.stop();
        }
        this.controlClient.stop();
        EventDispatcher.dispatchEvent(this, "DisconnectedToNetwork");
    }

    /* Client Application Layer Events */
    @SimpleEvent(description = "Device connected to the Group Owner Server; Triggered by ControlClient.peerConnected")
    public void DeviceConnected(String ipAddress) {
        this.setStatus(Connected);
        EventDispatcher.dispatchEvent(this, "DeviceConnected", ipAddress);
    }

    @SimpleEvent(description = "Device reconnected to the Group Owner Server; Triggered by ControlClient.peerReconnected")
    public void DeviceReconnected() {
        this.setStatus(Registered);
        this.isReleased = false;
        EventDispatcher.dispatchEvent(this, "DeviceReconnected");
    }

    @SimpleEvent(description = "Device is now registered to the Group Owner Server; Triggered by ControlClient.peerRegistered")
    public void DeviceRegistered(int deviceId) {
        this.setStatus(Registered);
        EventDispatcher.dispatchEvent(this, "DeviceRegistered", deviceId);
    }

    @SimpleEvent(description = "Device is now inactive; ")
    public void DeviceInactive() {
        this.setStatus(Inactive);
        EventDispatcher.dispatchEvent(this, "DeviceInactive");
    }

    @SimpleEvent(description = "Device is now disconnected to the Group Owner Server ")
    public void DeviceDisconnected() {
        this.setStatus(NetworkConnected);
        this.receiver.disconnect();
        EventDispatcher.dispatchEvent(this, "DeviceDisconnected");
    }

    /* Server Application Layer Events */
    @SimpleEvent(description = "GOServer has now started and accepting connection; Triggered by StartGOServer")
    public void GoServerStarted(String ipAddress) {
        EventDispatcher.dispatchEvent(this, "GOServerStarted", ipAddress);
    }

    @SimpleEvent(description = "Connection of a peer is accepted by the Group Owner Server; Triggered by GroupServer.peerConnected")
    public void ConnectionAccepted(String ipAddress) {
        EventDispatcher.dispatchEvent(this, "ConnectionAccepted", ipAddress);
    }

    @SimpleEvent(description = "Connection of a peer is registered by the Group Owner Server; Triggered by GroupServer.peerRegistered")
    public void ConnectionRegistered(String deviceName) {
        EventDispatcher.dispatchEvent(this, "ConnectionRegistered", deviceName);
    }

    @SimpleEvent(description = "Connection of a peer is reconnected to the Group Owner Server; Triggered by GroupServer.peerReconnected")
    public void ConnectionReconnected(String deviceName) {
        EventDispatcher.dispatchEvent(this, "ConnectionReconnected", deviceName);
    }

    /* Core Events of the framework for Peer-to-Peer communication */
    @SimpleEvent(description = "Peers information is available from the Group Owner Server; Triggered by PeersChanged or RequestPeers")
    public void PeersAvailable()  {
        EventDispatcher.dispatchEvent(this, "PeersAvailable");
    }

    @SimpleEvent(description = "PeersList has changed; Triggered by ControlClient or GroupServer.register")
    public void PeersChanged() {
        EventDispatcher.dispatchEvent(this, "PeersChanged");
    }

    @SimpleEvent(description = "Data received")
    public void DataReceived(String peer, String data) {
        EventDispatcher.dispatchEvent(this, "DataReceived", peer, data);
    }

    @SimpleEvent(description = "Call request received")
    public void CallReceived(String peer) {
        EventDispatcher.dispatchEvent(this, "CallReceived", peer);
    }

    @SimpleEvent(description = "Call request accepted")
    public void CallAccepted(String peer) {
        this.startCall(WifiDirectUtil.getPeerIp(peer));
        this.startCallReceiver();
        EventDispatcher.dispatchEvent(this, "CallAccepted", peer);
    }

    @SimpleEvent(description = "Call request rejected")
    public void CallRejected(String peer) {
        this.stopCall();
        EventDispatcher.dispatchEvent(this, "CallRejected", peer);
    }

    @SimpleEvent(description = "Call ended")
    public void CallEnded() {
        EventDispatcher.dispatchEvent(this, "CallEnded");
    }

    @SimpleEvent(description = "For testing purposes only")
    public void Trigger(final String msg){
        EventDispatcher.dispatchEvent(this, "Trigger", msg);
    }

    /* Component Properties */
    @SimpleProperty(description = "Returns the representation of this device with the format" +
                    "[deviceName] deviceMACAddress",
                    category = PropertyCategory.BEHAVIOR)
    public String Device() {
        return deviceToString(this.mDevice);
    }

    @SimpleProperty(description = "Returns name of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceName() {
        if(this.mDevice != null) {
            return this.mDevice.deviceName;
        }
        return defaultDeviceName;
    }

    @SimpleProperty(description = "Returns status of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceStatus() {
        switch (this.status) {
            case Available:
                return "Available";
            case Unavailable:
                return "Unavailable";
            case Invited:
                return "Invited";
            case NetworkConnected:
                return "Network Connected";
            case Connected:
                return "Connected";
            case Registered:
                return "Registered";
            case Inactive:
                return "Inactive";
            case Failed:
                return "Failed";
            default:
                return "Unknown";
        }
    }

    @SimpleProperty(description = "Returns MAC address of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceMACAddress() {
        if(this.mDevice != null) {
            return this.mDevice.deviceAddress;
        }
        return defaultDeviceMACAddress;
    }

    @SimpleProperty(description = "Returns the IP address of the device",
                    category = PropertyCategory.BEHAVIOR)
    public String DeviceIPAddress() {
        if(this.controlClient != null) {
            return this.controlClient.getmPeer().getIpAddress();
        }
        return defaultDeviceIPAddress;
    }

    @SimpleProperty(description = "All the available devices near you",
                    category = PropertyCategory.BEHAVIOR)
    public List<String> AvailableDevices() {
        List<String> availableDevices = new ArrayList<String>();
        Collection<WifiP2pDevice> devices = this.receiver.getAvailableDevices();
        if(devices != null) {
            for (WifiP2pDevice device : devices) {
                availableDevices.add(deviceToString(device));
            }
        }
        return availableDevices;
    }

    @SimpleProperty(description = "All the available peers in the network",
                    category = PropertyCategory.BEHAVIOR)
    public List<String> AvailablePeers() {
        List<String> availablePeers = new ArrayList<String>();
        Collection<WifiDirectPeer> peers = this.controlClient.getPeers();
        if(peers != null) {
            for(WifiDirectPeer peer : peers ) {
                availablePeers.add(peer.toString());
            }
        }
        return availablePeers;
    }

    @SimpleProperty(description = "Returns the name of the group",
                    category = PropertyCategory.BEHAVIOR)
    public String GroupName() {
        if(this.mGroup != null) {
            return this.mGroup.getNetworkName();
        }
        return defaultGroupName;
    }

    @SimpleProperty(description = "Returns the Group owner's device representation",
                    category = PropertyCategory.BEHAVIOR)
    public String GroupOwner() {
        if(this.mGroupOwner != null) {
            return deviceToString(this.mGroupOwner);
        }
        return defaultDeviceName;
    }

    @SimpleProperty(description = "Returns the Group owner's host address",
                    category = PropertyCategory.BEHAVIOR)
    public String GroupOwnerAddress() {
        if(this.mConnectionInfo != null) {
            return this.mConnectionInfo.groupOwnerAddress.getHostAddress();
        }
        return defaultDeviceIPAddress;
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

    /* Component Functions */
    @SimpleFunction(description = "Connect to a certain device")
    public void Connect(String macAddress) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = macAddress;
        this.connectToDevice(config);
    }

    @SimpleFunction(description = "Reconnect to original Group Owner")
    public void Reconnect() {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = this.originalGoMac;
        this.connectToDevice(config);
    }

    @SimpleFunction(description = "Become inactive in the network")
    public void RequestInactive() {
        this.controlClient.requestInactivity();
    }

    @SimpleFunction(description = "Connect to a foreign device")
    public void ConnectToOther(String macAddress) {

    }

    @SimpleFunction(description = "Stop the client")
    public void Disconnect() {
        this.controlClient.stop();
        if(this.IsGroupOwner()) {
            this.groupServer.stop();
        }
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

    @SimpleFunction(description = "Requests the list of peers addresses from the group owner")
    public void RequestPeers() {
        if(this.status == Registered) {
            this.controlClient.requestPeers();
        }
    }

    @SimpleFunction(description = "Broadcast string to the network")
    public void SendData(String msg) {
        this.controlClient.sendMessage(msg);
    }

    @SimpleFunction(description = "Send a call request to another peer")
    public void CallPeer(int peerId) {
        this.controlClient.requestCall(peerId);
        this.startCallReceiver();
    }

    @SimpleFunction(description = "Accept a call from a peer")
    public void AcceptCall(int peerId) {
        WifiDirectPeer callee = this.controlClient.getPeerById(peerId);
        this.controlClient.acceptCall(peerId);
        this.startCallReceiver();
        this.startCall(callee.getIpAddress());
    }

    @SimpleFunction(description = "Reject a call from a peer")
    public void RejectCall(int peerId) {
        this.controlClient.rejectCall(peerId);
        this.stopCallReceiver();
    }

    @SimpleFunction(description = "End Call")
    public void EndCall() {
        this.stopCallReceiver();
        this.stopCall();
    }

    @Override
    public void onDelete() {

    }

    @Override
    public void onDestroy() {

    }

    public void initializeChannel() {
        this.manager = (WifiP2pManager) form.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(form, form.getMainLooper(), null);
        this.receiver = new WifiDirectBroadcastReceiver(this.manager, this.channel, this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        form.registerReceiver(this.receiver, intentFilter);
    }

    public void connectToDevice(WifiP2pConfig config) {
        this.manager.connect(this.channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });
    }

    public void requestConnectionInfo(){
        this.manager.requestConnectionInfo(this.channel, this.receiver);
    }

    public void connectionInfoAvailable() {
        this.manager.requestGroupInfo(this.channel, this.receiver);
    }

    public void groupInfoAvailable() {
        this.NetworkInfoAvailable();
        if(this.isReleased) {
            this.reconnectClient();
        }
        else {
            if(this.mConnectionInfo.isGroupOwner) {
                if(this.groupServer == null || !this.groupServer.isAccepting) {
                    this.startGoServer();
                }
            }

            if(this.controlClient == null || !this.controlClient.isRunning) {
                this.startClient(groupServerPort);
            }
        }
    }

    public void startGoServer() {
        if(this.IsGroupOwner()) {
            try {
                this.groupServer = new WifiDirectGroupServer(this,
                                                             this.mConnectionInfo.groupOwnerAddress,
                                                             groupServerPort);
                this.groupServer.setHandler(this.handler);
                AsynchUtil.runAsynchronously(this.groupServer);
            } catch (IOException e) {
                wifiDirectError("startGoServer",
                        ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                        e.getMessage());
            }
        }
    }

    public void startClient(int port) {
        try {
            this.controlClient = new WifiDirectControlClient(this, this.mConnectionInfo.groupOwnerAddress, port);
            this.controlClient.setHandler(this.handler);
            this.controlClient.setmPeer(new WifiDirectPeer(this.mDevice, userServerPort));
            AsynchUtil.runAsynchronously(this.controlClient);
        } catch (Exception e) {
            wifiDirectError("startClient",
                    ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                    e.getMessage());
        }
    }

    public void reconnectClient() {
        WifiDirectPeer peer = this.controlClient.getmPeer();
        WifiDirectPeer newPeer = new WifiDirectPeer(this.mDevice, userServerPort);
        newPeer.setId(peer.getId());
        newPeer.setIpAddress(peer.getIpAddress());
        newPeer.setStatus(WifiDirectPeer.PEER_STATUS_INACTIVE);
        this.controlClient.setmPeer(newPeer);
        this.controlClient.reinitialize();
        AsynchUtil.runAsynchronously(this.controlClient);
    }

    public void startCallReceiver() {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                AudioTrack track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, callFreqRate,
                                                  AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                                                  callBufferSize, AudioTrack.MODE_STREAM);
                track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
                track.play();

                try {
                    DatagramSocket socket = new DatagramSocket(callReceiverPort);
                    byte[] buf = new byte[callBufferSize];

                    WifiDirectP2P.this.isCalling = true;
                    while(WifiDirectP2P.this.isCalling) {
                        DatagramPacket packet = new DatagramPacket(buf, callBufferSize);
                        socket.receive(packet);
                        track.write(packet.getData(), 0, packet.getLength());
                    }

                    track.release();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopCallReceiver() {
        this.isCalling = false;
    }

    public void startCall(final String peerAddress) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                int minBufSize = AudioRecord.getMinBufferSize(callFreqRate,
                                                              AudioFormat.CHANNEL_IN_MONO,
                                                              AudioFormat.ENCODING_PCM_16BIT) * 10;

                AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                                                            callFreqRate,
                                                            AudioFormat.CHANNEL_IN_MONO,
                                                            AudioFormat.ENCODING_PCM_16BIT,
                                                            minBufSize);

                byte[] buf = new byte[callBufferSize];
                int bytes_read;
                try {
                    InetAddress address = InetAddress.getByName(peerAddress);
                    DatagramSocket socket = new DatagramSocket();
                    audioRecorder.startRecording();

                    WifiDirectP2P.this.isCalling = true;
                    while(WifiDirectP2P.this.isCalling) {
                        bytes_read = audioRecorder.read(buf, 0, callBufferSize);
                        if(bytes_read  > 0) {
                            DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, callReceiverPort);
                            socket.send(packet);
                            Thread.sleep(callVoiceInterval, 0);
                        }
                    }
                    audioRecorder.stop();
                    audioRecorder.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopCall() {
        this.isCalling = false;
    }

    public void reconnectedToNetwork() {
        this.requestConnectionInfo();
    }

    public void temporaryDisconnect() {
        this.isReleased = true;
        this.originalGoMac = this.mGroupOwner.deviceAddress;
        this.receiver.disconnect();
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isReleased() {
        return this.isReleased;
    }

    public void wifiDirectError(String functionName, int errorCode, Object... args) {
        this.form.dispatchErrorOccurredEvent(this, functionName, errorCode, args);
    }
}
