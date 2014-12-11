package com.google.appinventor.components.runtime;

import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.net.wifi.p2p.WifiP2pManager.*;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 */
@DesignerComponent(version = YaVersion.WIFIDIRECTCLIENT_COMPONENT_VERSION,
        designerHelpDescription = "Wifi Direct Client Component",
        category = ComponentCategory.CONNECTIVITY,
        nonVisible = true,
        iconName = "images/wifiDirect.png")
@SimpleObject
@UsesPermissions(permissionNames =
                 "android.permission.ACCESS_WIFI_STATE, " +
                 "android.permission.CHANGE_WIFI_STATE, " +
                 "android.permission.CHANGE_NETWORK_STATE, " +
                 "android.permission.ACCESS_NETWORK_STATE, " +
                 "android.permission.CHANGE_WIFI_MULTICAST_STATE")

public final class WifiDirectClient extends WifiDirectConnectionBase {

    private List<Component> attachedComponents = new ArrayList<Component>();
    private WifiP2pDeviceList devices = new WifiP2pDeviceList();
    private Set<Integer> acceptableDeviceClasses;

    public WifiDirectClient(ComponentContainer container) {
        super(container, "WifiDirectClient");
    }

    @SimpleFunction(description = "Scans all devices nearby")
    public void ScanDevices() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                return;
            }

            @Override
            public void onFailure(int reasonCode) {
                return;
            }
        });
    }

    @SimpleEvent(description = "List of nearby devices is available")
    public void DevicesAvailable(){
        EventDispatcher.dispatchEvent(this, "DevicesAvailable");
    }

    @SimpleFunction(description = "All the available devices near you")
    public List<String> Devices() {
        List<String> availableDevices = new ArrayList<String>();

        if(this.devices != null) {
            for (WifiP2pDevice device : this.devices.getDeviceList()) {
                if (device.deviceName != null && device.deviceAddress != null) {
                    availableDevices.add(device.deviceName + "#" + device.deviceAddress);
                }
            }
        }
        return availableDevices;
    }

    public void setDevices(WifiP2pDeviceList devices){
        this.devices = devices;
    }

}
