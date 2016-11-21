package com.kaltura.playkitdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Created by itanbarpeled on 14/11/2016.
 */

abstract class AbsPlayKitAppActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        if (savedInstanceState != null) {
            return;
        }

        setToolbar();
    }


    abstract protected int getLayoutId();


    protected void setToolbar() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        //getSupportActionBar().setWindowTitle("BLIII");
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_info:
                startInfoActivity();
                return true;

            case R.id.action_chromecast:
                showMessage(R.string.feature_not_developed);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }


    protected void showMessage(int string) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Snackbar snackbar = Snackbar.make(toolbar, string, Snackbar.LENGTH_LONG);
        snackbar.show();
    }


    protected void startInfoActivity() {
        Intent intent = new Intent(AbsPlayKitAppActivity.this, InfoActivity.class);
        startActivity(intent);
    }



}
