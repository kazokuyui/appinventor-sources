package com.google.appinventor.components.runtime.util;

import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Utility class for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class WifiDirectUtil {
    public static final String defaultDeviceName = "MyDevice";
    public static final String defaultDeviceMACAddress = "Unknown";
    public static final String defaultDeviceIPAddress = "0.0.0.0";
    public static final String defaultGroupName = "MyP2PGroup";
    public static final int defaultServerPort = 4545;
    public static final int defaultGroupServerPort = 3000;
    public static final int defaultBufferSize = 1024;
    public static final int defaultTimeOut = 3000;
    public static boolean SSL = System.getProperty("ssl") != null;

    public static String deviceToString(WifiP2pDevice device) {
        return "[" + device.deviceName + "] " + device.deviceAddress;
    }

    public static String getDeviceStatus(WifiP2pDevice device){
        int status = device.status;
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown: " + Integer.toString(status);
        }
    }

    public static String getDeviceFormat() {
        return "[deviceName] deviceMACAddress";
    }

}
