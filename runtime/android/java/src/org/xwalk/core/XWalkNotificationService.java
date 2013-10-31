package org.xwalk.core;

import android.content.Intent;

public interface XWalkNotificationService {
    public void setBridge(XWalkContentsClientBridge bridge);
    public void showNotification(
            String title, String message, String icon, int id, int processId, int routeId);
    public void cancelNotification(int notificationId, int processId, int routeId);
    public void shutdown();
    public boolean maybeHandleIntent(Intent intent);
}
