package com.kaltura.playkit.plugins.playback;

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;

public class KalturaUDRMLicenseRequestAdapter implements PKRequestParams.Adapter {

    private final String applicationName;

    public static void install(Player player, String applicationName) {
        KalturaUDRMLicenseRequestAdapter decorator = new KalturaUDRMLicenseRequestAdapter(applicationName, player);
        player.getSettings().setLicenseRequestAdapter(decorator);
    }

    private KalturaUDRMLicenseRequestAdapter(String applicationName, Player player) {
        this.applicationName = applicationName;
    }

    @Override
    public PKRequestParams adapt(PKRequestParams requestParams) {
        requestParams.headers.put("Referrer", applicationName);
        return requestParams;
    }

    @Override
    public void updateParams(Player player) {
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }
}
