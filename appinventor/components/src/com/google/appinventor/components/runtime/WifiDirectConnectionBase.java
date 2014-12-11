package com.google.appinventor.components.runtime;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 */
public abstract class WifiDirectConnectionBase extends AndroidNonvisibleComponent implements
        Component, Deleteable, OnDestroyListener {
    /**
     * Creates a new AndroidNonvisibleComponent.
     *
     * @param form the container that this component will be placed in
     */
    protected WifiDirectConnectionBase(Form form) {
        super(form);
    }
}
