package com.kaltura.playkit.backend.ovp;

import android.text.TextUtils;

import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.ParamsRequestElement;
import com.kaltura.playkit.connect.RequestConfiguration;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.backend.base.RequestsHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpRequestsHandler extends RequestsHandler{

    public KalturaOvpRequestsHandler(String beAddress, RequestQueue executor) {
        super(beAddress, executor);
    }

    private HashMap<String, String> getWidgetKsParams(String keyPrefix, int partnerId){
        HashMap<String, String> params = new HashMap<>();
        params.put(keyPrefix+"service", "session");
        params.put(keyPrefix+"action", "startWidgetSession");
        params.put(keyPrefix+"widgetId", "_"+partnerId);
        return params;
    }

    private HashMap<String, String> getBaseEntryListParams(String keyPrefix, String entryId, int partnerId, String ks){
        HashMap<String, String> params = new HashMap<>();
        params.put(keyPrefix+"service", "baseEntry");
        params.put(keyPrefix+"action", "list");
        params.put(keyPrefix+"partnerId", ""+partnerId);
        params.put(keyPrefix+"filter.redirectFromEntryId", entryId);
        params.put(keyPrefix+"ks", ks);
        return params;
    }

    private HashMap<String, String> getBaseEntryContextDataParams(String keyPrefix, String entryId, int partnerId, String ks){
        HashMap<String, String> params = new HashMap<>();
        params.put(keyPrefix+"service", "baseEntry");
        params.put(keyPrefix+"action", "getContextData");
        params.put(keyPrefix+"partnerId", ""+partnerId);
        params.put(keyPrefix+"entryId", entryId);
        params.put(keyPrefix+"ks", ks);
        params.put(keyPrefix+"contextDataParams.ks", ks);
        params.put(keyPrefix+"contextDataParams.referrer", "MediaProvider");
        return params;
    }


    public void listEntry(final String ks, final int parentId, final String entryId, final OnRequestCompletion completion) {
        ParamsRequestElement requestElement = new ParamsRequestElement() {

            @Override
            public HashMap<String, String> getParams() {
                final HashMap<String, String> params = new HashMap<>();
                int reqPrefix = 1;
                String paramsKs = ks;
                if(TextUtils.isEmpty(ks)){
                    HashMap<String, String> widgetKsParams = getWidgetKsParams(reqPrefix+":", parentId);
                    paramsKs = "{1:result:ks}";
                    params.putAll(widgetKsParams);
                    reqPrefix++;
                }

                params.putAll(getBaseEntryListParams(reqPrefix+":", entryId, parentId, paramsKs));
                reqPrefix++;
                params.putAll(getBaseEntryContextDataParams(reqPrefix+":", entryId, parentId, paramsKs));
                return params;
            }

            @Override
            public boolean isMultipart() {
                return false;
            }

            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public String getUrl() {
                return baseUrl+"service/multirequest/action/null";
            }

            @Override
            public String getBody() {
                return null;
            }

            @Override
            public String getTag() {
                return "baseEntry-list";
            }

            @Override
            public Map getHeaders() {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public RequestConfiguration config() {
                return null;
            }

            @Override
            public void onComplete(ResponseElement responseElement) {
                if(completion != null){
                    completion.onComplete(responseElement);
                }
            }
        };

        requestsExecutor.queue(requestElement);
    }
}
