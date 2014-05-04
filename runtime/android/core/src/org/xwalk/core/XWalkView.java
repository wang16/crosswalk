// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

import org.xwalk.core.internal.XWalkNavigationHistoryInternal;
import org.xwalk.core.internal.XWalkViewInternal;

public class XWalkView extends XWalkViewInternal {

    public XWalkView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public XWalkView(Context context, Activity activity) {
        super(context, activity);
    }

    public XWalkNavigationHistory getNavigationHistory() {
        XWalkNavigationHistoryInternal history = super.getNavigationHistory();
        if (history == null || history instanceof XWalkNavigationHistory) {
            return (XWalkNavigationHistory) history;
        } else {
            return new XWalkNavigationHistory(history);
        }
    }
}
