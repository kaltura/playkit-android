package com.kaltura.playkit.plugins.ads.kaltura.events;

import com.kaltura.admanager.AdInfo;
import com.kaltura.playkit.PKEvent;



public class AdPluginEvent implements PKEvent {

    public AdPluginEvent.Type type;

    public AdPluginEvent(AdPluginEvent.Type type) {
        this.type = type;
    }

    public AdPluginEvent(AdPluginEvent.Type type, String message) {
        this.type = type;
    }

    public static class UpdateUIEvent extends AdPluginEvent {

        public boolean show;
        public int uiType;
        public float cuePoint;

        public UpdateUIEvent(boolean show, int type, float cuePoint) {
            super(Type.UPDATE_UI);
            this.show = show;
            this.uiType = type;
            this.cuePoint = cuePoint;
        }
    }

    public static class ProgressUpdateEvent extends AdPluginEvent {

        public long currentPosition;
        public long duration;

        public ProgressUpdateEvent(long currentPosition, long duration) {
            super(Type.AD_PROGRESS_UPDATE);
            this.currentPosition = currentPosition;
            this.duration = duration;
        }
    }

    public static class AdRequestedEvent extends AdPluginEvent {

        public String adTagUrl;

        public AdRequestedEvent(String adTagUrl) {
            super(Type.AD_REQUESTED);
            this.adTagUrl = adTagUrl;
        }
    }

    public static class AdLoadedEvent extends AdPluginEvent {

        public AdInfo adInfo;

        public AdLoadedEvent(AdInfo adInfo) {
            super(Type.LOADED);
            this.adInfo = adInfo;
        }
    }

    public static class AdvtClickEvent extends AdPluginEvent {

        public String advtLink;

        public AdvtClickEvent(String advtLink) {
            super(Type.CLICKED);
            this.advtLink = advtLink;
        }
    }

    public static class AdCountEvent extends AdPluginEvent {

        public int adIndex;
        public int totalAdCount;

        public AdCountEvent(int adIndex, int totalAdCount) {
            super(Type.AD_COUNT);
            this.adIndex = adIndex;
            this.totalAdCount = totalAdCount;
        }
    }


//    public static class InitCompletedEvent extends AdPluginEvent {
//
//        public MessageBus messageBus;
//
//        public InitCompletedEvent(MessageBus messageBus) {
//            super(Type.INIT);
//            this.messageBus = messageBus;
//        }
//    }

    public static class PlayBackUpdateEvent extends AdPluginEvent {

        public float duration;

        public PlayBackUpdateEvent(float duration) {
            super(Type.PLAYBACK_UPDATE);
            this.duration = duration;
        }
    }

    public static class AdBufferEvent extends AdPluginEvent {

        public boolean show;

        public AdBufferEvent(boolean show) {
            super(Type.AD_BUFFERING);
            this.show = show;
        }
    }

//    public static class ReplayEvent extends AdPluginEvent {
//
//        public boolean isAppInBackground;
//
//        public ReplayEvent(boolean isAppInBackground) {
//            super(Type.REPLAY);
//            this.isAppInBackground = isAppInBackground;
//        }
//    }

//    public static class AudioFocusEvent extends AdPluginEvent {
//
//        public boolean isAudioFocusGained;
//
//        public AudioFocusEvent(boolean isAudioFocusGained) {
//            super(Type.AUDIO_FOCUS);
//            this.isAudioFocusGained = isAudioFocusGained;
//        }
//    }
//
//    public static class ApplicationResumeEvent extends AdPluginEvent {
//        public boolean isPopupPlayerPause;
//
//        public ApplicationResumeEvent(boolean isPopupPlayerPause) {
//            super(Type.CUSTOM_APP_RESUME);
//            this.isPopupPlayerPause = isPopupPlayerPause;
//        }
//    }


    public enum Type {
        UPDATE_UI,
        ATTRIBUTION_UPDATE,
        AD_PROGRESS_UPDATE,
        END_AD_PLAYBACK_CONTROL,
        PLAYBACK_STATE,
        AD_REQUESTED,
        LOADED,
        STARTED,
        PAUSED,
        RESUMED,
        COMPLETED,
        SKIPPED,
        CLICKED,
        POSTROLL_AVAILABLE,
        POSTROLL_COMPLETED,
        AD_COUNT,
        PLAYER_SWITCH_MAXIMIZE,
        PLAYER_STATE,
        INIT,
        PLAYBACK_UPDATE,
        AD_BUFFERING,
//        REPLAY,
//        REMOVE_AD_ON_CAST,
//        AUDIO_FOCUS,
//        CUSTOM_APP_PAUSE,
//        CUSTOM_APP_RESUME
    }


    @Override
    public Enum eventType() {
        return this.type;
    }
}
