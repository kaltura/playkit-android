package com.kaltura.playkit.connect;

/**
 * Created by tehilarozin on 15/02/2017.
 */

public class RestrictionError extends ErrorElement {

    public enum Restriction{
        NotAllowed,
        NotEntitled,
        ConcurrencyLimitation,
        Suspended
    }


    public RestrictionError(String message, Restriction restriction) {
        super("RestrictionError", message, 533);
        this.extra = restriction;
    }

    public Restriction getRestriction() {
        return (Restriction) extra;
    }
}
