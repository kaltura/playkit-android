package com.kaltura.playkitdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.kaltura.playkitdemo.jsonConverters.ConverterSubMenu;


public class PlayerActivity extends AbsPlayerDemoActivity {

    private ConverterSubMenu mConverterSubMenu;
    private PlayerFragment mPlayerFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = getIntent();
        mConverterSubMenu = intent.getParcelableExtra(MainActivity.CONVERTER_SUB_MENU);

        mPlayerFragment = PlayerFragment.newInstance(mConverterSubMenu);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.player_fragment, mPlayerFragment).commit();

    }


    protected int getLayoutId() {
        return R.layout.activity_player;
    }

}
