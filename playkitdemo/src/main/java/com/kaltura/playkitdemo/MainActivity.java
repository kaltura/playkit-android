package com.kaltura.playkitdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.kaltura.playkitdemo.jsonConverters.ConverterPlayKitApp;
import com.kaltura.playkitdemo.jsonConverters.ConverterSubMenu;

import java.util.ArrayList;

import static com.kaltura.playkitdemo.R.id.player_fragment;




public class MainActivity extends AbsPlayKitAppActivity implements RootMenuFragment.OnRootMenuInteractionListener, SubMenuFragment.OnSubMenuInteractionListener {



    public static final String TAG = "PLAY_KIT";
    public static final String CONVERTER_SUB_MENU = "CONVERTER_SUB_MENU";
    public static final String CONVERTER_SUB_MENU_LIST = "CONVERTER_SUB_MENU_LIST";

    private ExpandableMenuFragment mExpandableMenuFragment;
    private PlayerFragment mPlayerFragment;
    private RootMenuFragment mRootMenuFragment;
    private SubMenuFragment mSubMenuFragment;
    private ConverterPlayKitApp mConverterPlayKitApp;




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mConverterPlayKitApp = intent.getParcelableExtra(SplashActivity.CONVERTER_PLAY_KIT_APP);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (findViewById(player_fragment) != null) { // tablet

            mExpandableMenuFragment = ExpandableMenuFragment.newInstance();
            //mPlayerFragment = PlayerFragment.newInstance();

            transaction.add(R.id.expanded_menu_fragment, mExpandableMenuFragment);
            //transaction.add(R.id.player_fragment, mPlayerFragment);
            transaction.addToBackStack(null).commit();

        } else { // smartphone

            mRootMenuFragment = RootMenuFragment.newInstance(mConverterPlayKitApp);
            transaction.add(R.id.smartphone_menu_container, mRootMenuFragment);
            transaction.addToBackStack(null).commit();

        }
    }



    @Override
    public void onResume() {
        super.onResume();

        //setBackButtonVisibility(false);
    }



    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }


    @Override
    public void onRootMenuInteraction(int rootMenuPosition) {

        Log.v(TAG, "MainActivity onRootMenuInteraction rootMenuPosition " + rootMenuPosition);

        ArrayList<ConverterSubMenu> converterSubMenuList = getSubMenuList(rootMenuPosition);

        /*
        if there is only one sub menu item, we show directly
        the PlayerActivity, instead of showing SubMenuFragment, and then PlayerActivity
         */
        if (converterSubMenuList.size() == 1) {
          startPlayerActivity(rootMenuPosition, 0);
        } else {

            setBackButtonVisibility(true);

            mSubMenuFragment = SubMenuFragment.newInstance(rootMenuPosition, converterSubMenuList);
            FragmentTransaction transaction  = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.smartphone_menu_container, mSubMenuFragment);
            transaction.addToBackStack(null).commit();
        }
    }


    @Override
    public void onSubMenuInteraction(int rootMenuPosition, int subMenuPosition) {

        Log.v(TAG, "MainActivity onSubMenuInteraction rootMenuPosition " + rootMenuPosition + " subMenuPosition " + subMenuPosition);

        startPlayerActivity(rootMenuPosition, subMenuPosition);

    }


    private ArrayList<ConverterSubMenu> getSubMenuList(int rootMenuPosition) {
        return new ArrayList<>(mConverterPlayKitApp.getConverterRootMenuList().get(rootMenuPosition).getSubMenu());
    }


    private void startPlayerActivity(int rootMenuPosition, int subMenuPosition) {

        ArrayList<ConverterSubMenu> converterSubMenuList = getSubMenuList(rootMenuPosition);

        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        intent.putExtra(CONVERTER_SUB_MENU, converterSubMenuList.get(subMenuPosition));
        startActivity(intent);
    }


}