package com.google.appinventor.components.runtime.util;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Utility class for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class WifiDirectUtil {
    /* Default Values */
    public static final String defaultDeviceName = "MyDevice";
    public static final String defaultDeviceMACAddress = "Unknown";
    public static final String defaultDeviceIPAddress = "0.0.0.0";
    public static final String defaultGroupName = "MyP2PGroup";
    public static final int defaultServerPort = 4545;
    public static final int groupServerPort = 3000;
    public static final int controlBufferSize = 8192;

    public static boolean SSL = System.getProperty("ssl") != null;

    public static String deviceToString(WifiP2pDevice device) {
        return "[" + device.deviceName + "] " + device.deviceAddress;
    }
}
