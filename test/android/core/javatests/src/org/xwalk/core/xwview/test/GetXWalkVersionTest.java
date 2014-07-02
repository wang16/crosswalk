// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.xwview.test;

import android.test.suitebuilder.annotation.SmallTest;

import org.chromium.base.test.util.Feature;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test suite for getXWalkVersion().
 */
public class GetXWalkVersionTest extends XWalkViewTestBase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @SmallTest
    @Feature({"GetXWalkVersion"})
    public void testGetXWalkVersion() throws Throwable {
        String version = getXWalkVersionOnUiThread();
        Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
        Matcher matcher = pattern.matcher(version);
        assertTrue("The Crosswalk version is invalid.", matcher.find());
    }
}
