package com.kaltura.magikapp.magikapp;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.connect.backend.magikapp.data.Configuration;
import com.kaltura.magikapp.MagikApplication;
import com.kaltura.magikapp.R;
import com.kaltura.magikapp.SplashFragment;
import com.kaltura.magikapp.magikapp.core.ActivityComponentsInjector;
import com.kaltura.magikapp.magikapp.core.ComponentsInjector;
import com.kaltura.magikapp.magikapp.core.FragmentAid;
import com.kaltura.magikapp.magikapp.core.PluginProvider;
import com.kaltura.magikapp.magikapp.homepage.Template1Fragment;
import com.kaltura.magikapp.magikapp.menu.MenuMediator;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;

import static android.view.View.VISIBLE;


public class ScrollingActivity extends AppCompatActivity implements FragmentAid, ToolbarMediator.ToolbarActionListener, PluginProvider, MagikApplication.ConfigurationsReady {

    public MenuMediator mMenuMediator;
    private ToolbarMediator mToolbarMediator;
    protected CoordinatorLayout mCoordMainContainer;
    protected android.support.v4.app.FragmentManager mFragmentManager;
    protected ProgressBar mWaitProgress;
    protected int mLastCollapsingLayoutColor = -1;
    private SplashFragment splashFragment;

    private Theme_Type theme = Theme_Type.COLA;
    public enum Theme_Type {
        SPORT,
        COLA,
        FOOD
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // TODO: show spinner!
        MagikApplication.get().registerToConfigurationReady(this);
    }

    private void startLoadingActivity() {
        setContentView(R.layout.base_drawer);
        initComponents();
        inflateLayout();
        initOthers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu and adds defined items to the toolbar.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);

        Drawable icon = menu.getItem(0).getIcon();
        icon.mutate();
        icon.setColorFilter(theme == Theme_Type.FOOD? Color.DKGRAY: Color.WHITE, PorterDuff.Mode.SRC_IN);

        mToolbarMediator.setToolbarMenu(menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    private Fragment getTemplate() {
        return Template1Fragment.newInstance();
    }

    protected void inflateLayout() {
        getFragmentManager().beginTransaction().add(R.id.activity_scrolling_content, getTemplate()).commit();
    }

    public Theme_Type getCurrentTheme(){
        return theme;
    }

    protected void initComponents() {
        ActivityComponentsInjector injector = getComponentsInjector();// create function for the injector and change it in users activity

        mMenuMediator = injector.getMenu(this);// new SlideMenuMediator(this, R.id.drawer_layout, R.id.sideMenu);

        mToolbarMediator = injector.getToolbar(this);// new TopToolbarMediator(this, R.id.toolbar, this);
        mToolbarMediator.setToolbarActionListener(this);
        switch (theme){
            case FOOD:
                mToolbarMediator.setToolbarLogo(getResources().getDrawable(R.drawable.logo_app));
                break;
            case COLA:
                mToolbarMediator.setToolbarLogo(getResources().getDrawable(R.drawable.coca_cola_logo));
                break;
            case SPORT:
                mToolbarMediator.setToolbarLogo(getResources().getDrawable(R.drawable.logo_app));
                break;
        }

        mCoordMainContainer = (CoordinatorLayout) findViewById(R.id.activity_scrolling);
//        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        mToolbarMediator.onToolbarMenuAction(menuItem.getItemId());

        return super.onOptionsItemSelected(menuItem);
    }

    @NonNull
    protected ActivityComponentsInjector getComponentsInjector() {
        return new ComponentsInjector();
    }

    protected void initOthers() {
        mFragmentManager = getSupportFragmentManager();
    }

    @Override
    public void onToolbarAction(ToolbarMediator.ToolbarAction action) {
        switch (action) {
            case Menu:
                mMenuMediator.toggleDrawer();
                break;

            case Back:
                onBackPressed();
                break;
        }
    }

    protected boolean backOnStack() {
        boolean handled = false;
        if (mFragmentManager != null && mFragmentManager.getBackStackEntryCount() >= 1) {
            handled = mFragmentManager.getBackStackEntryCount() > 1;
            mFragmentManager.popBackStackImmediate();
        }
        return handled;
    }

//    protected int getFragmentsContainerId(){
//        return R.id.activity_scrolling_content;
//    }

    @Override
    public AppCompatActivity getActivity() {
        return this;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void setWaitProgressVisibility(int state) {
        if (state == VISIBLE && mWaitProgress == null) {
            mWaitProgress = (ProgressBar) findViewById(R.id.wait_progress);
        }

        if (mWaitProgress != null) {
            mWaitProgress.setVisibility(state);
        }
    }

    @Override
    public void setToolbarTitle(String title) {
        mToolbarMediator.setTitle(title);
    }

    @Override
    public void setToolbarActionListener(ToolbarMediator.ToolbarActionListener listener) {
        mToolbarMediator.setToolbarActionListener(listener);
    }

    protected int[] getCollapsingFromToBackColors(boolean transparent) {
        int color = -1;
        switch (theme){
            case FOOD:
                color = Color.WHITE;
                break;
            case COLA:
                color = Color.RED;
                break;
            case SPORT:
                color = Color.GREEN;
                break;
        }
        int[] colors = new int[2];
        colors[0] = transparent ? color : Color.TRANSPARENT;
        colors[1] = transparent ? Color.TRANSPARENT : color;
        return colors;
    }

    @Override
    public boolean changeToolbarLayoutColor(boolean toBeTransparent, final View... applyTo) {

        int[] fromToColors = getCollapsingFromToBackColors(toBeTransparent);

        //TODO: to prevent blinks we need the prev color
        if (fromToColors[1] == mLastCollapsingLayoutColor) {
            return false;
        }

        mLastCollapsingLayoutColor = fromToColors[1];

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromToColors[0], fromToColors[1]);
        colorAnimation.setDuration(0); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int animatedColor = (int) animator.getAnimatedValue();
                mToolbarMediator.setToolbarColor(animatedColor);
                if (applyTo != null) {
                    for (View view : applyTo) {
                        view.setBackgroundColor(animatedColor);
                    }
                }
            }

        });
        colorAnimation.start();

        return true;
    }

    @Override
    public void setStatusBarColor(int color/*Activity activity, Context context, boolean isTransparent*/){
        //mToolbarMediator.setStatusBarState(activity, context, isTransparent);
        if (color != -1) {
            int windowFlagskitkat = -1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                windowFlagskitkat = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                windowFlagskitkat = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            }
            if (windowFlagskitkat != -1){
                getWindow().setFlags(windowFlagskitkat, windowFlagskitkat);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(color/*context.getResources().getColor(R.color.transparent)*/);
                    getWindow().setNavigationBarColor(color/*context.getResources().getColor(R.color.transparent)*/);
                }
            }
        }
    }

    @Override
    public void setToolbarHomeButton(@ToolbarMediator.ToolbarHomeButton int button) {
        Drawable[] drawables = new Drawable[2];
        drawables[0] = getResources().getDrawable(R.mipmap.ic_action_navigation_arrow_back);
        drawables[0].setColorFilter(theme == Theme_Type.FOOD? Color.DKGRAY : Color.WHITE, PorterDuff.Mode.SRC_IN);
        drawables[1] = getResources().getDrawable(R.mipmap.menu_icon_tablet);
        drawables[1].setColorFilter(theme == Theme_Type.FOOD? Color.DKGRAY : Color.WHITE, PorterDuff.Mode.SRC_IN);

        mToolbarMediator.setHomeButton(button, drawables);
    }

    @Override
    public void onReady(Configuration configuration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                splashFragment = SplashFragment.newInstance(MagikApplication.get().getConfigurations().getSplashVideo());
                splashFragment.show(getSupportFragmentManager(), "SPLASH");
                ScrollingActivity.this.getWindow().getDecorView().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        splashFragment.dismiss();
                        startLoadingActivity();
                    }
                }, 10000);
            }
        });
    }

    @Override
    public void onLoadFailure() {
        finish();
    }
}
