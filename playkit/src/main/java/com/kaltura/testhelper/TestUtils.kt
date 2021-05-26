package com.kaltura.testhelper

import android.content.Context
import android.net.Uri
import com.kaltura.android.exoplayer2.Format
import com.kaltura.android.exoplayer2.source.TrackGroup
import com.kaltura.android.exoplayer2.source.TrackGroupArray
import com.kaltura.android.exoplayer2.source.dash.manifest.AdaptationSet
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifestParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.jvm.Throws

open class TestUtils {
    companion object {
        fun parseLocalDashManifest(context: Context, fileName: String): DashManifest {
            val inputStream: InputStream = getInputStream(context, fileName)
            return DashManifestParser().parse(Uri.EMPTY, inputStream)
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
        fun getTrackGroupArrayFromHLSManifest(context: Context, fileName: String) {
            //TODO: Parsing for HLS Manifest
        }

    }
}
