package com.kaltura.magikapp.magikapp.asset_page;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.kaltura.magikapp.R;


/**
 * Created by vladir on 03/12/2016.
 */

public class IconTextRoundButton extends TextRoundButton implements SecondaryViewsInitiator.SecondaryView {

    protected ImageView mImageView;

    public IconTextRoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public IconTextRoundButton(Context context) {
        super(context);
        init(context, null);
    }

    public IconTextRoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @Override
    protected int getButtonLayout() {
        return R.layout.icon_text_round_button;
    }

    protected void init(Context context, AttributeSet attrs){
        super.init(context, attrs);
        mImageView = (ImageView) mRoot.findViewById(R.id.round_button_image);
        mImageView.setDuplicateParentStateEnabled(true);

        if (attrs != null) {
            TypedArray appearance = context.obtainStyledAttributes(attrs, R.styleable.HelenRoundButton);
            Drawable imageNotPressedRef = appearance.getDrawable(R.styleable.HelenRoundButton_image_not_pressed);
            Drawable imagePressedRef = appearance.getDrawable(R.styleable.HelenRoundButton_image_pressed);
            setImages(new Drawable[]{imagePressedRef, imageNotPressedRef});
            appearance.recycle();
        }
    }

    @Override
    public void setText(String text) {
        if (text != null && !text.isEmpty()) {
            mText.setText(text);
            setTextPaddings();
            setImagePaddings();
        }
    }

    protected void setImagePaddings(){
        float left = getResources().getDimension(R.dimen.icon_text_round_button_image_padding_left);
        float top = getResources().getDimension(R.dimen.icon_text_round_button_image_padding_top);
        float right = getResources().getDimension(R.dimen.icon_text_round_button_image_padding_right);
        float bottom = getResources().getDimension(R.dimen.icon_text_round_button_image_padding_bottom);
        mImageView.setPadding((int)left,(int)top,(int)right,(int)bottom);
    }

    protected void setTextPaddings(){
        float right = getResources().getDimension(R.dimen.icon_text_round_button_text_padding_right);
        mText.setPadding(0, 0, (int) right, 0);
    }

    public void setImages(Drawable[] drawables) {
        mImageView.setImageDrawable(RoundButtonUtils.getStateDrawableForImageImages(drawables));
    }

}




















