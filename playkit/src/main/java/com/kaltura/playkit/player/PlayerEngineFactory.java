package com.kaltura.playkit.player;

import android.content.Context;

import com.kaltura.playkit.PKMediaFormat;

/**
 * Created by anton.afanasiev on 19/07/2017.
 */

class PlayerEngineFactory {

    static PlayerEngineType getDesiredPlayerType(PKMediaFormat mediaFormat, boolean is360Supported) {
        if (is360Supported) {
            return PlayerEngineType.VR_PLAYER;
        }

        if (mediaFormat != PKMediaFormat.wvm) {
            return PlayerEngineType.EXOPLAYER;
        } else {
            return PlayerEngineType.MEDIA_PLAYER;
        }
    }

    static PlayerEngine getPlayerEngine(Context context, PlayerEngineType playerEngineType) throws PlayerInitializationException {

        switch (playerEngineType) {
            case MEDIA_PLAYER:
                return new MediaPlayerWrapper(context);
            case VR_PLAYER:
                //Load DefaultVRPlayerFactory.java from playkitvr library.
                Class<?> clazz;
                try {
                    clazz = Class.forName("com.kaltura.playkitvr.DefaultVRPlayerFactory");
                } catch (ClassNotFoundException e) {
                    throw new PlayerInitializationException("Could not find com.kaltura.playkitvr.DefaultVRPlayerFactory class." +
                            " Please check if com.kaltura.playkitvr library exist in project structure", e);
                }

                VRPlayerFactory vrPlayerFactory;
                try {
                    vrPlayerFactory = (VRPlayerFactory) clazz.newInstance();
                } catch (InstantiationException e) {
                    throw new PlayerInitializationException("Failed to create new instance of VRPlayerFactory", e);
                } catch (IllegalAccessException e) {
                    throw new PlayerInitializationException("Illegal package access to VRPlayerFactory. Failed to create.", e);
                }

                ExoPlayerWrapper exoWrapper = new ExoPlayerWrapper(context);
                return vrPlayerFactory.newInstance(context, exoWrapper);

            default:
                return new ExoPlayerWrapper(context);
        }
    }

    static class PlayerInitializationException extends Exception {
        PlayerInitializationException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }
}
