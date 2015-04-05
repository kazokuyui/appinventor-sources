package com.google.appinventor.components.runtime;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
/**
 * Data Worker for WifiDirect Component
 *
 * @author nmcalabroso@up.edu.ph (neil)
 * @author erbunao@up.edu.ph (earle)
 */
public class WifiDirectWorker implements Runnable {
    private final List<WifiDirectDataEvent> queue = new LinkedList<WifiDirectDataEvent>();

    public void processData(WifiDirectServer server, SocketChannel socket, byte[] data, int count) {
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        synchronized(queue) {
            queue.add(new WifiDirectDataEvent(server, socket, dataCopy));
            queue.notify();
        }
    }

    public void run() {
        WifiDirectDataEvent dataEvent;

        while(true) {
            synchronized(queue) {
                while(queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException ignored) {

                    }
                }
                dataEvent = queue.remove(0);
            }

            dataEvent.server.send(dataEvent.socket, dataEvent.data);
        }
    }
}
