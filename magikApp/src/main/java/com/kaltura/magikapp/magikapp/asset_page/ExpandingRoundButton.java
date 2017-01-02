package com.kaltura.magikapp.magikapp.asset_page;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.kaltura.magikapp.R;

import static com.kaltura.magikapp.magikapp.asset_page.ExpandingRoundButton.ExpansionMode.Collapsed;
import static com.kaltura.magikapp.magikapp.asset_page.ExpandingRoundButton.ExpansionMode.Expanded;

/** Used for like and comments button in the secondary button on the media page
 * Created by vladir on 06/12/2016.
 */

public class ExpandingRoundButton extends IconTextRoundButton {

    private ExpansionMode mExpansionMode;

    public enum ExpansionMode {
        Collapsed,
        Expanded
    }

    public ExpandingRoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public ExpandingRoundButton(Context context) {
        super(context);
        init(context, null);
    }

    public ExpandingRoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs){
        mExpansionMode = Collapsed;
        super.init(context, attrs);

//todo need to figure out how to animate the collapse and expense
//        LayoutTransition lt = new LayoutTransition();
//        lt.enableTransitionType(LayoutTransition.APPEARING);
//        mContainer.setLayoutTransition(lt);

    }

    @Override
    protected LinearLayout.LayoutParams getSmallLinearLayout() {
        LinearLayout.LayoutParams params;
        if (mExpansionMode.equals(Collapsed)){
            params = new LinearLayout.LayoutParams(getDimension(R.dimen.round_round_button_small_width),
                    getDimension(R.dimen.round_round_button_small_height));
        } else {
            params = new LinearLayout.LayoutParams(getDimension(R.dimen.like_round_button_width),
                    getDimension(R.dimen.text_round_button_small_height));
        }

        return params;
    }

    @Override
    protected LinearLayout.LayoutParams getMediumLinearLayout() {
        LinearLayout.LayoutParams params;
        if (mExpansionMode.equals(Collapsed)){
            params = new LinearLayout.LayoutParams(getDimension(R.dimen.round_round_button_medium_width),
                    getDimension(R.dimen.round_round_button_medium_height));
        } else {
            params = new LinearLayout.LayoutParams(getDimension(R.dimen.like_round_button_width),
                    getDimension(R.dimen.text_round_button_medium_height));
        }

        return params;
    }

    @Override
    protected LinearLayout.LayoutParams getLargeLinearLayout() {
        LinearLayout.LayoutParams params;
        if (mExpansionMode.equals(Collapsed)){
            params = new LinearLayout.LayoutParams(getDimension(R.dimen.round_round_button_large_width),
                    getDimension(R.dimen.round_round_button_large_height));
        } else {
            params = new LinearLayout.LayoutParams(getDimension(R.dimen.like_round_button_width),
                    getDimension(R.dimen.text_round_button_large_height));
        }

        return params;
    }

    @Override
    public void setText(String text) {
        if (text.equals("0")){
            mExpansionMode = Collapsed;
            mImageView.setPadding(0, 0, 0, 0);
            mText.setText("");
            mText.setPadding(0, 0, 0, 0);

        } else {
            mExpansionMode = Expanded;
            super.setText(text);
        }

        mContainer.setLayoutParams(getButtonLayoutParams());
    }

}















