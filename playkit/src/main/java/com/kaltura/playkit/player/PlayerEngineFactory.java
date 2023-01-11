package com.kaltura.playkit.player;

import android.content.Context;

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.player.vr.VRPlayerFactory;

/**
 * Created by anton.afanasiev on 25/03/2018.
 */

class PlayerEngineFactory {

    static PlayerEngineType selectPlayerType(PKMediaFormat mediaFormat, boolean is360Supported) {

        if (is360Supported) {
            return PlayerEngineType.VRPlayer;
        }

        if (mediaFormat == PKMediaFormat.wvm) {
            return PlayerEngineType.MediaPlayer;
        }

        return PlayerEngineType.Exoplayer;
    }

    static PlayerEngine initializePlayerEngine(Context context, PlayerEngineType engineType, PlayerSettings playerSettings, PlayerView rootPlayerView) throws PlayerInitializationException {

        switch (engineType) {
            case MediaPlayer:
                return new MediaPlayerWrapper(context);
            case VRPlayer:
                //Load DefaultVRPlayerFactory.java from playkitvr library.
                Class<?> clazz;
                VRPlayerFactory vrPlayerFactory;
                try {
                    clazz = Class.forName("com.kaltura.playkitvr.DefaultVRPlayerFactory");
                    vrPlayerFactory = (VRPlayerFactory) clazz.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new PlayerInitializationException("Could not find com.kaltura.playkitvr.DefaultVRPlayerFactory class." +
                            " Please check if com.kaltura.playkitvr library exist in project structure", e);
                } catch (InstantiationException e) {
                    throw new PlayerInitializationException("Failed to create new instance of VRPlayerFactory", e);
                } catch (IllegalAccessException e) {
                    throw new PlayerInitializationException("Illegal package access to VRPlayerFactory. Failed to create.", e);
                }

                //Initialize ExoplayerWrapper for video playback which will use VRView for render purpose.
                ExoPlayerWrapper exoWrapper = new ExoPlayerWrapper(context, vrPlayerFactory.newVRViewInstance(context), playerSettings, rootPlayerView);
                return vrPlayerFactory.newInstance(context, exoWrapper, playerSettings.getVRSettings() != null ? playerSettings.getVRSettings() : null);

            default:
                return new ExoPlayerWrapper(context, playerSettings, rootPlayerView);
        }
    }

    static class PlayerInitializationException extends Exception {

        PlayerInitializationException(String message, Throwable throwable) {
            super(message, throwable);
        }

    }
}
