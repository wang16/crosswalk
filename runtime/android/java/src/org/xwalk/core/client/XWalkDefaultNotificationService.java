package org.xwalk.core.client;

import java.util.HashSet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.widget.Toast;

import org.xwalk.core.XWalkContentsClientBridge;
import org.xwalk.core.XWalkNotificationService;
import org.xwalk.core.XWalkView;

public class XWalkDefaultNotificationService implements XWalkNotificationService {
    private static final String TAG = "XWalkDefaultNotificationService";

    private static final String ACTION_CLICK_NOTIFICATION_SUFFIX = ".notification.click";
    private static final String ACTION_CLOSE_NOTIFICATION_SUFFIX = ".notification.close";
    private static final String INTENT_EXTRA_KEY_NOTIFICATION_ID = "id";
    private static final String INTENT_EXTRA_KEY_PROCESS_ID = "process_id";
    private static final String INTENT_EXTRA_KEY_ROUTE_ID = "route_id";
    private static final String INTENT_CATEGORY_NOTIFICATION_PREFIX = "notification_";

    private Context mContext;
    private XWalkContentsClientBridge mBridge;
    private XWalkView mView;
    private NotificationManager mNotificationManager;
    private BroadcastReceiver mNotificationCloseReceiver;
    private IntentFilter mNotificationCloseIntentFilter;
    // Boolean is for whether the notification is canceled by user.
    private HashSet<Integer> mExistNotificationIds;

    public XWalkDefaultNotificationService(Context context, XWalkView view) {
        mContext = context;
        mView = view;
        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // Cancel all exist notification at startup time. To avoid receive legacy pendingIntents.
        mNotificationManager.cancelAll();
        mExistNotificationIds = new HashSet<Integer>();
    }

    @Override
    public void setBridge(XWalkContentsClientBridge bridge) {
        mBridge = bridge;
    }

    private static String getCategoryFromNotificationId(int id) {
        return INTENT_CATEGORY_NOTIFICATION_PREFIX + id;
    }

    private void notificationChanged() {
        unregisterReceiver();
        if (mExistNotificationIds.isEmpty()) {
            Log.i(TAG, "notifications are all cleared," +
                    "unregister broadcast receiver for close pending intent");
        } else {
            registerReceiver();
        }
    }

    private void registerReceiver() {
        mNotificationCloseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mView.onNewIntent(intent);
            }
        };
        mNotificationCloseIntentFilter = new IntentFilter(
                mView.getActivity().getPackageName() + ACTION_CLOSE_NOTIFICATION_SUFFIX);
        for(Integer id : mExistNotificationIds) {
            mNotificationCloseIntentFilter.addCategory(getCategoryFromNotificationId(id));
        }
        try {
            mView.getActivity().registerReceiver(mNotificationCloseReceiver, mNotificationCloseIntentFilter);
        } catch (AndroidRuntimeException e) {
            //FIXME(wang16): The exception will happen when there are multiple xwalkviews in one activity.
            //               Remove it after notification service supports multi-views.
            mNotificationCloseReceiver = null;
            Log.w(TAG, e.getLocalizedMessage());
        }
    }

    private void unregisterReceiver() {
        if (mNotificationCloseReceiver != null) {
            mView.getActivity().unregisterReceiver(mNotificationCloseReceiver);
            mNotificationCloseReceiver = null;
        }
    }

    @Override
    public void shutdown() {
        unregisterReceiver();
        mBridge = null;
    }

    @Override
    public boolean maybeHandleIntent(Intent intent) {
        if (intent.getAction() == null) return false;
        int notificationId = intent.getIntExtra(INTENT_EXTRA_KEY_NOTIFICATION_ID, -1);
        int processId = intent.getIntExtra(INTENT_EXTRA_KEY_PROCESS_ID, -1);
        int routeId = intent.getIntExtra(INTENT_EXTRA_KEY_ROUTE_ID, -1);
        if (notificationId < 0) return false;
        if (intent.getAction().equals(
                mView.getActivity().getPackageName() + ACTION_CLOSE_NOTIFICATION_SUFFIX)) {
            onNotificationClose(notificationId, true, processId, routeId);
            return true;
        } else if (intent.getAction().equals(
                mView.getActivity().getPackageName() + ACTION_CLICK_NOTIFICATION_SUFFIX)) {
            onNotificationClick(notificationId, processId, routeId);
            return true;
        }
        return false;
    }

    @Override
    public void showNotification(
            String title, String message, String icon, int id, int processId, int routeId) {
        Context activity = mView.getActivity();
        String category = getCategoryFromNotificationId(id);
        Intent clickIntent = new Intent(activity, activity.getClass());
        clickIntent.setAction(activity.getPackageName() + ACTION_CLICK_NOTIFICATION_SUFFIX);
        clickIntent.putExtra(INTENT_EXTRA_KEY_NOTIFICATION_ID, id);
        clickIntent.putExtra(INTENT_EXTRA_KEY_PROCESS_ID, processId);
        clickIntent.putExtra(INTENT_EXTRA_KEY_ROUTE_ID, routeId);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        clickIntent.addCategory(category);
        PendingIntent pendingClickIntent = PendingIntent.getActivity(activity,
                0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent closeIntent = new Intent(activity.getPackageName() + ACTION_CLOSE_NOTIFICATION_SUFFIX);
        closeIntent.putExtra(INTENT_EXTRA_KEY_NOTIFICATION_ID, id);
        closeIntent.putExtra(INTENT_EXTRA_KEY_PROCESS_ID, processId);
        closeIntent.putExtra(INTENT_EXTRA_KEY_ROUTE_ID, routeId);
        closeIntent.addCategory(category);
        PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(activity,
                0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        int iconRes = mContext.getApplicationInfo().icon;
        if (iconRes == 0) iconRes = android.R.drawable.sym_def_app_icon;
        Notification notification = new Notification.Builder(mContext.getApplicationContext())
                .setContentText(message)
                .setContentTitle(title)
                .setSmallIcon(iconRes)
                .setAutoCancel(true)
                .setContentIntent(pendingClickIntent)
                .setDeleteIntent(pendingCloseIntent).build();
        doShowNotification(id, notification);
        mExistNotificationIds.add(id);
        notificationChanged();
        onNotificationShown(id, processId, routeId);
    }
    
    @Override
    public void cancelNotification(int notificationId, int processId, int routeId) {
        mNotificationManager.cancel(notificationId);
        onNotificationClose(notificationId, false, processId, routeId);
    }

    public void doShowNotification(int id, Notification notification) {
        mNotificationManager.notify(id, notification);
    }

    public void onNotificationShown(int notificationId, int processId, int routeId) {
        if (mExistNotificationIds.contains(notificationId)) {
            if (mBridge != null) {
                mBridge.notificationDisplayed(notificationId, processId, routeId);
            }
            // TODO(wang16): invoke js callback.
            Toast.makeText(mContext,
                    "notification " + notificationId + " shown", Toast.LENGTH_LONG).show();
        }
    }

    public void onNotificationClick(int notificationId, int processId, int routeId) {
        if (mExistNotificationIds.remove(notificationId)) {
            notificationChanged();
            if (mBridge != null) {
                mBridge.notificationClicked(notificationId, processId, routeId);
            } 
            // TODO(wang16): invoke js callback.
            Toast.makeText(mContext,
                    "notification " + notificationId + " clicked", Toast.LENGTH_LONG).show();
        }
    }

    public void onNotificationClose(
            int notificationId, boolean byUser, int processId, int routeId) {
        if (mExistNotificationIds.remove(notificationId)) {
            notificationChanged();
            if (mBridge != null) {
                mBridge.notificationClosed(notificationId, byUser, processId, routeId);
            }
            // TODO(wang16): invoke js callback.
            if (byUser) {
                Toast.makeText(mContext,
                        "notification " + notificationId + " closed by user", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext,
                        "notification " + notificationId + " closed by js", Toast.LENGTH_LONG).show();
            }
        }
    }
}
