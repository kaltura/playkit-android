package com.kaltura.playkit;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Noam Tamim @ Kaltura on 13/10/2016.
 */

public class Utils {
    private static final String TAG = "Utils";

    public static String readAssetToString(Context context, String asset) {
        try {
            InputStream assetStream = context.getAssets().open(asset);
            return fullyReadInputStream(assetStream, 1024*1024).toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed reading asset " + asset, e);
            return null;
        }
    }

    @NonNull
    public static ByteArrayOutputStream fullyReadInputStream(InputStream inputStream, int byteLimit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte data[] = new byte[1024];
        int count;

        while ((count = inputStream.read(data)) != -1) {
            int maxCount = byteLimit - bos.size();
            if (count > maxCount) {
                bos.write(data, 0, maxCount);
                break;
            } else {
                bos.write(data, 0, count);
            }
        }
        bos.flush();
        bos.close();
        inputStream.close();
        return bos;
    }

    /*public static String getFileExtension(String fileUrl) {
        MediaStore.Files.
        return null;

    }*/

    public static String getMimeType(Context context, Uri uri) {
        String extension;

        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        }

        return extension;
    }

    public static String getFileExt(String fileUrl) {
        String ext = "";
        try {
            URL url = new URL(fileUrl);
            ext = url.getPath().substring(url.getPath().lastIndexOf("."));



        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
return ext;
    }
}
