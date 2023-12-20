package com.kaltura.playkit.player.thumbnail;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.kaltura.androidx.media3.common.ParserException;
import com.kaltura.androidx.media3.extractor.text.SimpleSubtitleDecoder;
import com.kaltura.androidx.media3.extractor.text.Subtitle;
import com.kaltura.androidx.media3.extractor.text.SubtitleDecoderException;
import com.kaltura.androidx.media3.extractor.text.webvtt.WebvttCssStyle;
import com.kaltura.androidx.media3.extractor.text.webvtt.WebvttCueInfo;
import com.kaltura.androidx.media3.extractor.text.webvtt.WebvttCueParser;
import com.kaltura.androidx.media3.extractor.text.webvtt.WebvttParserUtil;
import com.kaltura.androidx.media3.common.util.ParsableByteArray;

import java.util.ArrayList;
import java.util.List;

public final class PKThumbnailsWebVttDecoder extends SimpleSubtitleDecoder {

    private static final int EVENT_NONE = -1;
    private static final int EVENT_END_OF_FILE = 0;
    private static final int EVENT_COMMENT = 1;
    private static final int EVENT_STYLE_BLOCK = 2;
    private static final int EVENT_CUE = 3;

    private static final String COMMENT_START = "NOTE";
    private static final String STYLE_START = "STYLE";

    private final ParsableByteArray parsableWebvttData;

    public PKThumbnailsWebVttDecoder() {
        super("WebvttDecoder");
        parsableWebvttData = new ParsableByteArray();
    }

    @Override
    public Subtitle decode(byte[] bytes, int length, boolean reset)
            throws SubtitleDecoderException {
        parsableWebvttData.reset(bytes, length);
        List<WebvttCssStyle> definedStyles = new ArrayList<>();

        // Validate the first line of the header, and skip the remainder.
        try {
            WebvttParserUtil.validateWebvttHeaderLine(parsableWebvttData);
        } catch (ParserException e) {
            throw new SubtitleDecoderException(e);
        }
        while (!TextUtils.isEmpty(parsableWebvttData.readLine())) {}

        int event;
        List<WebvttCueInfo> cueInfos = new ArrayList<>();
        while ((event = getNextEvent(parsableWebvttData)) != EVENT_END_OF_FILE) {
            if (event == EVENT_COMMENT) {
                skipComment(parsableWebvttData);
            } else if (event == EVENT_STYLE_BLOCK) {
                if (!cueInfos.isEmpty()) {
                    throw new SubtitleDecoderException("A style block was found after the first cue.");
                }
                parsableWebvttData.readLine(); // Consume the "STYLE" header.
            } else if (event == EVENT_CUE) {
                @Nullable
                WebvttCueInfo cueInfo = WebvttCueParser.parseCue(parsableWebvttData, definedStyles);
                if (cueInfo != null) {
                    cueInfos.add(cueInfo);
                }
            }
        }
        return new PKWebvttSubtitle(cueInfos);
    }

    /**
     * Positions the input right before the next event, and returns the kind of event found. Does not
     * consume any data from such event, if any.
     *
     * @return The kind of event found.
     */
    private static int getNextEvent(ParsableByteArray parsableWebvttData) {
        int foundEvent = EVENT_NONE;
        int currentInputPosition = 0;
        while (foundEvent == EVENT_NONE) {
            currentInputPosition = parsableWebvttData.getPosition();
            String line = parsableWebvttData.readLine();
            if (line == null) {
                foundEvent = EVENT_END_OF_FILE;
            } else if (STYLE_START.equals(line)) {
                foundEvent = EVENT_STYLE_BLOCK;
            } else if (line.startsWith(COMMENT_START)) {
                foundEvent = EVENT_COMMENT;
            } else {
                foundEvent = EVENT_CUE;
            }
        }
        parsableWebvttData.setPosition(currentInputPosition);
        return foundEvent;
    }

    private static void skipComment(ParsableByteArray parsableWebvttData) {
        while (!TextUtils.isEmpty(parsableWebvttData.readLine())) {}
    }
}
