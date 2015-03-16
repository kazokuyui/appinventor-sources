package com.google.appinventor.components.runtime;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.WifiDirectUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

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

    private ServerSocket serverSocket;
    private int serverPort;
    private Collection<Socket> clients;
    private boolean isAccepting;

    protected WifiDirectGroupOwner(ComponentContainer container) {
        super(container, "WifiDirectGroupOwner");
        this.serverPort = WifiDirectUtil.defaultServerPort;
        this.clients = new ArrayList<Socket>();
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
    public boolean AcceptingConnection() {
        return this.isAccepting;
    }

    @SimpleFunction(description = "Start accepting new connections")
    public void AcceptConnection() {
        this.isAccepting = true;

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket socket = new ServerSocket(WifiDirectGroupOwner.this.serverPort);
                    WifiDirectGroupOwner.this.setServerSocket(socket);

                    while (isAccepting) {
                        Socket client = socket.accept();
                        addPeer(client);
                        ConnectionAccepted(client.getRemoteSocketAddress().toString());
                        DataInputStream in = new DataInputStream(client.getInputStream());
                        DataOutputStream out = new DataOutputStream(client.getOutputStream());
                        if(in.readUTF().equals(WifiDirectUtil.REGISTER_TO_NETWORK)) {
                            out.writeUTF(WifiDirectUtil.REGISTRATION_ACCEPTED);
                        }
                    }
                } catch (IOException e) {
                    wifiDirectError("AcceptConnection",
                            ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                            e.getMessage());
                }
            }
        });

    }

    @SimpleFunction(description = "Stop accepting new connection")
    public void StopAcceptingConnection(){
        this.isAccepting = false;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            wifiDirectError("AcceptConnection",
                    ErrorMessages.ERROR_WIFIDIRECT_UNABLE_TO_READ,
                    e.getMessage());
        }
    }

    @SimpleFunction(description = "Request list of peers")
    public void RequestPeers(){
        this.manager.requestGroupInfo(this.channel, (WifiP2pManager.GroupInfoListener) this.receiver);
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

    public void addPeer(Socket peer) {
        this.clients.add(peer);
    }

    public boolean removePeer(Socket peer) {
        return this.clients.remove(peer);
    }

    public void setServerSocket(ServerSocket socket) {
        this.serverSocket = socket;
    }
}
