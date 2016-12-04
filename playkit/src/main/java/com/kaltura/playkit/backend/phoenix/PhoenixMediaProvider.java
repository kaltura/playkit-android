package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonParseException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.data.KalturaMediaAsset;
import com.kaltura.playkit.backend.phoenix.data.KalturaMediaFile;
import com.kaltura.playkit.backend.phoenix.data.PhoenixParser;
import com.kaltura.playkit.backend.phoenix.services.AssetService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.backend.SessionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider implements MediaEntryProvider {

    private RequestQueue requestsExecutor;
    private SessionProvider sessionProvider;
    private MediaAsset mediaAsset;


    private class MediaAsset {
        String assetId;
        @APIDefines.AssetReferenceType
        String referenceType;
        List<String> formats;
        String epgId;
        long startDate;
        String streamType;

        boolean hasFormats() {
            return formats != null && formats.size() > 0;
        }
    }

    //!! add parameter for streamType - catchup/startOver/...

    public PhoenixMediaProvider() {
        this.mediaAsset = new MediaAsset();
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
    }

    public PhoenixMediaProvider setSessionProvider(@NonNull SessionProvider ksProvider) {
        this.sessionProvider = ksProvider;
        return this;
    }

    public PhoenixMediaProvider setAssetId(@NonNull String assetId) {
        this.mediaAsset.assetId = assetId;
        return this;
    }


    public PhoenixMediaProvider setFormats(@NonNull String... formats) {
        this.mediaAsset.formats = new ArrayList<>(Arrays.asList(formats));
        return this;
    }

    /**
     * @param referenceType - can be one of the {@link com.kaltura.playkit.backend.phoenix.APIDefines.AssetReferenceType} values
     * @return
     */
    public PhoenixMediaProvider setReferenceType(@NonNull String referenceType) {
        this.mediaAsset.referenceType = referenceType;
        return this;
    }

    public PhoenixMediaProvider setEpgId(@Nullable String epgId) {
        this.mediaAsset.epgId = epgId;
        return this;
    }

    public PhoenixMediaProvider setStartDate(long startDate) {
        this.mediaAsset.startDate = startDate;
        return this;
    }

    public PhoenixMediaProvider setLiveStreamType(@APIDefines.LiveStreamType String streamType){
        this.mediaAsset.streamType = streamType;
        return this;
    }

    public PhoenixMediaProvider setRequestExecutor(@NonNull RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }

    /**
     * Activates the providers data fetching process.
     * According to previously provided arguments, a request is built and passed to the remote server.
     * Fetching flow can ended with {@link PKMediaEntry} object if succeeded or with {@link ErrorElement} if failed.
     *
     * @param completion - a callback for handling the result of data fetching flow.
     */
    @Override
    public void load(final OnMediaLoadCompletion completion) {
        ErrorElement error = validateAsset();
        if(error != null){
            if (completion != null) {
                completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
            }
            return;
        }

        sessionProvider.getKs(new OnCompletion<String>() {
            @Override
            public void onComplete(String response) {
                ErrorElement error = validateKs(response);
                if (error == null) {
                    final String ks = response;
                    RequestBuilder requestBuilder = AssetService.assetGet(sessionProvider.baseUrl(), ks, mediaAsset.assetId, mediaAsset.referenceType);

                    requestBuilder.completion(new OnRequestCompletion() {
                        @Override
                        public void onComplete(ResponseElement response) {
                            onAssetGetResponse(response, completion);
                        }
                    });

                    requestsExecutor.queue(requestBuilder.build());
                } else {
                    if (completion != null) {
                        completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
                    }
                }
            }
        });
    }

    /**
     * validate basic parameters needed
     * @return
     */
    /*private ErrorElement validateLoad() {
        ErrorElement error = validateKs();
        return error != null ? error : validateAsset();
    }*/


    /**
     * Asset id is required for data fetching.
     * Ott play must have defined format(s) in order to select the right media file to play.
     * @return
     */
    private ErrorElement validateAsset() {
        String error = TextUtils.isEmpty(this.mediaAsset.assetId) ? ": Missing required parameters, assetId" :
                (!this.mediaAsset.hasFormats() ? ": at least 1 media file format is required!" : null);
        return error != null ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + error) :
                null;
    }

    private ErrorElement validateKs(String ks) {
        return TextUtils.isEmpty(ks) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError +": SessionProvider should provide a valid KS token") :
                null;
    }


    private void onAssetGetResponse(final ResponseElement response, final OnMediaLoadCompletion completion) {
        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        if (response != null && response.isSuccess()) {
            KalturaMediaAsset asset = null;

            try {
                //**************************
                /* parse json string to a single object, according to a specific type - returns an object of the specific type */
                //asset = PhoenixParser.parseResult(response.getResponse(), KalturaMediaAsset.class);

                /* parse json string according to 1 or more types - returns Object, can be used for multiple response */
                asset = (KalturaMediaAsset) PhoenixParser.parse(response.getResponse(), KalturaMediaAsset.class);
                if (asset.error != null) {
                    error = asset.error;
                    asset = null;
                }
                //*************************
                //*************************
                /* parse json string to an object of type BaseResult or one of its sub classes - object type is parsed dynamically
                   from the response according to the value of "objectType" property, if none found will be parsed to BaseResult object
                   in case of error response - will be parsed to BaseResult with the error within */
                BaseResult assetResult = PhoenixParser.parse(response.getResponse());
                if(assetResult != null ) {
                    if (assetResult.error == null) {
                        asset = (KalturaMediaAsset) assetResult;
                    } else {
                        error = assetResult.error;
                    }
                } else { // response parsed to null but request to the server returned a "valid" response
                    throw new JsonParseException("missing response object");
                }
                //**************************
            } catch (JsonParseException ex) {
                error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
            }

            if (asset != null) {
                mediaEntry = ProviderParser.getMedia(asset, mediaAsset.formats);
            }

         } else {
            error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
        }

        if (completion != null) {
            completion.onComplete(Accessories.buildResult(mediaEntry, error));
        }
    }

    public static PKMediaEntry getMediaEntry(KalturaMediaAsset assetInfo, List<String> formats) {
        return ProviderParser.getMedia(assetInfo, formats);
    }



    static class ProviderParser {

        static PKMediaEntry getMedia(KalturaMediaAsset assetInfo, List<String> formats) {
            return getMedia(assetInfo.getId() + "", assetInfo.getFiles(), formats);
        }

        static PKMediaEntry getMedia(String id, List<KalturaMediaFile> mediaFiles) {
            return getMedia(id, mediaFiles, null);
        }

        private static PKMediaEntry getMedia(String assetId, List<KalturaMediaFile> mediaFiles, List<String> formats) {
            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId("" + assetId);

            ArrayList<PKMediaSource> sources = new ArrayList<>();
            long maxDuration = 0;
            if (mediaFiles != null) {
                // if provided, only the "formats" matching MediaFiles should be parsed and added to the PKMediaEntry media sources
                for (KalturaMediaFile file : mediaFiles) {
                    if (formats == null || formats.contains(file.getType())) {
                        sources.add(new PKMediaSource().setId(file.getId() + "").setUrl(file.getUrl()));
                        maxDuration = Math.max(file.getDuration(), maxDuration);
                    }
                }
            }
            return mediaEntry.setDuration(maxDuration).setSources(sources);
        }
    }
}
/*public PhoenixMediaProvider(SessionProvider sessionProvider, String assetId, String assetReferenceType, String... formats) {
        this.sessionProvider = sessionProvider;
        this.assetId = assetId;
        this.referenceType = assetReferenceType;
        this.formats = Arrays.asList(formats);
    }*/

/*private String assetId;
    private String referenceType;
    private List<String> formats;*/