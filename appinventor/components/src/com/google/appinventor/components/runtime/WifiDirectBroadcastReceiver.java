package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.*;

import java.util.ArrayList;
import java.util.Collection;

import static android.net.wifi.p2p.WifiP2pManager.*;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver implements
        ConnectionInfoListener, PeerListListener, ChannelListener, GroupInfoListener {

    private WifiP2pManager manager;
    private Channel channel;
    private WifiDirectClient client;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       WifiDirectClient client) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.client = client;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, this);
            }
        }
        else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.
                    getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                this.client.DeviceConnected();
            }

            this.client.setIsConnected(networkInfo.isConnected());
            this.client.setIsAvailable(networkInfo.isAvailable());
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        this.client.setConnectionInfo(wifiP2pInfo);
        this.client.ConnectionInfoAvailable();
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        this.client.setAvailableDevices(wifiP2pDeviceList);
        this.client.DevicesAvailable();
    }

    @Override
    public void onChannelDisconnected() {

    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        Collection<WifiP2pDevice> list = new ArrayList<WifiP2pDevice>();
        list.addAll(wifiP2pGroup.getClientList()); //siblings
        if(!wifiP2pGroup.isGroupOwner()){
            this.client.setGroupOwner(wifiP2pGroup.getOwner());
        }
        else{
            WifiP2pDevice me = new WifiP2pDevice(wifiP2pGroup.getOwner());
            if(me.deviceName.isEmpty()){
                me.deviceName = "Me";
            }
            this.client.setGroupOwner(me);
        }

        this.client.setPeers(list);
        this.client.PeersAvailable();
    }
}