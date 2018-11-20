package com.kaltura.playkit.player;

import android.content.Context;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.player.vr.VRPlayerFactory;

/**
 * Created by anton.afanasiev on 25/03/2018.
 */

class PlayerEngineFactory {
    private static final PKLog log = PKLog.get("PlayerEngineFactory");

    static PlayerEngineType selectPlayerType(PKMediaFormat mediaFormat, boolean is360Supported) {

        if (is360Supported) {
            return PlayerEngineType.VRPlayer;
        }

        if (mediaFormat == PKMediaFormat.wvm) {
            return PlayerEngineType.MediaPlayer;
        }

        return PlayerEngineType.Exoplayer;
    }

    static PlayerEngine initializePlayerEngine(Context context, PlayerEngineType engineType, PlayerSettings playerSettings) throws PlayerInitializationException {

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
                    String errorClassNotFoundException = "Could not find com.kaltura.playkitvr.DefaultVRPlayerFactory class." +
                            " Please check if com.kaltura.playkitvr library exist in project structure";
                    log.e(errorClassNotFoundException);
                    throw new PlayerInitializationException(errorClassNotFoundException, e);
                } catch (InstantiationException e) {
                    String errorInstantiationException = "Failed to create new instance of VRPlayerFactory";
                    log.e(errorInstantiationException);
                    throw new PlayerInitializationException(errorInstantiationException, e);
                } catch (IllegalAccessException e) {
                    String errorIllegalAccessException = "Illegal package access to VRPlayerFactory. Failed to create.";
                    log.e(errorIllegalAccessException);
                    throw new PlayerInitializationException(errorIllegalAccessException, e);
                }

                //Initialize ExoplayerWrapper for video playback which will use VRView for render purpose.
                ExoPlayerWrapper exoWrapper = new ExoPlayerWrapper(context, vrPlayerFactory.newVRViewInstance(context), playerSettings);
                return vrPlayerFactory.newInstance(context, exoWrapper);
            default:
                return new ExoPlayerWrapper(context, playerSettings);
        }
    }

    static class PlayerInitializationException extends Exception {

        PlayerInitializationException(String message, Throwable throwable) {
            super(message, throwable);
        }

    }
}
