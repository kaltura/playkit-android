package com.kaltura.magikapp.magikapp.core;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by tehilarozin on 01/06/2016.
 *
 * provides access to component needed by "plugin" components
 */
public interface PluginProvider {

    AppCompatActivity getActivity();

    Context getContext();

}
