package com.kaltura.magikapp.magikapp.asset_page;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.kaltura.magikapp.R;

/**
 * Created by vladir on 30/11/2016.
 */

public abstract class BaseRoundButton extends LinearLayout {

    protected final int RADIUS = 70;
    protected View mRoot;
    protected LinearLayout mContainer;
    protected int mPrimaryColor;
    protected ButtonSize mButtonSize;

    public enum ButtonSize {
        Small(0),
        Medium(1),
        Large(2);

        int id;

        ButtonSize(int id) {
            this.id = id;
        }

        static ButtonSize fromId(int id) {
            for (ButtonSize f : values()) {
                if (f.id == id) return f;
            }
            throw new IllegalArgumentException();
        }
    }

    public BaseRoundButton(Context context) {
        super(context);
        inflate(context);
    }

    public BaseRoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context);
    }

    public BaseRoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context);
    }

    private void inflate(Context context){
        mRoot = inflate(context, getButtonLayout(), this);
    }

    protected void init(Context context, AttributeSet attrs){
        mContainer = (LinearLayout) mRoot.findViewById(R.id.round_button_container);

        if (attrs != null) {
            TypedArray appearance = context.obtainStyledAttributes(attrs, R.styleable.HelenRoundButton);
            int size = appearance.getInt(R.styleable.HelenRoundButton_button_size, -1);
            if (size != (-1)){
                mButtonSize = ButtonSize.fromId(size);
                mContainer.setLayoutParams(getButtonLayoutParams());
            }
        }

//        if (!isInEditMode()) {
//            String tmpPrimaryColor = getConfiguration().getColor(ConfigObjectConsts.COLOR_PRIMARY, ConfigObjectConsts.COLOR_PRIMARY1);
//            mPrimaryColor = Color.parseColor(tmpPrimaryColor);
//        } else {
//            mPrimaryColor = Color.parseColor(String.valueOf(Color.MAGENTA));
//        }

//        Drawable defaultBackground = getDefaultBackground();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            String tmpPrimaryColor = TvinciSDK.getConfiguration().getColor(ConfigObjectConsts.COLOR_SECONDARY, ConfigObjectConsts.COLOR_SECONDARY1);
//            RippleDrawable r = new RippleDrawable(getPressedColorSelector(Color.parseColor(tmpPrimaryColor)), defaultBackground, null);
//            setBackground(r);
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            setBackground(defaultBackground);
//        } else {
//            setBackgroundDrawable(defaultBackground);
//        }
    }

    abstract protected int getButtonLayout();

    protected Drawable getDefaultBackground() {
        int focusedStateSet[] = { android.R.attr.state_selected, android.R.attr.state_pressed };
        int checkedStateSet[] = { -android.R.attr.state_activated };

        StateListDrawable sld = new StateListDrawable();

        Drawable defaultSelectedBackground = getDefaultSelectedBackground();
        for (int s : focusedStateSet) {
            sld.addState(new int[] { s }, defaultSelectedBackground);
        }

        sld.addState(checkedStateSet, getDefaultRegularState());

        return sld;
    }

    protected Drawable getDefaultSelectedBackground(){
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(RADIUS);
        background.setColor(mPrimaryColor);
        return background;
    }

    protected Drawable getDefaultRegularState(){
        GradientDrawable regular = new GradientDrawable();
        regular.setShape(GradientDrawable.RECTANGLE);
        regular.setStroke(1, ResourcesCompat.getColor(getResources(), R.color.white, null));
        regular.setCornerRadius(RADIUS);
        regular.setColor(Color.TRANSPARENT);
        return regular;
    }

    protected Drawable getDisabledState(){
        return null;
    }


    protected static ColorStateList getPressedColorSelector(int pressedColor) {
        return new ColorStateList(new int[][]{new int[]{}}, new int[]{pressedColor});
    }

    protected LayoutParams getButtonLayoutParams(){
        switch (mButtonSize){
            case Large:
                return getLargeLinearLayout();
            case Medium:
                return getMediumLinearLayout();
            case Small:
                return getSmallLinearLayout();
            default:
                return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    protected LayoutParams getSmallLinearLayout() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    protected LayoutParams getMediumLinearLayout() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    protected LayoutParams getLargeLinearLayout() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }


    public int getDimension(int res){
        return (int) getResources().getDimension(res);
    }


}
