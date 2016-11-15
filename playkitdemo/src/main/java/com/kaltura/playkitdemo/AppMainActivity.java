package com.kaltura.playkitdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import static com.kaltura.playkitdemo.R.id.player_fragment;




public class AppMainActivity extends AbsPlayerDemoActivity implements RootMenuFragment.OnRootMenuInteractionListener, SubMenuFragment.OnSubMenuInteractionListener {



    public static final String TAG = "PLAYER_DEMO";


    private ExpandableMenuFragment mExpandableMenuFragment;
    private PlayerFragment mPlayerFragment;
    private RootMenuFragment mRootMenuFragment;
    private SubMenuFragment mSubMenuFragment;




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (findViewById(player_fragment) != null) { // tablet

            mExpandableMenuFragment = ExpandableMenuFragment.newInstance();
            mPlayerFragment = PlayerFragment.newInstance();

            transaction.add(R.id.expanded_menu_fragment, mExpandableMenuFragment);
            transaction.add(R.id.player_fragment, mPlayerFragment);
            transaction.commit();

        } else { // smartphone


            mRootMenuFragment = RootMenuFragment.newInstance();
            mSubMenuFragment = SubMenuFragment.newInstance();

            transaction.add(R.id.smartphone_menu_container, mRootMenuFragment).commit();

        }

    }



    protected int getLayoutId() {
        return R.layout.activity_app_main;
    }



    @Override
    public void onRootMenuInteraction(int  position) {

        Log.v(TAG, "AppMainActivity onRootMenuInteraction position " + position);

        getSupportFragmentManager().beginTransaction().replace(R.id.smartphone_menu_container, mSubMenuFragment, null).commit();

    }


    @Override
    public void onSubMenuInteraction(int  position) {

        Log.v(TAG, "AppMainActivity onSubMenuInteraction position " + position);

        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);

    }



}