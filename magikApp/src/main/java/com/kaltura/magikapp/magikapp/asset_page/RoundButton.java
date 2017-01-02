package com.kaltura.magikapp.magikapp.asset_page;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.kaltura.magikapp.R;

/**
 * Created by vladir on 30/11/2016.
 */

public class RoundButton extends BaseRoundButton implements SecondaryViewsInitiator.SecondaryView {

    protected RoundButtonCommonImage mMode;
    protected ImageView mImageView;


    enum RoundButtonCommonImage {
        Like(0),
        Favorites(1),
        Comment(2),
        Share(3),
        Play(4),
        Download(5),
        MoreOptions(6);

        int id;

        RoundButtonCommonImage(int id) {
            this.id = id;
        }

        static RoundButtonCommonImage fromId(int id) {
            for (RoundButtonCommonImage f : values()) {
                if (f.id == id) return f;
            }
            throw new IllegalArgumentException();
        }
    }

    public RoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundButton(Context context) {
        super(context);
        init(context, null);
    }

    public RoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected int getButtonLayout() {
        return R.layout.round_button_layout;
    }

    protected void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);
        mImageView = (ImageView) mRoot.findViewById(R.id.round_button_image);

        if (!isInEditMode()) {
            if (attrs != null) {
                TypedArray appearance = context.obtainStyledAttributes(attrs, R.styleable.HelenRoundButton);
                int modeInt = appearance.getInt(R.styleable.HelenRoundButton_button_mode, -1);

                if (modeInt != (-1)) {
                    mMode = RoundButtonCommonImage.fromId(modeInt);
                    setImages(populateCommonImages(mMode));
                } else {
                    Drawable imageNotPressedRef = appearance.getDrawable(R.styleable.HelenRoundButton_image_not_pressed);
                    Drawable imagePressedRef = appearance.getDrawable(R.styleable.HelenRoundButton_image_pressed);
                    mImageView.setImageDrawable(RoundButtonUtils.getStateDrawableForImageImages(new Drawable[]{imagePressedRef, imageNotPressedRef}));
                }

                appearance.recycle();
            } else {
                setImages(populateCommonImages(mMode));
            }
        }
    }

    private Drawable[] populateCommonImages(RoundButtonCommonImage mode) {
        Drawable notPressed = null;
        Drawable pressed = null;
        switch (mode){
            case Like:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_face, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_face, null);
                break;
            case Comment:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_insta, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_insta, null);
                break;
            case Favorites:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_pintrest, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_pintrest, null);
                break;
            case Share:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_mail, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_mail, null);
                break;
            case Play:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.vid_badge_copy, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.vid_badge_copy, null);
                break;
            case Download:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_twitter, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_twitter, null);
                break;
            case MoreOptions:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_twitter, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_twitter, null);
                break;
        }

        return new Drawable[]{pressed, notPressed};

    }

    /**
     * images {pressed, not pressed}
     ø     * @param images
     */
    private void setImages(Drawable[] images){
        Drawable pressed = images[0];
        Drawable notPressed = images[1];

        if (isSelected()){
            if (pressed != null) {
                mImageView.setImageDrawable(pressed);
            }
        } else {
            if (notPressed != null) {
                mImageView.setImageDrawable(notPressed);
            }
        }
    }


    @Override
    public void setText(String string){

    }

    @Override
    protected LayoutParams getSmallLinearLayout() {
        return new LayoutParams(getDimension(R.dimen.round_round_button_small_width),
                getDimension(R.dimen.round_round_button_small_height));
    }

    @Override
    protected LayoutParams getMediumLinearLayout() {
        return new LayoutParams(getDimension(R.dimen.round_round_button_medium_width),
                getDimension(R.dimen.round_round_button_medium_height));
    }

    @Override
    protected LayoutParams getLargeLinearLayout() {
        return new LayoutParams(getDimension(R.dimen.round_round_button_large_width),
                getDimension(R.dimen.round_round_button_large_height));
    }

}
