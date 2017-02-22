package com.kaltura.playkit;

/**
 * Player configuration data holder.
 * This object is used by the player in order to preset desired configurations.
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 * @deprecated This class was broken into distinct classes, {@link PKMediaConfig} and {@link PKPluginSettings}.
 */
@Deprecated
public class PlayerConfig {

    public final PKMediaConfig media = new PKMediaConfig();
    public final PKPluginSettings plugins = new PKPluginSettings();

}
