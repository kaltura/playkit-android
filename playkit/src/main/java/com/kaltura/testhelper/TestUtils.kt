package com.kaltura.testhelper

import android.content.Context
import android.net.Uri
import com.kaltura.android.exoplayer2.C
import com.kaltura.android.exoplayer2.Format
import com.kaltura.android.exoplayer2.metadata.Metadata
import com.kaltura.android.exoplayer2.source.TrackGroup
import com.kaltura.android.exoplayer2.source.TrackGroupArray
import com.kaltura.android.exoplayer2.source.dash.manifest.AdaptationSet
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsPlaylistParser
import com.kaltura.android.exoplayer2.util.MimeTypes
import com.kaltura.android.exoplayer2.util.Util
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList

open class TestUtils {
    companion object {
        fun parseLocalDashManifest(context: Context, fileName: String): DashManifest {
            val inputStream: InputStream = getInputStream(context, fileName)
            return DashManifestParser().parse(Uri.EMPTY, inputStream)
        }

        fun parseLocalHlsManifest(context: Context, fileName: String): HlsMasterPlaylist {
            val inputStream: InputStream = getInputStream(context, fileName)
            return (HlsPlaylistParser().parse(Uri.EMPTY, inputStream)) as HlsMasterPlaylist
        }

        fun getManifestString(context: Context, fileName: String) : String {
            val inputStream = getInputStream(context, fileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val manifestString = StringBuilder()
            bufferedReader.useLines { lines -> lines.forEach { manifestString.append(it)} }
            return manifestString.toString()
        }

        @Throws(IOException::class)
        fun getInputStream(context: Context, fileName: String): InputStream {
            return context.resources.assets.open(fileName)
        }

        @Throws(IOException::class)
        fun getTrackGroupArrayFromDashManifest(context: Context, fileName: String): TrackGroupArray {
            val dashManifest: DashManifest = parseLocalDashManifest(context, fileName)
            val adaptationSets: List<AdaptationSet> = dashManifest.getPeriod(0).adaptationSets

            val trackGroups: ArrayList<TrackGroup> = arrayListOf()

            for ((adaptationIndex, adaptationValue) in adaptationSets.withIndex()) {
                val format: ArrayList<Format> = arrayListOf()
                for ((representationIndex, representationValue) in adaptationValue.representations.withIndex()) {
                    format.add(adaptationSets[adaptationIndex].representations[representationIndex].format)
                }
                trackGroups.add(TrackGroup(*format.toTypedArray()))
            }

            return TrackGroupArray(*trackGroups.toTypedArray())
        }

        @Throws(IOException::class)
        fun getTrackGroupArrayFromHlsManifest(context: Context, fileName: String): TrackGroupArray {
            val hlsMasterPlaylist = parseLocalHlsManifest(context, fileName)
            var trackGroups: ArrayList<TrackGroup> = arrayListOf()
            //MimeTypes.getMediaMimeType(Util.getCodecsOfType(trackGroups.get(2).getFormat(0).codecs, C.TRACK_TYPE_VIDEO))
            trackGroups = extractHlsVideoTracks(hlsMasterPlaylist, trackGroups)
            trackGroups = extractHlsAudioTracks(hlsMasterPlaylist, trackGroups)

            if (hlsMasterPlaylist.subtitles.isNotEmpty()) {
                for ((subtitlesIndex, subtitlesValue) in hlsMasterPlaylist.subtitles.withIndex()) {
                    trackGroups.add(TrackGroup(subtitlesValue.format))
                }
            }

            return TrackGroupArray(*trackGroups.toTypedArray())
        }

        private fun deriveVideoFormat(variantFormat: Format): Format {
            val codecs: String? = Util.getCodecsOfType(variantFormat.codecs, C.TRACK_TYPE_VIDEO)
            val sampleMimeType: String? = MimeTypes.getMediaMimeType(codecs)
            return Format.Builder()
                    .setId(variantFormat.id)
                    .setLabel(variantFormat.label)
                    .setContainerMimeType(variantFormat.containerMimeType)
                    .setSampleMimeType(sampleMimeType ?: "video/avc")
                    .setCodecs(codecs)
                    .setMetadata(variantFormat.metadata)
                    .setAverageBitrate(variantFormat.averageBitrate)
                    .setPeakBitrate(variantFormat.peakBitrate)
                    .setWidth(variantFormat.width)
                    .setHeight(variantFormat.height)
                    .setFrameRate(variantFormat.frameRate)
                    .setSelectionFlags(variantFormat.selectionFlags)
                    .setRoleFlags(variantFormat.roleFlags)
                    .build()
        }

        private fun deriveAudioFormat(variantFormat: Format, mediaTagFormat: Format?, isPrimaryTrackInVariant: Boolean): Format {
            val codecs: String?
            val metadata: Metadata?
            var channelCount = Format.NO_VALUE
            var selectionFlags = 0
            var roleFlags = 0
            var language: String? = null
            var label: String? = null
            if (mediaTagFormat != null) {
                codecs = mediaTagFormat.codecs
                metadata = mediaTagFormat.metadata
                channelCount = mediaTagFormat.channelCount
                selectionFlags = mediaTagFormat.selectionFlags
                roleFlags = mediaTagFormat.roleFlags
                language = mediaTagFormat.language
                label = mediaTagFormat.label
            } else {
                codecs = Util.getCodecsOfType(variantFormat.codecs, C.TRACK_TYPE_AUDIO)
                metadata = variantFormat.metadata
                if (isPrimaryTrackInVariant) {
                    channelCount = variantFormat.channelCount
                    selectionFlags = variantFormat.selectionFlags
                    roleFlags = variantFormat.roleFlags
                    language = variantFormat.language
                    label = variantFormat.label
                }
            }
            val sampleMimeType: String? = MimeTypes.getMediaMimeType(codecs)
            val averageBitrate = if (isPrimaryTrackInVariant) variantFormat.averageBitrate else Format.NO_VALUE
            val peakBitrate = if (isPrimaryTrackInVariant) variantFormat.peakBitrate else Format.NO_VALUE
            return Format.Builder()
                    .setId(variantFormat.id)
                    .setLabel(label)
                    .setContainerMimeType(variantFormat.containerMimeType)
                    .setSampleMimeType(sampleMimeType ?: "audio/mp4")
                    .setCodecs(codecs)
                    .setMetadata(metadata)
                    .setAverageBitrate(averageBitrate)
                    .setPeakBitrate(peakBitrate)
                    .setChannelCount(channelCount)
                    .setSelectionFlags(selectionFlags)
                    .setRoleFlags(roleFlags)
                    .setLanguage(language)
                    .build()
        }

        private fun extractHlsVideoTracks(masterPlaylist: HlsMasterPlaylist, trackGroups: ArrayList<TrackGroup>): ArrayList<TrackGroup> {
            val variantTypes = IntArray(masterPlaylist.variants.size)
            var videoVariantCount = 0
            var audioVariantCount = 0
            for (i in 0 until masterPlaylist.variants.size) {
                val variant: HlsMasterPlaylist.Variant = masterPlaylist.variants.get(i)
                val format: Format = variant.format
                if (format.height > 0 || Util.getCodecsOfType(format.codecs, C.TRACK_TYPE_VIDEO) != null) {
                    variantTypes[i] = C.TRACK_TYPE_VIDEO
                    videoVariantCount++
                } else if (Util.getCodecsOfType(format.codecs, C.TRACK_TYPE_AUDIO) != null) {
                    variantTypes[i] = C.TRACK_TYPE_AUDIO
                    audioVariantCount++
                } else {
                    variantTypes[i] = C.TRACK_TYPE_UNKNOWN
                }
            }
            var useVideoVariantsOnly = false
            var useNonAudioVariantsOnly = false
            var selectedVariantsCount = variantTypes.size
            if (videoVariantCount > 0) {
                // We've identified some variants as definitely containing video. Assume variants within the
                // master playlist are marked consistently, and hence that we have the full set. Filter out
                // any other variants, which are likely to be audio only.
                useVideoVariantsOnly = true
                selectedVariantsCount = videoVariantCount
            } else if (audioVariantCount < variantTypes.size) {
                // We've identified some variants, but not all, as being audio only. Filter them out to leave
                // the remaining variants, which are likely to contain video.
                useNonAudioVariantsOnly = true
                selectedVariantsCount = variantTypes.size - audioVariantCount
            }
            val selectedPlaylistUrls = arrayOfNulls<Uri>(selectedVariantsCount)
            val selectedPlaylistFormats = arrayOfNulls<Format>(selectedVariantsCount)
            val selectedVariantIndices = IntArray(selectedVariantsCount)
            var outIndex = 0
            for (i in 0 until masterPlaylist.variants.size) {
                if ((!useVideoVariantsOnly || variantTypes[i] == C.TRACK_TYPE_VIDEO)
                        && (!useNonAudioVariantsOnly || variantTypes[i] != C.TRACK_TYPE_AUDIO)) {
                    val variant: HlsMasterPlaylist.Variant = masterPlaylist.variants.get(i)
                    selectedPlaylistUrls[outIndex] = variant.url
                    selectedPlaylistFormats[outIndex] = variant.format
                    selectedVariantIndices[outIndex++] = i
                }
            }
            val codecs = selectedPlaylistFormats[0]!!.codecs
            val numberOfVideoCodecs = Util.getCodecCountOfType(codecs, C.TRACK_TYPE_VIDEO)
            val numberOfAudioCodecs = Util.getCodecCountOfType(codecs, C.TRACK_TYPE_AUDIO)
            val codecsStringAllowsChunklessPreparation = numberOfAudioCodecs <= 1 && numberOfVideoCodecs <= 1 && numberOfAudioCodecs + numberOfVideoCodecs > 0
            // if (codecsStringAllowsChunklessPreparation) {
            if (numberOfVideoCodecs > 0) {
                val videoFormats = ArrayList<Format>()
                for (index in 0 until selectedVariantsCount) {
                    videoFormats.add(deriveVideoFormat(selectedPlaylistFormats[index]!!))
                }
                trackGroups.add(TrackGroup(*videoFormats.toTypedArray()))
                if (numberOfAudioCodecs > 0
                        && (masterPlaylist.muxedAudioFormat != null || masterPlaylist.audios.isEmpty())) {
                    trackGroups.add(TrackGroup(deriveAudioFormat(selectedPlaylistFormats[0]!!, masterPlaylist.muxedAudioFormat, false)))
                }
                masterPlaylist.muxedCaptionFormats?.let { ccFormats ->
                    for (i in ccFormats.indices) {
                        trackGroups.add(TrackGroup(ccFormats[i]))
                    }
                }
            } else  /* numberOfAudioCodecs > 0 */ {
                // Variants only contain audio.
                val audioFormats = ArrayList<Format>()
                for (index in 0 until selectedVariantsCount) {
                    audioFormats.add(deriveAudioFormat(selectedPlaylistFormats[index]!!, masterPlaylist.muxedAudioFormat, true))
                }
                trackGroups.add(TrackGroup(*audioFormats.toTypedArray()))
            }
            val id3TrackGroup = TrackGroup(
                    Format.Builder()
                            .setId("ID3")
                            .setSampleMimeType(MimeTypes.APPLICATION_ID3)
                            .build())
            trackGroups.add(id3TrackGroup)
            //  }

            return trackGroups
        }

        private fun extractHlsAudioTracks(masterPlaylist: HlsMasterPlaylist, trackGroups: ArrayList<TrackGroup>): ArrayList<TrackGroup> {
            val scratchPlaylistFormats = ArrayList<Format>( /* initialCapacity= */masterPlaylist.audios.size)
            val alreadyGroupedNames = HashSet<String>()
            for (renditionByNameIndex in masterPlaylist.audios.indices) {
                val name: String = masterPlaylist.audios[renditionByNameIndex].name
                if (!alreadyGroupedNames.add(name)) {
                    // This name already has a corresponding group.
                    continue
                }
                scratchPlaylistFormats.clear()
                // Group all renditions with matching name.
                for (renditionIndex in masterPlaylist.audios.indices) {
                    if (Util.areEqual(name, masterPlaylist.audios[renditionIndex].name)) {
                        val rendition: HlsMasterPlaylist.Rendition = masterPlaylist.audios[renditionIndex]
                        scratchPlaylistFormats.add(rendition.format)
                    }
                }
            }

            if (scratchPlaylistFormats.size > 0) {
                trackGroups.add(TrackGroup(*scratchPlaylistFormats.toTypedArray()))
            }
            return trackGroups
        }
    }
}
