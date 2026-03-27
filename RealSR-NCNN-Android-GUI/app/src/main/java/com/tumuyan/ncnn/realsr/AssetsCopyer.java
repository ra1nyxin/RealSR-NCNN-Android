package com.tumuyan.ncnn.realsr;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetsCopyer {

    private static final String TAG = "AssetsCopyer";
    private static final int BUFFER_SIZE = 8192;

    /**
     * 释放 Assets 目录到指定路径
     *
     * @param context       上下文
     * @param assetsDir     Assets 中的源目录（空字符串表示根目录）
     * @param releaseDir    目标存储路径
     * @param skipExistFile 是否跳过已存在文件
     */
    public static void releaseAssets(Context context, String assetsDir,
                                     String releaseDir, boolean skipExistFile) {

        if (TextUtils.isEmpty(releaseDir)) return;
        releaseDir = releaseDir.endsWith("/") ? releaseDir.substring(0, releaseDir.length() - 1) : releaseDir;
        final String currentAssetsPath = (TextUtils.isEmpty(assetsDir) || assetsDir.equals("/")) ? "" : assetsDir;

        AssetManager assetManager = context.getAssets();
        try {
            String[] fileNames = assetManager.list(currentAssetsPath);
            if (fileNames != null && fileNames.length > 0) {
                for (String name : fileNames) {
                    String subAssetsPath = TextUtils.isEmpty(currentAssetsPath) ? name : currentAssetsPath + File.separator + name;
                    String[] childFiles = assetManager.list(subAssetsPath);

                    if (childFiles != null && childFiles.length > 0) {
                        // 级联创建目录并递归
                        ensureDirectory(releaseDir + File.separator + subAssetsPath);
                        releaseAssets(context, subAssetsPath, releaseDir, skipExistFile);
                    } else {
                        // 它是文件：执行写入
                        copySingleFile(assetManager, subAssetsPath, releaseDir + File.separator + subAssetsPath, skipExistFile);
                    }
                }
            } else {
                // 处理单个文件直接拷贝的情况
                if (!currentAssetsPath.isEmpty()) {
                    copySingleFile(assetManager, currentAssetsPath, releaseDir + File.separator + currentAssetsPath, skipExistFile);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get asset list or copy file: " + currentAssetsPath, e);
        }
    }

    private static void copySingleFile(AssetManager assetManager, String assetFilePath, String destFilePath, boolean skipExistFile) {
        File destFile = new File(destFilePath);

        if (destFile.exists()) {
            if (skipExistFile) {
                Log.d(TAG, "File exists, skipping: " + destFilePath);
                return;
            }
            if (!destFile.delete()) {
                Log.w(TAG, "Failed to delete existing file: " + destFilePath);
            }
        }

        File parent = destFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // 使用 try-with-resources 自动关闭流
        try (InputStream in = assetManager.open(assetFilePath);
             OutputStream out = new FileOutputStream(destFile)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            Log.d(TAG, "Successfully copied: " + assetFilePath);
        } catch (IOException e) {
            Log.e(TAG, "Error writing file: " + destFilePath, e);
        }
    }

    private static void ensureDirectory(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (!file.isDirectory()) {
                file.delete();
                file.mkdirs();
            }
        } else {
            file.mkdirs();
        }
    }
}
