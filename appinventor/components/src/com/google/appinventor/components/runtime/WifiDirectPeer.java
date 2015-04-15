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
    private ArrayList<Integer> ports;

    public WifiDirectPeer(String name, String macAddress, String ipAddress, ArrayList<Integer> ports) {
        this.name = name;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.ports = (ArrayList<Integer>) ports.clone();
    }

    public WifiDirectPeer(WifiP2pDevice device) {
        this.name = device.deviceName;
        this.macAddress = device.deviceAddress;
    }

    public void addPort(int newPort) {
        this.ports.add(newPort);
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

    public ArrayList<Integer> getPorts() {
        return ports;
    }

    public void setPorts(ArrayList<Integer> ports) {
        this.ports = ports;
    }

    public String toString() {
        return this.name+"@"+this.ipAddress;
    }
}
