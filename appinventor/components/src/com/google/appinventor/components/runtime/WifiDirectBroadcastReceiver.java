package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.*;

import java.util.Collection;

import static android.net.wifi.p2p.WifiP2pManager.*;

/**
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver implements
        ConnectionInfoListener, PeerListListener, ChannelListener, GroupInfoListener {

    private WifiP2pManager manager;
    private Channel channel;
    private WifiDirectP2P main;
    private Collection<WifiP2pDevice> availableDevices;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, WifiDirectP2P main) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.main = main;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, this);
            }
        }
        else if(WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            if (manager == null) return;
            NetworkInfo networkInfo = intent.getParcelableExtra(EXTRA_NETWORK_INFO);

            if (networkInfo.isAvailable()) {
                this.main.AvailableToNetwork();
            }

            if (networkInfo.isConnected()) {
                if(this.main.isReconnect()) {
                    this.main.reconnectedToNetwork();
                }
                else if(this.main.isGateway()) {
                    this.main.connectedAsGateway();
                }
                else {
                    this.main.ConnectedToNetwork();
                }
            }
        }
        else if(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            this.main.setDevice((WifiP2pDevice) intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE));
            this.main.DeviceInfoAvailable();
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        this.main.setConnectionInfo(wifiP2pInfo);
        this.main.connectionInfoAvailable();
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        this.setAvailableDevices(wifiP2pDeviceList);
        this.main.DevicesAvailable();
    }

    @Override
    public void onChannelDisconnected() {
        this.main.DisconnectedToNetwork();
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        this.main.setP2PGroup(wifiP2pGroup);
        this.main.groupInfoAvailable();
    }

    public Collection<WifiP2pDevice> getAvailableDevices() {
        return availableDevices;
    }

    public void setAvailableDevices(WifiP2pDeviceList availableDevices) {
        this.availableDevices = availableDevices.getDeviceList();
    }

    public void disconnect() {
        if (this.manager != null && this.channel != null) {
            manager.removeGroup(this.channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    if(WifiDirectBroadcastReceiver.this.main.isReleased()) {
                        WifiDirectBroadcastReceiver.this.main.DeviceInactive();
                    }
                    else {
                        WifiDirectBroadcastReceiver.this.main.DisconnectedToNetwork();
                    }
                }

                @Override
                public void onFailure(int i) {

                }
            });
        }
    }
}
