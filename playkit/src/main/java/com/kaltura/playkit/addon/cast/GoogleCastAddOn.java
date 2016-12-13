package com.kaltura.playkit.addon.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.TextTrackStyle;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by itanbarpeled on 07/12/2016.
 */
public class GoogleCastAddOn {


    private static final String MOCK_DATA = "MOCK_DATA";


    public static class Builder {


        private KCastInfo mKCastInfo;


        public Builder() {
            mKCastInfo = new KCastInfo();
        }

        public Builder setFormat(String format) {
            mKCastInfo.setFormat(format);
            return this;
        }

        public Builder setInitObject(String initObject) {
            mKCastInfo.setInitObject(initObject);
            return this;
        }

        public Builder setKs(String ks) {
            mKCastInfo.setKs(ks);
            return this;
        }

        public Builder setPartnerId(int partnerId) {
            mKCastInfo.setPartnerId(partnerId);
            return this;
        }


        public Builder setUiConfId(String uiConfId) {
            mKCastInfo.setUiConfId(uiConfId);
            return this;
        }


        public Builder setAdTagUrl(String adTagUrl) {
            mKCastInfo.setAdTagUrl(adTagUrl);
            return this;
        }

        public Builder setMediaEntryId(String mediaEntryId) {
            mKCastInfo.setMediaEntryId(mediaEntryId);
            return this;
        }

        public Builder setMetadata(MediaMetadata mediaMetadata) {
            mKCastInfo.setMetadata(mediaMetadata);
            return this;
        }

        public Builder setMediaTrackList(List<MediaTrack> mediaTrackList) {
            mKCastInfo.setMediaTrackList(mediaTrackList);
            return this;
        }

        public Builder setTextTrackStyle(TextTrackStyle textTrackStyle) {
            mKCastInfo.setTextTrackStyle(textTrackStyle);
            return this;
        }


        public Builder setMwEmbedUrl(String mwEmbedUrl) {
            mKCastInfo.setMwEmbedUrl(mwEmbedUrl);
            return this;
        }


        public MediaInfo build() {
            return getMediaInfo(mKCastInfo);
        }


    }



    private static MediaInfo getMediaInfo(KCastInfo kCastInfo) {

        JSONObject customData = CastConfigHelper.getCustomData(kCastInfo);

        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                .setContentType(MOCK_DATA)
                .setCustomData(customData)
                .setMetadata(kCastInfo.getMetadata());

        return mediaInfoBuilder.build();

    }





//    public static void getMediaInfoBuilder(OnCompletion<MediaInfo.Builder> completionListener) {
//
//        JSONObject customData = new JSONObject();
//        JSONObject embedConfig = new JSONObject();
//
//        try {
//
//
//            // OVP
//            String entryId = "0_l1v5vzh3";
//            embedConfig.put("lib", TEMP_LIB_URL_OVP);
//            embedConfig.put("publisherID", "243342");
//            embedConfig.put("uiconfID", "21099702");
//            embedConfig.put("entryID", entryId);
//            //embedConfig.put("flashVars", getFlashVars(null, TEMP_AD_TAG_URL, null, null));
//
//
//
//
//            // OTT
//            /*
//            String entryId = "258656";
//            embedConfig.put("lib", TEMP_LIB_URL_OTT);
//            embedConfig.put("publisherID", "198");
//            embedConfig.put("uiconfID", "8413355");
//            embedConfig.put("entryID", entryId);
//            embedConfig.put("flashVars", getFlashVars("temp_ks", TEMP_AD_TAG_URL, "Web SD", entryId));
//            */
//
//
//
//            customData.put("embedConfig", embedConfig);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//
//        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
//                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
//                .setContentType(MOCK_DATA)
//                .setCustomData(customData);
//
//
//        completionListener.onComplete(mediaInfoBuilder);
//
//
//
//        /*
//        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder("https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/hls/BigBuckBunny.m3u8")
//
//                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
//                .setContentType("application/x-mpegurl");
//
//        completionListener.onComplete(mediaInfoBuilder);
//        */
//
//
//    }
//
//
//
//
//
//
//    public static void getMediaInfoBuilder(PlayerConfig playerConfig, SessionProvider sessionProvider,
//                                           final String uiConf, final String fileFormat,
//                                           final OnCompletion<MediaInfo.Builder> completionListener) {
//
//        // initObj
//        // base url
//
//        final String entryId = getEntryId(playerConfig);
//        final int partnerId = getPartnerId(sessionProvider);
//        final String adTagUrl = getAdTagUrl(playerConfig);
//
//
//        final MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
//                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
//                .setContentType(MOCK_DATA);
//
//
//        getKs(sessionProvider, new OnCompletion<String>() {
//
//            @Override
//            public void onComplete(final String ks) {
//
//                mediaInfoBuilder.setCustomData(getCustomData(uiConf, fileFormat, entryId, partnerId, adTagUrl, ks));
//
//                completionListener.onComplete(mediaInfoBuilder);
//
//
//            }
//        });
//
//    }
//
//
//
//    public static void getMediaInfoBuilder(PlayerConfig playerConfig, SessionProvider sessionProvider,
//                                           String uiConf, final OnCompletion<MediaInfo.Builder> completionListener) {
//
//        getMediaInfoBuilder(playerConfig, sessionProvider, uiConf, null, new OnCompletion<MediaInfo.Builder>() {
//
//            @Override
//            public void onComplete(MediaInfo.Builder mediaInfoBuilder) {
//
//                completionListener.onComplete(mediaInfoBuilder);
//
//            }
//
//        });
//
//    }







}
