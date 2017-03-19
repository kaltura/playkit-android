package com.kaltura.playkit;

/**
 * Player configuration data holder.
 * This object is used by the player in order to preset desired configurations.
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 * @deprecated PlayerConfig.Media and PlayerConfig.Plugins are now {@link PKMediaConfig} and 
 * {@link PKPluginConfigs}, respectively. This class remains to ease the migration, and will be removed.
 */
@Deprecated
public class PlayerConfig {

    public final PKMediaConfig media = new PKMediaConfig();
    public final PKPluginConfigs plugins = new PKPluginConfigs();

}
