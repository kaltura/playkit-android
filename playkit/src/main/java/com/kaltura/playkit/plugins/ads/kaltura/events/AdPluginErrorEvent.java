
package com.kaltura.playkit.plugins.ads.kaltura.events;

import com.kaltura.admanager.AdEvent;
import com.kaltura.playkit.PKEvent;

public class AdPluginErrorEvent implements PKEvent {

    public AdPluginErrorEvent.Type type;

    public AdPluginErrorEvent(AdPluginErrorEvent.Type type) {
        this.type = type;
    }

    public AdPluginErrorEvent(AdPluginErrorEvent.Type type, String message) {
        this.type = type;
    }

    public static class AdErrorEvent extends AdPluginErrorEvent {
        public AdEvent.AdErrorEventType errorEvent;
        public String adErrorMessage;
        public Throwable ex;

        public AdErrorEvent(AdEvent.AdErrorEventType errorEvent, String adErrorMessage, Throwable ex) {
            super(Type.AD_ERROR);
            this.errorEvent = errorEvent;
            this.adErrorMessage = adErrorMessage;
            this.ex = ex;
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
