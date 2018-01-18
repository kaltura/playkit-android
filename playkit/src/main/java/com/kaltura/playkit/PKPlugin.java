/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.content.Context;

import com.google.gson.JsonObject;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        String getVersion();
        
        PKPlugin newInstance();
        void warmUp(Context context);
        
        Object mergeConfig(Object base, JsonObject additions);
//        Object resolveConfig(Object base, JsonObject additions, TokenResolver resolver);
    }

    protected abstract void onLoad(Context context, Player player, Object config, MessageBus messageBus, TokenResolver tokenResolver);
    protected abstract void onUpdateMedia(PKMediaConfig mediaConfig);
    protected abstract void onUpdateConfig(Object config);
    protected abstract void onApplicationPaused();
    protected abstract void onApplicationResumed();

    protected abstract void onDestroy();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }
}
