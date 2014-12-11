package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;

/**
 * @author nmcalabroso@up.edu.ph (neil)
 */
@DesignerComponent(version = YaVersion.WIFIDIRECTCLIENT_COMPONENT_VERSION,
        designerHelpDescription = "Wifi Direct component",
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

    /**
     * Creates a new AndroidNonvisibleComponent.
     *
     * @param form the container that this component will be placed in
     */
    protected WifiDirectClient(Form form) {
        super(form);
    }

    @Override
    public void onDelete() {

    }

    @Override
    public void onDestroy() {

    }
}
