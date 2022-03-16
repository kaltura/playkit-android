package com.kaltura.playkit.drm

import android.net.Uri
import android.os.Build
import com.kaltura.android.exoplayer2.Format
import com.kaltura.android.exoplayer2.drm.DrmInitData
import com.kaltura.android.exoplayer2.extractor.mp4.PsshAtomUtil
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist
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
        val segmentUrl: String?
        val inputStreamLocalPath = BufferedInputStream(FileInputStream(localPath))
        val masterPlaylist: HlsMultivariantPlaylist = HlsPlaylistParser().parse(Uri.parse(localPath), inputStreamLocalPath) as HlsMultivariantPlaylist

        val variant = masterPlaylist.variants
        if (variant.isNotEmpty()) {
            format = variant[0].format
            segmentUrl = variant[0].url.toString()
        } else {
            throw IOException("At least one video representation is required")
        }

        val isMediaPlaylist = BufferedInputStream(FileInputStream(segmentUrl))
        val mediaPlaylist: HlsMediaPlaylist = HlsPlaylistParser().parse(Uri.parse(localPath), isMediaPlaylist) as HlsMediaPlaylist
        if (mediaPlaylist.segments.isNotEmpty()) {
            mediaPlaylist.segments[0].drmInitData?.let { drmData ->
                hasContentProtection = true
                drmInitData = drmData
            }
        }

        if (drmInitData == null) {
            log.i("no content protection found")
            return this
        }

        drmInitData?.let {
            val schemeInitData = getWidevineSchemeData(drmInitData)
            schemeInitData?.let {
                hlsWidevineInitData = it.data
            }
        }

        return this
    }

    fun getWidevineInitData(): ByteArray? = hlsWidevineInitData

    private fun getWidevineSchemeData(drmInitData: DrmInitData?): DrmInitData.SchemeData? {
        val widevineUUID = MediaSupport.WIDEVINE_UUID
        if (drmInitData == null) {
            log.e("No PSSH in media")
            return null
        }

        var schemeData: DrmInitData.SchemeData? = null
        for (i in 0 until drmInitData.schemeDataCount) {
            if (drmInitData[i].matches(widevineUUID)) {
                schemeData = drmInitData[i]
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

