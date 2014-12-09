package com.google.appinventor.components.runtime.util;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Utility class for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 */
public class WifiDirectUtil {
    public static final String REGISTER_TO_NETWORK = "registerToNetwork";
    public static final String REGISTRATION_ACCEPTED = "registrationAccepted";

    public static final String defaultDeviceName = "MyDevice";
    public static final String defaultDeviceAddress = "Unknown";
    public static final int defaultServerPort = 4545;
    public static final int defaultBufferSize = 1024;

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
