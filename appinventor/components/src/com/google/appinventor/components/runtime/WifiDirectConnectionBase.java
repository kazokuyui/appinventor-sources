package com.google.appinventor.components.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static android.net.wifi.p2p.WifiP2pManager.*;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 */
@SimpleObject
public abstract class WifiDirectConnectionBase extends AndroidNonvisibleComponent implements
        Component, Deleteable, OnDestroyListener {

    protected final String logTag;
    protected WifiP2pManager manager;
    protected Channel channel;
    protected BroadcastReceiver receiver;
    protected final IntentFilter intentFilter = new IntentFilter();
    protected String encoding;

    protected WifiDirectConnectionBase(ComponentContainer container, String logTag) {
        this(container.$form(), logTag);
        form.registerForOnDestroy(this);
    }

    private WifiDirectConnectionBase(Form form, String logTag) {
        super(form);
        this.logTag = logTag;
        this.manager = (WifiP2pManager) form.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(form, form.getMainLooper(), null);
        this.receiver = new WifiDirectBroadcastReceiver(this.manager, this.channel, this);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        form.registerReceiver(this.receiver, null);
        CharacterEncoding("UTF-8");
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
            defaultValue = "UTF-8")
    @SimpleProperty
    public void CharacterEncoding(String encoding) {
        try {
            "check".getBytes(encoding);
            this.encoding = encoding;
        }catch (UnsupportedEncodingException ignored) {

        }
    }

    @SimpleProperty(description = "Whether WiFi Direct is available on the device",
            category = PropertyCategory.BEHAVIOR)
    public boolean Available() {
        return true;
    }

    @SimpleProperty(description = "Whether WiFi Direct is enabled on the device",
            category = PropertyCategory.BEHAVIOR)
    public boolean Enabled() {
        return true;
    }

    @SimpleProperty(description = "Whether the device is connected to another device",
            category = PropertyCategory.BEHAVIOR)
    public boolean IsConnected() {
        return true;
    }

    public void Disconnect() {

    }

    @Override
    public void onDelete(){

    }

    @Override
    public void onDestroy(){

    }
}
