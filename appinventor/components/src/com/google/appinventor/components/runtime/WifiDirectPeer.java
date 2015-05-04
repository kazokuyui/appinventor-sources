package com.google.appinventor.components.runtime;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.ArrayList;

/**
 * Peer Object for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectPeer {
    private int id;
    private String name;
    private String macAddress;
    private String ipAddress;
    private int port;

    public WifiDirectPeer(String rawPeer) {
        String[] parts = rawPeer.split("@");
        String[] name_id = parts[0].split(":");
        String[] ip_port = parts[2].split(":");

        this.name = name_id[0];
        this.id = Integer.parseInt(name_id[1]);
        this.macAddress = parts[1];
        this.ipAddress = ip_port[0];
        this.port = Integer.parseInt(ip_port[1]);
    }

    public WifiDirectPeer(WifiP2pDevice device, int port) {
        this.name = device.deviceName;
        this.macAddress = device.deviceAddress;
        this.port = port;
        this.id = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        return this.name+":"+this.id+"@"+this.macAddress+"@"+this.ipAddress+":"+this.port;
    }
}
