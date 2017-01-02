package com.kaltura.magikapp.magikapp.asset_page;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

/**
 * Created by vladir on 05/12/2016.
 */

public class RoundButtonUtils {

    /**
     * images {pressed, not pressed}
     ø     * @param images
     */
    public static StateListDrawable getStateDrawableForImageImages(Drawable[] images){
        Drawable pressed = images[0];
        Drawable notPressed = images[1];

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_pressed}, pressed);
        states.addState(new int[] {android.R.attr.state_focused}, pressed);
        states.addState(new int[] {android.R.attr.state_selected}, pressed);
        states.addState(new int[] { }, notPressed);
        return states;
    }
}
