package com.google.appinventor.components.runtime;

/**
 * RSP Handler for WifiDirectP2P Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */

public class WifiDirectRSPHandler {
    private byte[] rsp = null;

    public synchronized boolean handleResponse(byte[] rsp) {
        this.rsp = rsp;
        this.notify();
        return true;
    }

    public synchronized void waitForResponse() {
        while(this.rsp == null) {
            try {
                this.wait();
            } catch (InterruptedException ignored) {

            }
        }
    }
}
