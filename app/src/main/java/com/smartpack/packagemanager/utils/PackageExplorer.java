/*
 * Copyright (C) 2020-2021 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of Package Manager, a simple, yet powerful application
 * to manage other application installed on an android device.
 *
 */

package com.smartpack.packagemanager.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartpack.packagemanager.R;
import com.smartpack.packagemanager.activities.PackageExploreActivity;

import net.dongliu.apk.parser.ApkFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on February 09, 2020
 */
public class PackageExplorer {

    public static List<String> mAPKList = new ArrayList<>();
    public static MaterialCardView mSelect;

    public static boolean isTextFile(String path) {
        return path.endsWith(".txt") || path.endsWith(".xml") || path.endsWith(".json") || path.endsWith(".properties")
                || path.endsWith(".version") || path.endsWith(".sh") || path.endsWith(".MF") || path.endsWith(".SF")
                || path.endsWith(".RSA") || path.endsWith(".html");
    }

    public static boolean isImageFile(String path) {
        return path.endsWith(".bmp") || path.endsWith(".png") || path.endsWith(".jpg");
    }

    public static int getSpanCount(Activity activity) {
        return Utils.getOrientation(activity) == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1;
    }

    public static String readManifest(String apk) {
        try (ApkFile apkFile = new ApkFile(new File(apk))) {
            String manifest = apkFile.getManifestXml();
            apkFile.close();
            return manifest;
        } catch (IOException ignored) {
        }
        return null;
    }

    public static String readXMLFromAPK(String apk, String path) {
        try (ApkFile apkFile = new ApkFile(new File(apk))) {
            String xnkData = apkFile.transBinaryXml(path);
            apkFile.close();
            return xnkData;
        } catch (IOException ignored) {
        }
        return null;
    }

    public static Uri getIconFromPath(String path) {
        File mFile = new File(path);
        if (mFile.exists()) {
            return Uri.fromFile(mFile);
        }
        return null;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void saveIcon(Bitmap bitmap, String dest, Activity activity) {
        if (Utils.isStorageWritePermissionDenied(activity)) {
            ActivityCompat.requestPermissions(activity, new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            Utils.snackbar(activity.findViewById(android.R.id.content), activity.getString(R.string.permission_denied_write_storage));
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                PackageData.makePackageFolder();
            }
            @Override
            protected Void doInBackground(Void... voids) {
                File file = new File(dest);
                try {
                    OutputStream outStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                } catch (IOException ignored) {}
                return null;
            }

            @SuppressLint("StringFormatInvalid")
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                new MaterialAlertDialogBuilder(activity)
                        .setMessage(PackageData.mApplicationName + " icon " +
                                activity.getString(R.string.export_file_message, Objects.requireNonNull(
                                        new File(dest).getParentFile()).toString()))
                        .setPositiveButton(R.string.cancel, (dialogInterface, i) -> {
                        }).show();
            }
        }.execute();
    }

    public static void copyToStorage(String path, String dest, Activity activity) {
        if (Utils.isStorageWritePermissionDenied(activity)) {
            ActivityCompat.requestPermissions(activity, new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            Utils.snackbar(activity.findViewById(android.R.id.content), activity.getString(R.string.permission_denied_write_storage));
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (!Utils.exist(dest)) {
                    Utils.mkdir(dest);
                }
            }
            @Override
            protected Void doInBackground(Void... voids) {
                Utils.copy(path, dest + "/" + new File(path).getName());
                return null;
            }

            @SuppressLint("StringFormatInvalid")
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                new MaterialAlertDialogBuilder(activity)
                        .setMessage(new File(path).getName() + " " +
                                activity.getString(R.string.export_file_message, dest))
                        .setPositiveButton(R.string.cancel, (dialogInterface, i) -> {
                        }).show();
            }
        }.execute();
    }

    public static void exploreAPK(LinearLayout linearLayout, String path, Activity activity) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                linearLayout.setVisibility(View.VISIBLE);
                if (Utils.exist(activity.getCacheDir().getPath() + "/apk")) {
                    Utils.delete(activity.getCacheDir().getPath() + "/apk");
                }
                Utils.mkdir(activity.getCacheDir().getPath() + "/apk");
                PackageData.mPath = activity.getCacheDir().getPath() + "/apk";
            }

            @Override
            protected Void doInBackground(Void... voids) {
                Utils.unzip(path,activity.getCacheDir().getPath() + "/apk");
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                linearLayout.setVisibility(View.GONE);
                Intent explorer = new Intent(activity, PackageExploreActivity.class);
                activity.startActivity(explorer);
            }
        }.execute();
    }

}