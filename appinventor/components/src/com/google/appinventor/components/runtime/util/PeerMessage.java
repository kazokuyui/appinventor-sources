package com.google.appinventor.components.runtime.util;

/**
 * PeerMessage for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class PeerMessage {
    public static final int PEER_DATA = 1001;
    public static final int CONTROL_DATA = 1002;
    public static final String CTRL_CONNECTED = "CONNECTED";
    public static final String CTRL_REGISTERED = "REGISTERED";

    private int type;
    private String data;

    public PeerMessage(int type, String data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String toString() {
        return this.type + "/" + this.data + "\r\n";
    }
}
