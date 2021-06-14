package com.remote.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class PermissionManager {

    public static JSONObject getGrantedPermissions(Context context) {
        JSONObject data = new JSONObject();
        try {
            JSONArray perms = new JSONArray();
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    String perm = pi.requestedPermissions[i];
                    perm = perm.replace("android.permission.", "");
                    perms.put(perm);
                }
            }
            data.put("permissions", perms);
        } catch (Exception e) {
        }
        return data;
    }

    public static boolean canIUse(String perm, Context context) {
        return context.getPackageManager().checkPermission(perm, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

}
