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
    private String name;
    private String macAddress;
    private String ipAddress;
    private int port;

    public WifiDirectPeer(String name, String macAddress, String ipAddress, int port) {
        this.name = name;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public WifiDirectPeer(WifiP2pDevice device) {
        this.name = device.deviceName;
        this.macAddress = device.deviceAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
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

    public String toString() {
        return this.name+"@"+this.ipAddress;
    }
}
