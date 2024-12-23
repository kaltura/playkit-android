/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.player.metadata;

import com.kaltura.androidx.media3.common.Metadata;
import com.kaltura.androidx.media3.extractor.metadata.emsg.EventMessage;
import com.kaltura.androidx.media3.extractor.metadata.id3.ApicFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.BinaryFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.ChapterFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.ChapterTocFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.CommentFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.GeobFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.PrivFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.TextInformationFrame;
import com.kaltura.androidx.media3.extractor.metadata.id3.UrlLinkFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by anton.afanasiev on 09/04/2017.
 */

public class MetadataConverter {

    public static List<PKMetadata> convert(Metadata metadata) {

        PKMetadata pkMetadataEntry;
        List<PKMetadata> convertedMetadata = new ArrayList<>();

        for (int i = 0; i < metadata.length(); i++) {
            pkMetadataEntry = convert(metadata.get(i));
            if (pkMetadataEntry != null) {
                convertedMetadata.add(pkMetadataEntry);
            }
        }

        return convertedMetadata;
    }

    private static PKMetadata convert(Metadata.Entry entry) {

        if (entry instanceof ApicFrame) {
            return convert((ApicFrame) entry);
        } else if (entry instanceof BinaryFrame) {
            return convert((BinaryFrame) entry);
        } else if (entry instanceof ChapterFrame) {
            return convert((ChapterFrame) entry);
        } else if (entry instanceof ChapterTocFrame) {
            return convert((ChapterTocFrame) entry);
        } else if (entry instanceof CommentFrame) {
            return convert((CommentFrame) entry);
        } else if (entry instanceof GeobFrame) {
            return convert((GeobFrame) entry);
        } else if (entry instanceof PrivFrame) {
            return convert((PrivFrame) entry);
        } else if (entry instanceof TextInformationFrame) {
            return convert((TextInformationFrame) entry);
        } else if (entry instanceof UrlLinkFrame) {
            return convert((UrlLinkFrame) entry);
        } else if (entry instanceof EventMessage) {
            return convert((EventMessage) entry);
        }

        return null;
    }


    private static PKMetadata convert(ApicFrame frame) {
        return new PKApicFrame(
                frame.id,
                frame.mimeType,
                frame.description,
                frame.pictureType,
                frame.pictureData
        );
    }

    private static PKMetadata convert(BinaryFrame frame) {
        return new PKBinaryFrame(
                frame.id,
                frame.data
        );
    }

    private static PKMetadata convert(ChapterFrame frame) {

        List<PKMetadata> subFrames = new ArrayList<>();

        for (int i = 0; i < frame.getSubFrameCount(); i++) {
            PKMetadata pkMetadataEntry = convert(frame.getSubFrame(i));
            subFrames.add(pkMetadataEntry);
        }

        return new PKChapterFrame(
                frame.id,
                frame.chapterId,
                frame.startTimeMs,
                frame.endTimeMs,
                frame.startOffset,
                frame.endOffset,
                subFrames
        );
    }


    private static PKMetadata convert(ChapterTocFrame frame) {
        List<PKMetadata> subFrames = new ArrayList<>();
        List<String> children = Arrays.asList(frame.children);

        for (int i = 0; i < frame.getSubFrameCount(); i++) {
            PKMetadata pkMetadataEntry = convert(frame.getSubFrame(i));
            subFrames.add(pkMetadataEntry);
        }

        return new PKChapterTocFrame(
                frame.id,
                frame.elementId,
                frame.isRoot,
                frame.isOrdered,
                children,
                subFrames
        );
    }

    private static PKMetadata convert(CommentFrame frame) {
        return new PKCommentFrame(
                frame.id,
                frame.language,
                frame.description,
                frame.text
        );
    }

    private static PKMetadata convert(GeobFrame frame) {
        return new PKGeobFrame(
                frame.id,
                frame.mimeType,
                frame.filename,
                frame.description,
                frame.data
        );
    }

    private static PKMetadata convert(PrivFrame frame) {
        return new PKPrivFrame(
                frame.id,
                frame.owner,
                frame.privateData
        );
    }

    private static PKMetadata convert(TextInformationFrame frame) {
        return new PKTextInformationFrame(
                frame.id,
                frame.description,
                frame.value
        );
    }

    private static PKMetadata convert(UrlLinkFrame frame) {
        return new PKUrlLinkFrame(
                frame.id,
                frame.description,
                frame.url
        );
    }

    private static PKMetadata convert(EventMessage frame) {
        return new PKEventMessage(
                frame.schemeIdUri,
                frame.value,
                frame.durationMs,
                frame.id,
                frame.messageData
        );
    }
}
