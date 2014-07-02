// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.xwview.test;

import android.graphics.Bitmap;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Pair;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Callable;

import org.chromium.base.test.util.DisabledTest;
import org.chromium.base.test.util.Feature;
import org.chromium.content.browser.test.util.CallbackHelper;
import org.chromium.content.browser.test.util.Criteria;
import org.chromium.content.browser.test.util.CriteriaHelper;
import org.chromium.net.test.util.TestWebServer;
import org.xwalk.core.XWalkView;
import org.xwalk.core.internal.XWalkClient;
import org.xwalk.core.internal.XWalkSettings;
import org.xwalk.core.internal.XWalkWebChromeClient;
import org.xwalk.core.xwview.test.util.CommonResources;

/**
 * TODO(wang16): This test should be moved to internal test.
 *
 * Test suite for setAppCacheEnabled().
 */
public class SetAppCacheEnabledTest extends XWalkViewTestBase {
    private static final long TEST_TIMEOUT = 20000L;
    private static final int CHECK_INTERVAL = 100;
    private TestHelperBridge mContentClient;
    private XWalkSettings mSettings;

    static class ManifestTestHelper {
        private final TestWebServer mWebServer;
        private final String mHtmlPath;
        private final String mHtmlUrl;
        private final String mManifestPath;

        ManifestTestHelper(TestWebServer webServer, String htmlPageName, String manifestName) {
            mWebServer = webServer;
            mHtmlPath = "/" + htmlPageName;
            mHtmlUrl = webServer.setResponse(
                    mHtmlPath, "<html manifest=\"" + manifestName + "\"></html>", null);
            mManifestPath = "/" + manifestName;
            webServer.setResponse(
                    mManifestPath,
                    "CACHE MANIFEST",
                    CommonResources.getContentTypeAndCacheHeaders("text/cache-manifest", false));
        }

        String getHtmlPath() {
            return mHtmlPath;
        }

        String getHtmlUrl() {
            return mHtmlUrl;
        }

        String getManifestPath() {
            return mManifestPath;
        }

        int waitUntilHtmlIsRequested(final int initialRequestCount) throws InterruptedException {
            return waitUntilResourceIsRequested(mHtmlPath, initialRequestCount);
        }

        int waitUntilManifestIsRequested(final int initialRequestCount)
                throws InterruptedException {
            return waitUntilResourceIsRequested(mManifestPath, initialRequestCount);
        }

        private int waitUntilResourceIsRequested(
                final String path, final int initialRequestCount) throws InterruptedException {
            boolean result = CriteriaHelper.pollForCriteria(new Criteria() {
                @Override
                public boolean isSatisfied() {
                    return mWebServer.getRequestCount(path) > initialRequestCount;
                }
            }, TEST_TIMEOUT, CHECK_INTERVAL);
            assertTrue(result);
            return mWebServer.getRequestCount(path);
        }
    }

    private XWalkSettings getXWalkSettings(final XWalkView view) {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mSettings = view.getSettings();
            }
        });
        return mSettings;
    }

    @SmallTest
    @Feature({"XWalkView", "Preferences", "AppCache"})
    public void testAppCache() throws Throwable {
        final TestHelperBridge helperBridge =
                new TestHelperBridge();
        mContentClient = helperBridge;
        final XWalkViewTestBase.TestXWalkUIClientBase uiClient =
                new XWalkViewTestBase.TestXWalkUIClientBase(helperBridge);
        final XWalkViewTestBase.TestXWalkResourceClientBase resourceClient =
                new XWalkViewTestBase.TestXWalkResourceClientBase(helperBridge);
        final XWalkView xWalkView =
                createXWalkViewContainerOnMainSync(getActivity(), uiClient,
                        resourceClient);

        final XWalkSettings settings = getXWalkSettings(xWalkView);
        settings.setJavaScriptEnabled(true);
        settings.setAppCacheEnabled(false);

        TestWebServer webServer = null;
        try {
            webServer = new TestWebServer(false);
            ManifestTestHelper helper = new ManifestTestHelper(
                    webServer, "testAppCache.html", "appcache.manifest");
            loadUrlSyncByContent(
                    xWalkView,
                    mContentClient,
                    helper.getHtmlUrl());
            helper.waitUntilHtmlIsRequested(0);
            // Unfortunately, there is no other good way of verifying that AppCache is
            // disabled, other than checking that it didn't try to fetch the manifest.
            Thread.sleep(1000);
            assertEquals(0, webServer.getRequestCount(helper.getManifestPath()));
            // Enables AppCache. Use the default path if app cache path isn't set.
            settings.setAppCacheEnabled(true);
            loadUrlSyncByContent(
                    xWalkView,
                    mContentClient,
                    helper.getHtmlUrl());
            helper.waitUntilManifestIsRequested(0);
        } finally {
            if (webServer != null) webServer.shutdown();
        }
    }

    /*
     * @SmallTest
     * @Feature({"XWalkView", "Preferences", "AppCache"})
     * This test is flaky but the root cause is not found yet. See crbug.com/171765.
     */
    @DisabledTest
    public void testAppCacheWithTwoViews() throws Throwable {
        // We don't use the test helper here, because making sure that AppCache
        // is disabled takes a lot of time, so running through the usual drill
        // will take about 20 seconds.
        ViewPair views = createViews();

        XWalkSettings settings0 = getXWalkSettingsOnUiThreadByContent(
                views.getContent0());
        settings0.setJavaScriptEnabled(true);
        settings0.setAppCachePath("whatever");
        settings0.setAppCacheEnabled(true);
        XWalkSettings settings1 = getXWalkSettingsOnUiThreadByContent(
                views.getContent1());
        settings1.setJavaScriptEnabled(true);
        // AppCachePath setting is global, no need to set it for the second view.
        settings1.setAppCacheEnabled(true);

        TestWebServer webServer = null;
        try {
            webServer = new TestWebServer(false);
            ManifestTestHelper helper0 = new ManifestTestHelper(
                    webServer, "testAppCache_0.html", "appcache.manifest_0");
            mContentClient = views.getClient0();
            loadUrlSyncByContent(
                    views.getContent0(),
                    mContentClient,
                    helper0.getHtmlUrl());
            int manifestRequests0 = helper0.waitUntilManifestIsRequested(0);
            ManifestTestHelper helper1 = new ManifestTestHelper(
                    webServer, "testAppCache_1.html", "appcache.manifest_1");
            mContentClient = views.getClient1();
            loadUrlSyncByContent(
                    views.getContent1(),
                    mContentClient,
                    helper1.getHtmlUrl());
            helper1.waitUntilManifestIsRequested(0);
            settings1.setAppCacheEnabled(false);
            mContentClient = views.getClient0();
            loadUrlSyncByContent(
                    views.getContent0(),
                    mContentClient,
                    helper0.getHtmlUrl());
            helper0.waitUntilManifestIsRequested(manifestRequests0);
            final int prevManifestRequestCount =
                    webServer.getRequestCount(helper1.getManifestPath());
            int htmlRequests1 = webServer.getRequestCount(helper1.getHtmlPath());
            mContentClient = views.getClient1();
            loadUrlSyncByContent(
                    views.getContent1(),
                    mContentClient,
                    helper1.getHtmlUrl());
            helper1.waitUntilHtmlIsRequested(htmlRequests1);
            // Unfortunately, there is no other good way of verifying that AppCache is
            // disabled, other than checking that it didn't try to fetch the manifest.
            Thread.sleep(1000);
            assertEquals(
                    prevManifestRequestCount, webServer.getRequestCount(helper1.getManifestPath()));
        } finally {
            if (webServer != null) webServer.shutdown();
        }
    }
}
