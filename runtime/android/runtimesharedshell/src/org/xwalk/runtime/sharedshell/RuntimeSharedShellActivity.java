// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.sharedshell;

import org.xwalk.app.activity.RuntimeActivityBase;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

public class RuntimeSharedShellActivity extends RuntimeActivityBase {
    @Override
    protected void DidTryLoadRuntimeView(View webView) {
        if (webView != null) {
            setContentView(webView);
            GetRuntimeView().loadAppFromUrl("http://www.baidu.com");
        } else {
            TextView msgText = new TextView(this);
            msgText.setText(R.string.download_dialog_msg);
            msgText.setTextSize(36);
            msgText.setTextColor(Color.BLACK);
            setContentView(msgText);
        }
    }
}
