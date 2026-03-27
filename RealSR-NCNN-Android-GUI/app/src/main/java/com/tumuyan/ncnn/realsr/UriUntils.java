package com.tumuyan.ncnn.realsr;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import java.lang.reflect.Method;

public class UriUntils {
    private static final String TAG = "UriUntils";
    public static String getFileName(Uri uri, Context context) {
        if (uri == null) return null;
        String path = getPathFromUri(uri, context);
        if (path != null) {
            return new File(path).getName();
        }
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) return cursor.getString(index);
            }
        } catch (Exception e) {
            Log.e(TAG, "getFileName failed", e);
        }
        return null;
    }

    public static String getPathFromUri(Uri uri, Context context) {
        if (uri == null) return null;
        String path = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            path = handleDocumentUri(context, uri);
        } 
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            path = getDataColumn(context, uri, null, null);
            if (path == null) {
                path = getPathByReflection(context, uri);
            }
        } 
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        return path;
    }

    private static String handleDocumentUri(Context context, Uri uri) {
        final String auth = uri.getAuthority();
        final String docId = DocumentsContract.getDocumentId(uri);
        if ("com.android.externalstorage.documents".equals(auth)) {
            String[] split = docId.split(":");
            if ("primary".equalsIgnoreCase(split[0])) {
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
        } else if ("com.android.providers.downloads.documents".equals(auth)) {
            if (docId.startsWith("raw:")) return docId.substring(4);            
            try {
                String cleanId = docId.replaceAll("^(msf|raw):", "");
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(cleanId));
                return getDataColumn(context, contentUri, null, null);
            } catch (Exception e) {
                return null;
            }
        } else if ("com.android.providers.media.documents".equals(auth)) {
            String[] split = docId.split(":");
            Uri contentUri = null;
            switch (split[0]) {
                case "image": contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI; break;
                case "video": contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI; break;
                case "audio": contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; break;
            }
            return getDataColumn(context, contentUri, "_id=?", new String[]{split[1]});
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        final String column = MediaStore.MediaColumns.DATA;
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{column}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            Log.w(TAG, "getDataColumn failed for " + uri);
        }
        return null;
    }

    private static String getPathByReflection(Context context, Uri uri) {
        try {
            ProviderInfo info = context.getPackageManager().resolveContentProvider(uri.getAuthority(), 0);
            if (info == null) return null;
            Method getPathStrategy = FileProvider.class.getDeclaredMethod("getPathStrategy", Context.class, String.class);
            getPathStrategy.setAccessible(true);
            Object strategy = getPathStrategy.invoke(null, context, uri.getAuthority());
            if (strategy != null) {
                Method getFileForUri = strategy.getClass().getDeclaredMethod("getFileForUri", Uri.class);
                getFileForUri.setAccessible(true);
                File file = (File) getFileForUri.invoke(strategy, uri);
                if (file != null) {
                    String path = file.getAbsolutePath();
                    if (!path.contains(context.getPackageName())) return path;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Reflection failed", e);
        }
        return null;
    }
}
