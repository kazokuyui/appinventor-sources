package com.google.appinventor.components.runtime.util;

/**
 * PeerMessage for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class PeerMessage {
    public static final int USER_DATA = 1001;
    public static final int CONTROL_DATA = 1002;
    public static final String CTRL_CONNECTED = "CONNECTED";
    public static final String CTRL_REGISTER = "REGISTER";
    public static final String CTRL_RECONNECT = "RECONNECT";
    public static final String CTRL_RECONNECTED = "RECONNECTED";
    public static final String CTRL_REGISTERED = "REGISTERED";
    public static final String CTRL_REQUEST_PEER = "REQUEST_PEER";
    public static final String CTRL_PEERS_LIST = "PEERS_LIST";
    public static final String CTRL_PEERS_CHANGE = "PEERS_CHANGE";
    public static final String CTRL_REQUEST_CALL = "CALL_REQUEST";
    public static final String CTRL_ACCEPT_CALL = "CALL_ACCEPT";
    public static final String CTRL_REJECT_CALL = "CALL_REJECT";
    public static final String CTRL_REQUEST_INACTIVITY = "REQUEST_INACTIVITY";
    public static final String CTRL_ACCEPT_INACTIVITY = "ACCEPT_INACTIVITY";
    public static final String CTRL_QUIT = "QUIT";
    public static final String USR_BROADCAST = "BROADCAST";
    public static final String USR_MESSAGE = "MESSAGE";

    private int type;
    private String header;
    private String data;

    public PeerMessage(int type, String header, String data) {
        this.type = type;
        this.header = header;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String toString() {
        return this.getType() + "/" + this.getHeader() + "/" + this.getData() + "\r\n";
    }
}
