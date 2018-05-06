package com.kaltura.playkit.plugins.youbora;

import com.npaw.youbora.lib6.adapter.PlayerAdapter;
import com.npaw.youbora.lib6.plugin.Options;
import com.npaw.youbora.lib6.plugin.Plugin;

/**
 * Created by gilad.nadav on 06/05/2018.
 */

public class NPAWPlugin extends Plugin {

    public NPAWPlugin(Options options) {
        super(options);
    }

    public NPAWPlugin(Options options, PlayerAdapter adapter) {
        super(options, adapter);
    }
}
