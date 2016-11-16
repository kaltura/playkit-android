package com.kaltura.playkit.backend.phoenix;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.data.AssetInfo;
import com.kaltura.playkit.backend.phoenix.data.AssetResult;
import com.kaltura.playkit.backend.phoenix.data.MediaFile;
import com.kaltura.playkit.backend.phoenix.data.ResultAdapter;
import com.kaltura.playkit.backend.phoenix.services.AssetService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.SessionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider implements MediaEntryProvider {

    private RequestQueue requestsExecutor = APIOkRequestsExecutor.getSingleton();

    private SessionProvider sessionProvider;
    private String assetId;
    private String referenceType;
    private List<String> formats;


    public PhoenixMediaProvider(SessionProvider sessionProvider, String assetId, String assetReferenceType, String... formats) {
        this.sessionProvider = sessionProvider;
        this.assetId = assetId;
        this.referenceType = assetReferenceType;
        this.formats = Arrays.asList(formats);
    }

    public PhoenixMediaProvider setRequestExecutor(RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }

    public PhoenixMediaProvider setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public PhoenixMediaProvider setFormats(List<String> formats) {
        this.formats = new ArrayList<>(formats);
        return this;
    }

    public PhoenixMediaProvider setSessionProvider(SessionProvider ksProvider) {
        this.sessionProvider = ksProvider;
        return this;
    }

     public PhoenixMediaProvider setReferenceType(String referenceType) {
        this.referenceType = referenceType;
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
        // Ott play must have defined format(s) in order to select the right media file to play.
        if (this.formats == null || formats.size() == 0) {
            if (completion != null) {
                completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": media file format is required!")));
            }
            return;
        }

        RequestBuilder requestBuilder = AssetService.assetGet(sessionProvider.baseUrl(), sessionProvider.partnerId(), sessionProvider.getKs(), assetId, referenceType);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                onAssetGetResponse(response, completion);
            }
        });

        requestsExecutor.queue(requestBuilder.build());

    }


    private void onAssetGetResponse(final ResponseElement response, final OnMediaLoadCompletion completion) {
        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        if (response != null && response.isSuccess()) {
            AssetInfo asset = null;

            try {
                AssetResult assetResult = PhoenixParser.parseAssetResult(response.getResponse());
                if(assetResult.error != null){
                    error = assetResult.error;
                } else {
                    asset = assetResult.asset;
                }
            } catch (JsonSyntaxException ex) {
                error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
            }

            if (asset != null) {
                mediaEntry = PhoenixParser.getMedia(asset, formats);
            }

        } else {
            error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
        }

        if (completion != null) {
            completion.onComplete(Accessories.buildResult(mediaEntry, error));
        }
    }

    public static PKMediaEntry getMediaEntry(AssetInfo assetInfo, List<String> formats) {
        return PhoenixParser.getMedia(assetInfo, formats);
    }



    static class PhoenixParser {

        static PKMediaEntry getMedia(AssetInfo assetInfo, List<String> formats) {
            return getMedia(assetInfo.getId()+"", assetInfo.getFiles(), formats);
        }

        static PKMediaEntry getMedia(String id, List<MediaFile> mediaFiles) {
            return getMedia(id, mediaFiles, null);
        }

        private static PKMediaEntry getMedia(String assetId, List<MediaFile> mediaFiles, List<String> formats){
            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId("" + assetId);

            ArrayList<PKMediaSource> sources = new ArrayList<>();
            long maxDuration = 0;
            if(mediaFiles != null) {
                // if provided, only the "formats" matching MediaFiles should be parsed and added to the PKMediaEntry media sources
                for (MediaFile file : mediaFiles) {
                    if (formats == null || formats.contains(file.getType())) {
                        sources.add(new PKMediaSource().setId(file.getId() + "").setUrl(file.getUrl()));
                        maxDuration = Math.max(file.getDuration(), maxDuration);
                    }
                }
            }
            return mediaEntry.setDuration(maxDuration).setSources(sources);
        }

        static AssetResult parseAssetResult(String json) throws JsonSyntaxException {
            return new GsonBuilder().registerTypeAdapter(AssetResult.class, new ResultAdapter()).create().fromJson(json, AssetResult.class);
        }
    }
}
