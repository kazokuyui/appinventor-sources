package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import static android.net.wifi.p2p.WifiP2pManager.*;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver implements
        ConnectionInfoListener, PeerListListener {

    private WifiP2pManager manager;
    private Channel channel;
    private WifiDirectConnectionBase base;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       WifiDirectConnectionBase base) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.base = base;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, this);
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        WifiDirectClient client = (WifiDirectClient) this.base;
        client.setDevices(wifiP2pDeviceList);
        client.DevicesAvailable();
    }
}
