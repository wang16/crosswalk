// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core;

import org.xwalk.core.internal.XWalkNavigationHistoryInternal;
import org.xwalk.core.internal.XWalkNavigationItemInternal;

public class XWalkNavigationHistory extends XWalkNavigationHistoryInternal {

    public XWalkNavigationHistory(XWalkNavigationHistoryInternal internal) {
        super(internal);
    }

    public XWalkNavigationItem getItemAt(int index) {
        XWalkNavigationItemInternal item = super.getItemAt(index);
        if (item == null || item instanceof XWalkNavigationItem) {
            return (XWalkNavigationItem) item;
        } else {
            return new XWalkNavigationItem(item);
        }
    }

    public XWalkNavigationItem getCurrentItem() {
        XWalkNavigationItemInternal item = super.getCurrentItem();
        if (item == null || item instanceof XWalkNavigationItem) {
            return (XWalkNavigationItem) item;
        } else {
            return new XWalkNavigationItem(item);
        }
    }
}
