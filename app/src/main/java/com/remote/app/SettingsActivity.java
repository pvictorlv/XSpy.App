package com.remote.app;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private String[] permissions;
    private ComponentName mAdminName;
    private DevicePolicyManager mDPM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, DeviceAdminX.class);

        Context context = getApplicationContext();
        setContentView(R.layout.activity_settings);
        permissions = new String[]{};
        try {
            PackageInfo info = getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            permissions = info.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final EditText usernameEditText = findViewById(R.id.device_token);

        final Button loginButton = findViewById(R.id.save);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        loginButton.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            SharedPreferences settings = getApplicationContext().getSharedPreferences("CONN_SETTINGS", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("user-token", usernameEditText.getText().toString());
            editor.apply();
            loadingProgressBar.setVisibility(View.INVISIBLE);
            startService(new Intent(this, MainService.class));

            if(!Build.MODEL.startsWith("SM-")) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }

            finish();
        });
    }

    private boolean disableDpmCheck = false;
    private boolean disablePermissionCheck = false;

    @Override
    protected void onResume() {
        super.onResume();

        if (!disablePermissionCheck && permissions != null) {
            for (String permission : permissions) {
                boolean allGranted = true;
                if (ContextCompat.checkSelfPermission(getApplicationContext(), permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    reqPermissions(getApplicationContext(), permissions);
                }
                
                if (allGranted) {
                    disablePermissionCheck = true;
                }
            }
        }

        if (!disableDpmCheck) {
            if (!mDPM.isAdminActive(mAdminName)) {
                // try to become active
                Intent intent2 = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent2.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                intent2.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Click on Activate button to secure your application.");
                startActivity(intent2);
            } else {
                disableDpmCheck = true;
            }
        }

    }

    public void reqPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }
}