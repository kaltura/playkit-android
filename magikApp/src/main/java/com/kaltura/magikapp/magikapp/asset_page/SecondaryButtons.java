package com.kaltura.magikapp.magikapp.asset_page;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.kaltura.magikapp.R;

/**
 * Created by vladir on 28/11/2016.
 */

public class SecondaryButtons extends RelativeLayout implements View.OnClickListener {

    private BaseRoundButton mComments;
    private ExpandingRoundButton mLike;
    private BaseRoundButton mShare;
    private BaseRoundButton mFavorites;

    private SecondaryButtonClickListener mListener;

    public BaseRoundButton getButton(SecondaryButtonType type){
        BaseRoundButton button = null;
        switch (type){
            case Comment:
                button = mComments;
                break;
            case Favorites:
                button = mFavorites;
                break;
            case Like:
                button = mLike;
                break;
            case Share:
                button = mShare;
                break;
        }
        return button;
    }

    public void init(AssetInfo asset) {
        SecondaryButtonsInitiatorHelper.init(this, asset);
    }

    public enum SecondaryButtonType {
        Comment,
        Share,
        Favorites,
        Like
    }

    public interface SecondaryButtonClickListener {
        void onClick(SecondaryButtonType type);
    }

    public void setButtonsOnClickListener(SecondaryButtonClickListener listener){
        mListener = listener;
    }

    public SecondaryButtons(Context context) {
        super(context);
        init(context);
    }

    public SecondaryButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SecondaryButtons(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        View root = inflate(context, R.layout.media_page_secondary_buttons_layout, this);
        mComments = (BaseRoundButton) root.findViewById(R.id.helen_round_button_comment_button);
        mComments.setOnClickListener(this);
        mShare = (BaseRoundButton) root.findViewById(R.id.helen_round_button_share_button);
        mShare.setOnClickListener(this);
        mFavorites = (BaseRoundButton) root.findViewById(R.id.helen_round_button_favorites_button);
        mFavorites.setOnClickListener(this);
        mLike = (ExpandingRoundButton) root.findViewById(R.id.helen_round_button_like_button);
        mLike.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.helen_round_button_comment_button:
                if (mListener != null) {
                    mListener.onClick(SecondaryButtonType.Comment);
                }
                break;
            case R.id.helen_round_button_share_button:
                if (mListener != null) {
                    mListener.onClick(SecondaryButtonType.Share);
                }
                break;
            case R.id.helen_round_button_favorites_button:
                if (mListener != null) {
                    mListener.onClick(SecondaryButtonType.Favorites);
                }
                break;
            case R.id.helen_round_button_like_button:
                if (mListener != null) {
                    mListener.onClick(SecondaryButtonType.Like);
                }
                break;
        }
    }

    public void setButtonSelected(SecondaryButtonType type, boolean isPressed){
        getButton(type).setSelected(isPressed);
    }
}




















