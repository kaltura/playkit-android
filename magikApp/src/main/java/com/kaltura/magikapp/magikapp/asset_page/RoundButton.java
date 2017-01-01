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
                notPressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_like_outline, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_like_fill, null);
                break;
            case Comment:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_comment_outline, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_comment_fill, null);
                break;
            case Favorites:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_add_favorits_outline, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_added_favorits_fill, null);
                break;
            case Share:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_share_outline, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_share_outline, null);
                break;
            case Play:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null);
                break;
            case Download:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_download, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_downlaod_finished, null);
                break;
            case MoreOptions:
                notPressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_more_options, null);
                pressed = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_more_options, null);
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
    public void setText(String text) {

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
