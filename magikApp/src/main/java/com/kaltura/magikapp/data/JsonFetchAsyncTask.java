package com.kaltura.magikapp.data;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by itanbarpeled on 18/11/2016.
 */

public class JsonFetchAsyncTask extends AsyncTask<String, String, String> {


    private JsonFetchHandler.OnJsonFetchedListener mJsonFetchedListener;
    private ProgressDialog mSpinner;
    private boolean mShowSpinner;


    public JsonFetchAsyncTask(Context context, JsonFetchHandler.OnJsonFetchedListener jsonFetchedListener) {
        this(context, false, jsonFetchedListener);
    }


    public JsonFetchAsyncTask(Context context, boolean showSpinner, JsonFetchHandler.OnJsonFetchedListener jsonFetchedListener) {
        mJsonFetchedListener = jsonFetchedListener;
        mShowSpinner = showSpinner;
        mSpinner = new ProgressDialog(context);
        mSpinner.setCancelable(false);
    }



    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mShowSpinner) {
            mSpinner.show();
        }
    }


    @Override
    protected String doInBackground(String... params) {

        HttpURLConnection urlConnection = null;
        InputStream in = null;
        BufferedReader reader = null;
        StringBuilder result = new StringBuilder();

        try {

            URL url = new URL(params[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-type", "application/json");
            in = new BufferedInputStream(urlConnection.getInputStream());

            reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }


        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            try {
                if (reader != null) {
                    reader.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }


        return result.toString();

    }

    @Override
    protected void onPostExecute(String result) {

        super.onPostExecute(result);
        if (mSpinner.isShowing()) {
            mSpinner.dismiss();
        }

        mJsonFetchedListener.onJsonFetched(result);
    }
}