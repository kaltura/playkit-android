package com.kaltura.magikapp;

import android.animation.Animator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import com.kaltura.magikapp.magikapp.BackgroundMovementAnimation;



public class SplashFragment extends DialogFragment {

    private ImageView mBackgroundImage;
    private VideoView mBackgroundVideo;
    private Animator mAnimator;
    private String videoUrl;
    private boolean isVideo = false;

    public static SplashFragment newInstance(String url) {
        SplashFragment splashFragment = new SplashFragment();
        splashFragment.videoUrl = url;
        splashFragment.isVideo = !TextUtils.isEmpty(url);
        return splashFragment;
    }

    protected int getLayoutId() {
        return R.layout.fragment_login_main_content;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStyle(STYLE_NO_TITLE, android.R.style.Theme_Material_Light_NoActionBar);
        } else {
            setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBackgroundImage = (ImageView)view.findViewById(R.id.img_background);
        mBackgroundVideo = (VideoView) view.findViewById(R.id.video_background);

    }

    @Override
    public void onDestroyView() {
        if (mBackgroundVideo.isPlaying()) {
            mBackgroundVideo.stopPlayback();
            mBackgroundVideo.setVisibility(View.GONE);
        } else {
            if(mAnimator != null) {mAnimator.end();}
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        showBackground();

    }

    private void showBackground(){
        if (isVideo) {
            mBackgroundVideo.setVisibility(View.VISIBLE);
            try{
                final VideoView videoHolder = mBackgroundVideo;
                videoHolder.setVideoPath(videoUrl);
                videoHolder.setOnPreparedListener (new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setLooping(true);
                        setDimension();
                    }
                });
                videoHolder.start();

            } catch(Exception ex) {

            }
        } else {
            mBackgroundImage.setVisibility(View.VISIBLE);
            mAnimator = BackgroundMovementAnimation.getMoveAnimator(mBackgroundImage, ScreenUtils.getScreenSize(getActivity()).x);
            mAnimator.start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        }
    }

    private void setDimension() {
        // Adjust the size of the video
        // so it fits on the screen
        float videoProportion = getVideoProportion();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float screenProportion = (float) screenHeight / (float) screenWidth;
        android.view.ViewGroup.LayoutParams lp = mBackgroundVideo.getLayoutParams();

        if (videoProportion < screenProportion) {
            lp.height= screenHeight;
            lp.width = (int) ((float) screenHeight / videoProportion);
        } else {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth * videoProportion);
        }
        mBackgroundVideo.setLayoutParams(lp);
    }

    // This method gets the proportion of the video that you want to display.
// I already know this ratio since my video is hardcoded, you can get the
// height and width of your video and appropriately generate  the proportion
//    as :height/width
    private float getVideoProportion(){
        return 1.5f;
    }

}
