package com.kaltura.magikapp.magikapp.homepage;

/**
 * Created by vladir on 02/01/2017.
 */

public enum ViewType {
    Player(0), Image(1);

    public int getNum(){
        return num;
    }

    public int num;
    ViewType(int i) {
        num = i;
    }
}
