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
    public static final int defaultTimeout = 3000;

    /* Control Plane Values */
    public static final int groupServerPort = 3000;
    public static final int userServerPort = 4444;
    public static final int controlBufferSize = 8192;
    public static boolean SSL = System.getProperty("ssl") != null;

    /* VoIP Values */
    public static final int callReceiverPort = 4545;
    public static final int callFreqRate = 8000;
    public static final int callVoiceInterval = 20;
    public static final int callVoiceByteSize = 2;
    public static final int callBufferSize = callVoiceInterval*callVoiceInterval*callVoiceByteSize*2;

    public static String deviceToString(WifiP2pDevice device) {
        return "[" + device.deviceName + "] " + device.deviceAddress;
    }
}
