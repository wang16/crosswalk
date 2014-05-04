// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core;

import android.webkit.WebResourceResponse;

import org.xwalk.core.internal.XWalkResourceClientInternal;
import org.xwalk.core.internal.XWalkViewInternal;

public class XWalkResourceClient extends XWalkResourceClientInternal {

    public XWalkResourceClient(XWalkView view) {
        super(view);
    }

    @Override
    public void onLoadStarted(XWalkViewInternal view, String url) {
        if (view instanceof XWalkView) {
            onLoadStarted((XWalkView) view, url);
        } else {
            super.onLoadStarted(view, url);
        }
    }

    public void onLoadStarted(XWalkView view, String url) {
        super.onLoadStarted(view, url);
    }

    @Override
    public void onLoadFinished(XWalkViewInternal view, String url) {
        if (view instanceof XWalkView) {
            onLoadFinished((XWalkView) view, url);
        } else {
            super.onLoadFinished(view, url);
        }
    }

    public void onLoadFinished(XWalkView view, String url) {
        super.onLoadFinished(view, url);
    }

    @Override
    public void onProgressChanged(XWalkViewInternal view, int progressInPercent) {
        if (view instanceof XWalkView) {
            onProgressChanged((XWalkView) view, progressInPercent);
        } else {
            super.onProgressChanged(view, progressInPercent);
        }
    }

    public void onProgressChanged(XWalkView view, int progressInPercent) {
        super.onProgressChanged(view, progressInPercent);
    }

    @Override
    public WebResourceResponse shouldInterceptLoadRequest(XWalkViewInternal view, String url) {
        if (view instanceof XWalkView) {
            return shouldInterceptLoadRequest((XWalkView) view, url);
        } else {
            return super.shouldInterceptLoadRequest(view, url);
        }
    }

    public WebResourceResponse shouldInterceptLoadRequest(XWalkView view, String url) {
        return super.shouldInterceptLoadRequest(view, url);
    }

    @Override
    public void onReceivedLoadError(XWalkViewInternal view, int errorCode, String description,
            String failingUrl) {
        if (view instanceof XWalkView) {
            onReceivedLoadError((XWalkView) view, errorCode, description, failingUrl);
        } else {
            super.onReceivedLoadError(view, errorCode, description, failingUrl);
        }
    }

    public void onReceivedLoadError(XWalkView view, int errorCode, String description,
            String failingUrl) {
        super.onReceivedLoadError(view, errorCode, description, failingUrl);
    }
}
