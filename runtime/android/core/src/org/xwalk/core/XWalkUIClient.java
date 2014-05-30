// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core;

import android.net.Uri;
import android.webkit.ValueCallback;

import org.xwalk.core.internal.XWalkJavascriptResultInternal;
import org.xwalk.core.internal.XWalkUIClientInternal;
import org.xwalk.core.internal.XWalkViewInternal;

public class XWalkUIClient extends XWalkUIClientInternal {

    public XWalkUIClient(XWalkView view) {
        super(view);
    }

    @Override
    public boolean onJavascriptModalDialog(XWalkViewInternal view, int type,
            String url, String message, String defaultValue, XWalkJavascriptResultInternal result) {
        if (view instanceof XWalkView) {
            return onJavascriptModalDialog(
                    (XWalkView) view,
                    type, url, message, defaultValue,
                    new XWalkJavascriptResultHandler(result));
        } else {
            return super.onJavascriptModalDialog(view, type, url, message, defaultValue, result);
        }
    }

    public boolean onJavascriptModalDialog(XWalkView view, int type,
            String url, String message, String defaultValue, XWalkJavascriptResult result) {
        XWalkJavascriptResultInternal resultInternal =
                ((XWalkJavascriptResultHandler) result).getInternal();
        return super.onJavascriptModalDialog(
                view, type, url, message, defaultValue, resultInternal);
    }

    @Override
    public void onRequestFocus(XWalkViewInternal view) {
        if (view instanceof XWalkView) {
            onRequestFocus((XWalkView) view);
        } else {
            super.onRequestFocus(view);
        }
    }

    public void onRequestFocus(XWalkView view) {
        super.onRequestFocus(view);
    }

    @Override
    public void onJavascriptCloseWindow(XWalkViewInternal view) {
        if (view instanceof XWalkView) {
            onJavascriptCloseWindow((XWalkView) view);
        } else {
            super.onJavascriptCloseWindow(view);
        }
    }

    public void onJavascriptCloseWindow(XWalkView view) {
        super.onJavascriptCloseWindow(view);
    }

    @Override
    public void onFullscreenToggled(XWalkViewInternal view, boolean enterFullscreen) {
        if (view instanceof XWalkView) {
            onFullscreenToggled((XWalkView) view, enterFullscreen);
        } else {
            super.onFullscreenToggled(view, enterFullscreen);
        }
    }

    public void onFullscreenToggled(XWalkView view, boolean enterFullscreen) {
        super.onFullscreenToggled(view, enterFullscreen);
    }

    @Override
    public void openFileChooser(XWalkViewInternal view, ValueCallback<Uri> uploadFile,
            String acceptType, String capture) {
        if (view instanceof XWalkView) {
            openFileChooser((XWalkView) view, uploadFile, acceptType, capture);
        } else {
            super.openFileChooser(view, uploadFile, acceptType, capture);
        }
    }

    public void openFileChooser(XWalkView view, ValueCallback<Uri> uploadFile,
            String acceptType, String capture) {
        super.openFileChooser(view, uploadFile, acceptType, capture);
    }

    @Override
    public void onScaleChanged(XWalkViewInternal view, float oldScale, float newScale) {
        if (view instanceof XWalkView) {
            onScaleChanged((XWalkView) view, oldScale, newScale);
        } else {
            super.onScaleChanged(view, oldScale, newScale);
        }
    }

    public void onScaleChanged(XWalkView view, float oldScale, float newScale) {
        super.onScaleChanged(view, oldScale, newScale);
    }
}
