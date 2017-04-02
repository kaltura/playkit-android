package com.kaltura.playkit;

import com.google.android.exoplayer2.metadata.id3.ApicFrame;
import com.google.android.exoplayer2.metadata.id3.CommentFrame;
import com.google.android.exoplayer2.metadata.id3.GeobFrame;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.PrivFrame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.metadata.id3.UrlLinkFrame;

/**
 * Created by anton.afanasiev on 02/04/2017.
 */

public class PKId3Metadata {

    private TextInformationFrame textInformationFrame = null;
    private UrlLinkFrame urlLinkFrame = null;
    private PrivFrame privFrame = null;
    private GeobFrame geobFrame = null;
    private ApicFrame apicFrame = null;
    private CommentFrame commentFrame = null;
    private Id3Frame id3Frame = null;

    public PKId3Metadata setTextInfoFram(TextInformationFrame textInfoFrame) {
        this.textInformationFrame = textInfoFrame;
        return this;
    }

    public PKId3Metadata setUrlLinkFrame(UrlLinkFrame urlLinkFrame) {
        this.urlLinkFrame = urlLinkFrame;
        return this;
    }

    public PKId3Metadata setPrivFrame(PrivFrame privFrame) {
        this.privFrame = privFrame;
        return this;
    }

    public PKId3Metadata setGeobFrame(GeobFrame geobFrame) {
        this.geobFrame = geobFrame;
        return this;
    }

    public PKId3Metadata setApicFrame(ApicFrame apicFrame) {
        this.apicFrame = apicFrame;
        return this;
    }

    public PKId3Metadata setCommentFrame(CommentFrame commentFrame) {
        this.commentFrame = commentFrame;
        return this;
    }

    public PKId3Metadata setId3Frame(Id3Frame id3Frame) {
        this.id3Frame = id3Frame;
        return this;
    }

    public boolean hasTextInfoFrame() {
        return textInformationFrame != null;
    }

    public boolean hasUrlLinkFrame() {
        return urlLinkFrame != null;
    }

    public boolean hasPrivFrame() {
        return privFrame != null;
    }

    public boolean hasGeobFrame() {
        return geobFrame != null;
    }

    public boolean hasApicFrame() {
        return apicFrame != null;
    }

    public boolean hasCommentFrame() {
        return commentFrame != null;
    }

    public boolean hasId3Frame() {
        return id3Frame != null;
    }

    public TextInformationFrame getTextInformationFrame() {
        return textInformationFrame;
    }

    public UrlLinkFrame getUrlLinkFrame() {
        return urlLinkFrame;
    }

    public PrivFrame getPrivFrame() {
        return privFrame;
    }

    public GeobFrame getGeobFrame() {
        return geobFrame;
    }

    public ApicFrame getApicFrame() {
        return apicFrame;
    }

    public CommentFrame getCommentFrame() {
        return commentFrame;
    }

    public Id3Frame getId3Frame() {
        return id3Frame;
    }

    public boolean hasMetadata() {
        return textInformationFrame != null
                || urlLinkFrame != null
                || privFrame != null
                || geobFrame != null
                || apicFrame != null
                || commentFrame != null
                || id3Frame != null;
    }
}
