package com.swifttrack.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swifttrack.app.data.model.NotificationItem;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationRepository {

    private static final String PREF_NAME = "swifttrack_notifications_pref";
    private static final String KEY_NOTIF_LIST = "key_notification_list";
    private static NotificationRepository instance;
    private final Gson gson = new Gson();

    private NotificationRepository() {}

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    public synchronized List<NotificationItem> getNotifications(Context context) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = pref.getString(KEY_NOTIF_LIST, null);

        List<NotificationItem> list;
        if (json == null || json.trim().isEmpty()) {
            list = generateInitialNotifications();
            saveNotifications(context, list);
        } else {
            Type type = new TypeToken<ArrayList<NotificationItem>>(){}.getType();
            try {
                list = gson.fromJson(json, type);
                if (list == null) list = new ArrayList<>();
            } catch (Exception e) {
                list = generateInitialNotifications();
                saveNotifications(context, list);
            }
        }

        // Sort: Pinned items first, then by timeMillis descending
        Collections.sort(list, (a, b) -> {
            if (a.isPinned != b.isPinned) {
                return a.isPinned ? -1 : 1;
            }
            return Long.compare(b.timeMillis, a.timeMillis);
        });

        return list;
    }

    public synchronized void saveNotifications(Context context, List<NotificationItem> list) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(list);
        pref.edit().putString(KEY_NOTIF_LIST, json).apply();
    }

    public synchronized void addNotification(Context context, NotificationItem item) {
        List<NotificationItem> list = getNotifications(context);
        // Remove duplicate ID if exists
        list.removeIf(n -> n.id.equalsIgnoreCase(item.id));
        list.add(0, item);
        saveNotifications(context, list);
    }

    public synchronized void deleteNotification(Context context, String id) {
        List<NotificationItem> list = getNotifications(context);
        list.removeIf(n -> n.id.equalsIgnoreCase(id));
        saveNotifications(context, list);
    }

    public synchronized void deleteAllNotifications(Context context) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().remove(KEY_NOTIF_LIST).apply();
    }

    public synchronized void markAsRead(Context context, String id) {
        List<NotificationItem> list = getNotifications(context);
        for (NotificationItem item : list) {
            if (item.id.equalsIgnoreCase(id)) {
                item.isRead = true;
                break;
            }
        }
        saveNotifications(context, list);
    }

    public synchronized void markAllAsRead(Context context) {
        List<NotificationItem> list = getNotifications(context);
        for (NotificationItem item : list) {
            item.isRead = true;
        }
        saveNotifications(context, list);
    }

    public synchronized void toggleReadStatus(Context context, String id) {
        List<NotificationItem> list = getNotifications(context);
        for (NotificationItem item : list) {
            if (item.id.equalsIgnoreCase(id)) {
                item.isRead = !item.isRead;
                break;
            }
        }
        saveNotifications(context, list);
    }

    public synchronized void togglePin(Context context, String id) {
        List<NotificationItem> list = getNotifications(context);
        for (NotificationItem item : list) {
            if (item.id.equalsIgnoreCase(id)) {
                item.isPinned = !item.isPinned;
                break;
            }
        }
        saveNotifications(context, list);
    }

    public synchronized int getUnreadCount(Context context) {
        List<NotificationItem> list = getNotifications(context);
        int unread = 0;
        for (NotificationItem item : list) {
            if (!item.isRead) unread++;
        }
        return unread;
    }

    private List<NotificationItem> generateInitialNotifications() {
        List<NotificationItem> list = new ArrayList<>();
        SimpleDateFormat tsFmt = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        long now = System.currentTimeMillis();

        list.add(new NotificationItem(
                "notif_ticket_101",
                "🎟️ Ticket Confirmed: TKT-8842-1920",
                "London Paddington (PAD) ➔ Heathrow Terminal 5 (HWV). Active digital ticket issued & ready for gate scanning.",
                "PURPLE",
                "TICKET",
                tsFmt.format(new Date(now - 120000)),
                now - 120000,
                false,
                true,
                "TKT-8842-1920"
        ));

        list.add(new NotificationItem(
                "notif_platform_102",
                "🚆 Train Approaching Platform 6",
                "Heathrow Express non-stop service departing in 4 min from Platform 6. Valid until ticket expiration.",
                "BLUE",
                "TRAIN_ARRIVAL",
                tsFmt.format(new Date(now - 60000)),
                now - 60000,
                false,
                true,
                "PAD"
        ));

        list.add(new NotificationItem(
                "notif_api_103",
                "🟢 Service Operating Normally",
                "Heathrow Express services are running non-stop every 15 minutes between London Paddington and Heathrow Airport.",
                "GREEN",
                "DISRUPTION",
                tsFmt.format(new Date(now - 300000)),
                now - 300000,
                true,
                false,
                null
        ));

        list.add(new NotificationItem(
                "notif_api_104",
                "Terminal 4 Transfer Advisory",
                "Terminal 4 transfer to Terminals 2 & 3 is free via Elizabeth Line. Please allow extra time for boarding.",
                "AMBER",
                "DISRUPTION",
                tsFmt.format(new Date(now - 600000)),
                now - 600000,
                true,
                false,
                null
        ));

        return list;
    }
}
