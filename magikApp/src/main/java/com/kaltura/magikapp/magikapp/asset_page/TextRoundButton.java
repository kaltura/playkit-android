package com.kaltura.magikapp.magikapp.asset_page;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.kaltura.magikapp.R;

/**
 * Created by vladir on 30/11/2016.
 */

public class TextRoundButton extends BaseRoundButton {

    protected TextView mText;

    public TextRoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public TextRoundButton(Context context) {
        super(context);
        init(context, null);
    }

    public TextRoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);
        mText = (TextView) mRoot.findViewById(R.id.round_button_text);

        if (!isInEditMode()) {
            if (attrs != null) {
                TypedArray appearance = context.obtainStyledAttributes(attrs, R.styleable.HelenRoundButton);
                String text = appearance.getString(R.styleable.HelenRoundButton_round_button_text);
                if (text != null) {
                    mText.setText(text);
                }
//                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (int) getResources().getDimension(R.dimen.helen_round_button_text_size));

                appearance.recycle();
            }
        }
    }

    @Override
    protected int getButtonLayout() {
        return R.layout.text_round_button;
    }

    public void setText(String text){
        mText.setText(text);
    }

    @Override
    protected LayoutParams getSmallLinearLayout() {
        return new LayoutParams(getDimension(R.dimen.text_round_button_small_width),
                getDimension(R.dimen.text_round_button_small_height));
    }

    @Override
    protected LayoutParams getMediumLinearLayout() {
        return new LayoutParams(getDimension(R.dimen.text_round_button_medium_width),
                getDimension(R.dimen.text_round_button_medium_height));
    }

    @Override
    protected LayoutParams getLargeLinearLayout() {
        return new LayoutParams(getDimension(R.dimen.text_round_button_large_width),
                getDimension(R.dimen.text_round_button_large_height));
    }



}


























