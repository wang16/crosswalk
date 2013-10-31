// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.xwview.test;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.chromium.base.test.util.Feature;
import org.xwalk.core.XWalkClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebChromeClient;
import org.xwalk.core.client.XWalkDefaultNotificationService;

/**
 * Test suite for web notification API.
 * This test will only cover notification.show() and notification.close().
 * The event handler will be covered in runtime level test. Because that
 * will need activity to participate. 
 */
public class WebNotificationTest extends XWalkViewTestBase {
    private XWalkDefaultNotificationService mNotificationService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        class TestXWalkClient extends XWalkClient {
            @Override
            public void onPageStarted(XWalkView view, String url, Bitmap favicon) {
                mTestContentsClient.onPageStarted(url);
            }

            @Override
            public void onPageFinished(XWalkView view, String url) {
                mTestContentsClient.didFinishLoad(url);
            }
        }
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getXWalkView().setXWalkClient(new TestXWalkClient());
                mNotificationService = new XWalkDefaultNotificationService(
                		getXWalkView().getActivity(), getXWalkView());
                getXWalkView().setNotificationService(mNotificationService);
                getXWalkView().enableRemoteDebugging();
            }
        });
    }

    @SmallTest
    @Feature({"WebNotification"})
    public void testWebNotificationShowAndClose() throws Throwable {
        loadAssetFile("notification.html");
        getInstrumentation().waitForIdleSync();
        executeJavaScriptAndWaitForResult("notify();");
        getInstrumentation().waitForIdleSync();
        assertEquals("notification shown", getTitleOnUiThread());
        executeJavaScriptAndWaitForResult("dismiss();");
        getInstrumentation().waitForIdleSync();
        assertEquals("notification closed", getTitleOnUiThread());
    }
}
