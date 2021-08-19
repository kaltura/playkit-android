package com.kaltura.playkit.drm

import android.net.Uri
import android.os.Build
import com.kaltura.android.exoplayer2.Format
import com.kaltura.android.exoplayer2.drm.DrmInitData
import com.kaltura.android.exoplayer2.extractor.mp4.PsshAtomUtil
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsPlaylistParser
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.player.MediaSupport
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException

class SimpleHlsParser {

    private val log = PKLog.get("SimpleHlsParser")

    @JvmField
    var format: Format? = null
    @JvmField
    var hlsWidevineInitData: ByteArray? = null
    @JvmField
    var hasContentProtection: Boolean = false
    @JvmField
    var drmInitData: DrmInitData? = null

    @Throws(IOException::class)
    fun parse(localPath: String): SimpleHlsParser {
        var segmentUrl: String? = null
        val isMasterPlaylist = BufferedInputStream(FileInputStream(localPath))
        val masterPlaylist: HlsMasterPlaylist = HlsPlaylistParser().parse(Uri.parse(localPath), isMasterPlaylist) as HlsMasterPlaylist

        masterPlaylist.variants?.let { variant ->
            if (variant.size > 0) {
                format = variant[0].format

                variant[0].url.lastPathSegment?.let { pathSegment ->
                    if (pathSegment.contains("media")) {
                        //TODO: Is it uniform for all the medias or not (it is standard or not)
                        val originLastPathSegment = pathSegment.replace("media", "origin")
                        val variantUrl = variant[0].url.toString()
                        val leftOverVariantUrl = variantUrl.substring(0, variantUrl.lastIndexOf("/") + 1)
                        segmentUrl = leftOverVariantUrl.plus(originLastPathSegment)
                    }
                }
            } else {
                throw IOException("At least one video representation is required")
            }
        }

        segmentUrl?.let {
            val isMediaPlaylist = BufferedInputStream(FileInputStream(it))
            val mediaPlaylist: HlsMediaPlaylist = HlsPlaylistParser().parse(Uri.parse(localPath), isMediaPlaylist) as HlsMediaPlaylist
            if (mediaPlaylist.segments.size > 0) {
                mediaPlaylist.segments[0].drmInitData?.let { drmData ->
                    hasContentProtection = true
                    drmInitData = drmData
                }
            }
        }

        if (drmInitData == null) {
            log.i("no content protection found")
            return this
        }

        drmInitData?.let {
            val schemeInitData = getWidevineInitData(drmInitData)
            schemeInitData?.let {
                hlsWidevineInitData = it.data
            }
        }

        return this
    }

    fun getWidevineInitData(): ByteArray? = hlsWidevineInitData

    private fun getWidevineInitData(drmInitData: DrmInitData?): DrmInitData.SchemeData? {
        val widevineUUID = MediaSupport.WIDEVINE_UUID
        if (drmInitData == null) {
            log.e("No PSSH in media")
            return null
        }

        var schemeData: DrmInitData.SchemeData? = null
        for (i in 0 until drmInitData!!.schemeDataCount) {
            if (drmInitData!![i] != null && drmInitData!![i].matches(widevineUUID)) {
                schemeData = drmInitData!![i]
            }
        }

        if (schemeData == null) {
            log.e("No Widevine PSSH in media")
            return null
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Prior to L the Widevine CDM required data to be extracted from the PSSH atom.
            val psshData = PsshAtomUtil.parseSchemeSpecificData(schemeData.data!!, widevineUUID)
            if (psshData != null) {
                schemeData = DrmInitData.SchemeData(widevineUUID, schemeData.mimeType, psshData)
            }
        }
        return schemeData
    }

}

