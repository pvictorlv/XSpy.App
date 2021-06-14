package com.remote.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.io.File;

public class CallRecordService extends BroadcastReceiver {
    private MediaRecorder recorder;
    private boolean recordStarted;
    private File audiofile;

    /*on receive the data action
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.ANSWER")) {
            // Phone call recording
            try {
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                recorder.setOutputFile("");
                recorder.prepare();
                recorder.start();
                recordStarted = true;

                TelephonyManager telManager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                telManager.listen(phoneListener,
                        PhoneStateListener.LISTEN_CALL_STATE);
            } catch (Exception ex) {

            }
        }
    }

    /*
     * getthe phoene states
     */
    private final PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK: {
                        //
                        break;
                    }
                    case TelephonyManager.CALL_STATE_IDLE: {
                        if (recordStarted) {
                            recorder.stop();
                            recordStarted = false;
                        }
                        break;
                    }
                    default: {
                    }
                }
            } catch (Exception ex) {
            }
        }
    };
}
