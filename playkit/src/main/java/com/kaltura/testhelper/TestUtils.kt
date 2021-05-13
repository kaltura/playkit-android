package com.kaltura.testhelper

import android.content.Context
import android.net.Uri
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifestParser
import java.io.IOException
import java.io.InputStream
import kotlin.jvm.Throws

open class TestUtils {
    companion object {
        fun parseLocalManifest(context: Context, fileName: String): DashManifest {
            val inputStream: InputStream = getInputStream(context, fileName)
            return DashManifestParser().parse(Uri.EMPTY, inputStream)
        }

        @Throws(IOException::class)
        fun getInputStream(context: Context, fileName: String): InputStream {
            return context.resources.assets.open(fileName)
        }
    }
}