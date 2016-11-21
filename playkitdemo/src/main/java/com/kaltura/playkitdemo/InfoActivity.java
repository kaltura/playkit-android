package com.kaltura.playkitdemo;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class InfoActivity extends AbsPlayKitAppActivity {

    TextView mSdkVersion;
    TextView mAppVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSdkVersion = (TextView) findViewById(R.id.sdk_version);
        mAppVersion = (TextView) findViewById(R.id.app_version);


        // TODO sdk version number should come from sdk
        mSdkVersion.setText(String.format(getString(R.string.sdk_version), "1.0.0"));
        mAppVersion.setText(String.format(getString(R.string.app_version), getString(R.string.app_version_number)));
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_info;
    }


    public void handleMoreInfoClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.more_info_url)));
        startActivity(browserIntent);
    }

}
