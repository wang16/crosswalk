// Copyright (c) 2013-2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.internal;

import java.io.Serializable;

import org.chromium.content.browser.NavigationHistory;

/**
 * This class represents a navigation history for a XWalkViewInternal instance.
 * It's not thread-safe and should be only called on UI thread.
 */
@XWalkAPI(createInternally = true)
public class XWalkNavigationHistoryInternal implements Cloneable, Serializable {
    private NavigationHistory mHistory;
    private XWalkViewInternal mXWalkView;

    // Never use this constructor.
    // It is only used in XWalkNavigationHistoryBridge.
    XWalkNavigationHistoryInternal() {
        mXWalkView = null;
        mHistory = null;
    }

    XWalkNavigationHistoryInternal(XWalkViewInternal view, NavigationHistory history) {
        mXWalkView = view;
        mHistory = history;
    }

    XWalkNavigationHistoryInternal(XWalkNavigationHistoryInternal history) {
        mXWalkView = history.mXWalkView;
        mHistory = history.mHistory;
    }

    /**
     * Total size of navigation history for the XWalkViewInternal.
     * @return the size of total navigation items.
     */
    @XWalkAPI
    public int size() {
        return mHistory.getEntryCount();
    }

    /**
     * Test whether there is an item at a specific index.
     * @param index the given index.
     * @return true if there is an item at the specific index.
     */
    @XWalkAPI
    public boolean hasItemAt(int index) {
        return index >=0 && index <= size() - 1;
    }

    /**
     * Get a specific item given by index.
     * @param index the given index.
     * @return the navigation item for the given index.
     */
    @XWalkAPI
    public XWalkNavigationItemInternal getItemAt(int index) {
        return new XWalkNavigationItemInternal(mHistory.getEntryAtIndex(index));
    }

    /**
     * Get the current item which XWalkViewInternal displays.
     * @return the current navigation item.
     */
    @XWalkAPI
    public XWalkNavigationItemInternal getCurrentItem() {
        return getItemAt(getCurrentIndex());
    }

    /**
     * Test whether XWalkViewInternal can go back.
     * @return true if it can go back.
     */
    @XWalkAPI
    public boolean canGoBack() {
        return mXWalkView.canGoBack();
    }

    /**
     * Test whether XWalkViewInternal can go forward.
     * @return true if it can go forward.
     */
    @XWalkAPI
    public boolean canGoForward() {
        return mXWalkView.canGoForward();
    }

    /**
     * The direction for web page navigation.
     */
    /** The backward direction for web page navigation. */
    @XWalkAPI(isConst = true)
    public final static int BACKWARD = 0;
    /** The forward direction for web page navigation. */
    @XWalkAPI(isConst = true)
    public final static int FORWARD = 1;

    /**
     * Navigates to the specified step from the current navigation item.
     * Do nothing if the offset is out of bound.
     * @param direction the direction of navigation.
     * @param steps go back or foward with a given steps.
     */
    @XWalkAPI
    public void navigate(int direction, int steps) {
        switch(direction) {
            case FORWARD:
                mXWalkView.navigateTo(steps);
                break;
            case BACKWARD:
                mXWalkView.navigateTo(-steps);
                break;
            default:
                break;
        }
    }

    /**
     * Get the index for current navigation item.
     * @return current index in the navigation history.
     */
    @XWalkAPI
    public int getCurrentIndex() {
        return mHistory.getCurrentEntryIndex();
    }

    /**
     * Clear all history owned by this XWalkViewInternal.
     */
    @XWalkAPI
    public void clear() {
        mXWalkView.clearHistory();
    }

    protected synchronized XWalkNavigationHistoryInternal clone() {
        return new XWalkNavigationHistoryInternal(this);
    }
}
