package com.kaltura.magikapp.magikapp;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.homepage.FragmentActivityMediator;
import com.kaltura.magikapp.magikapp.homepage.Template1Fragment;


public class ScrollingActivity extends AppCompatActivity implements FragmentActivityMediator {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        // get template type and create fragment with it
        getFragmentManager().beginTransaction().add(R.id.activity_scrolling_content, getTemplate()).commit();

    }

    private Fragment getTemplate() {
        return Template1Fragment.newInstance();
    }

    @Override
    public void setToolbar(Toolbar toolbar) {
//        setSupportActionBar(toolbar);
    }
}
