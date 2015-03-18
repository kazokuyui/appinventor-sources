package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.*;

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
    private WifiDirectBase main;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       WifiDirectBase main) {
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
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                this.main.DeviceConnected();
            }

            this.main.setIsConnected(networkInfo.isConnected());
            this.main.setIsAvailable(networkInfo.isAvailable());
        }
        else if(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            this.main.setDevice((WifiP2pDevice) intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE));
            this.main.DeviceInfoAvailable();
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        this.main.setConnectionInfo(wifiP2pInfo);
        this.main.ConnectionInfoAvailable();
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        this.main.setAvailableDevices(wifiP2pDeviceList);
        this.main.DevicesAvailable();
    }

    @Override
    public void onChannelDisconnected() {
        this.main.ChannelDisconnected();
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        this.main.setP2PGroup(wifiP2pGroup);
        this.main.GroupInfoAvailable();
    }
}
