
package com.kaltura.playkit.plugins.ads.kaltura.events;

import com.kaltura.playkit.PKEvent;

public class AdPluginErrorEvent implements PKEvent {

    public AdErrorEvent.Type type;

    public AdPluginErrorEvent(AdErrorEvent.Type type) {
        this.type = type;
    }

    public static class AdErrorEvent extends AdPluginErrorEvent {

        public final com.kaltura.admanager.AdErrorEvent adErrorEvent;
        public final String adErrorMessage;

        public AdErrorEvent(com.kaltura.admanager.AdErrorEvent adErrorEvent, String adErrorMessage) {
            super(Type.AD_ERROR);
            this.adErrorEvent = adErrorEvent;
            this.adErrorMessage = adErrorMessage;
        }
    }

    public enum Type {
        AD_ERROR
    }

    @Override
    public Enum eventType() {
        return this.type;
    }
}
